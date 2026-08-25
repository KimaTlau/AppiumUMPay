package org.example;

import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the emailed sign-up code out of the mailbox over IMAP, so registration finishes
 * without a person watching an inbox.
 *
 * Freshness is the real problem, not reading. The mailbox holds a code from every previous
 * run, and submitting yesterday's is worse than submitting nothing — it looks like a
 * rejection rather than a stale value. The guard is the address: each run registers its own
 * {@code +timestamp} alias, Gmail delivers every alias to the one inbox, and only one
 * message can ever be addressed to that alias. Matching on it makes a stale code impossible
 * rather than merely unlikely.
 *
 * An empty return always means "no code available", never "fail the run" — the caller
 * decides what that is worth.
 */
public class VerificationCodeReader {

    private static final String HOST = System.getProperty("umpay.imap.host", "imap.gmail.com");
    private static final String PORT = System.getProperty("umpay.imap.port", "993");

    /** The mailbox every +alias is delivered to. */
    private static final String MAILBOX =
            System.getProperty("umpay.mail.address", "lawma195.infinity@gmail.com");

    private static final long POLL_INTERVAL_MILLIS = 5000;

    /** Only the tail of the inbox is worth scanning; the message is always recent. */
    private static final int MESSAGES_TO_SCAN = 25;

    /** "your code is 123456" and friends, preferred over any loose six digit run. */
    private static final Pattern LABELLED_CODE =
            Pattern.compile("(?i)(?:code|otp|pin)\\D{0,30}?(\\d{6})");

    /** Fallback: a six digit number that is not part of a longer one. */
    private static final Pattern BARE_CODE = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

    /**
     * Credential sources Gmail has already turned down in this JVM, so a stale one is not
     * offered again. A refused Gmail login is slow and counts against the account, and
     * retrying a password that was wrong a moment ago cannot start being right.
     */
    private static final Set<String> REFUSED = ConcurrentHashMap.newKeySet();

    /**
     * Checks the mailbox wiring on its own, without running a whole suite.
     *
     * <pre>mvn -o test-compile exec:java -Dexec.mainClass=org.example.VerificationCodeReader
     *     -Dexec.classpathScope=test -Dexec.args="lawma195.infinity+something@gmail.com 30"</pre>
     *
     * Worth having because "no code" has two very different causes — a mailbox that cannot
     * be read, and a message that was never sent — and telling them apart from inside a test
     * run means waiting out the whole registration flow first.
     */
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: VerificationCodeReader <recipient-alias> [timeoutSeconds]");
            return;
        }

        int timeout = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        String code = waitForCode(args[0], timeout);

        System.out.println(code.isEmpty()
                ? "No code found for " + args[0]
                : "Code for " + args[0] + " is " + code);
    }

    /**
     * Waits for the message addressed to {@code recipient} and returns its six digit code.
     *
     * @param recipient      the +alias this run registered
     * @param timeoutSeconds how long to keep looking
     * @return the code, or "" if it never arrived or the mailbox could not be read
     */
    public static String waitForCode(String recipient, int timeoutSeconds) {

        if (!MailCredentials.isConfigured()) {
            System.out.println("No mailbox password configured - looked in "
                    + MailCredentials.describe());
            return "";
        }

        System.out.println("Watching " + MAILBOX + " over IMAP for the code sent to " + recipient);

        Store store = null;

        try {
            store = connect();

            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

            while (System.currentTimeMillis() < deadline) {

                String code = findCode(store, recipient);

                if (!code.isEmpty()) {
                    System.out.println("Verification code read from the mailbox: " + code);
                    return code;
                }

                Thread.sleep(POLL_INTERVAL_MILLIS);
            }

            System.out.println("No message for " + recipient + " arrived within "
                    + timeoutSeconds + " seconds");
            return "";

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception cannotRead) {
            System.out.println("Could not read the mailbox over IMAP: " + cannotRead.getMessage());
            return "";
        } finally {
            closeQuietly(store);
        }
    }

    /**
     * Connects with the first configured password Gmail actually accepts.
     *
     * Taking only the highest-precedence credential is what broke the web suite: a revoked
     * app password in the environment shadowed a working one in the secrets file, and the
     * run reported "Invalid credentials" with a usable credential on the same machine.
     * Only the source is ever logged, never the password.
     */
    private static Store connect() throws MessagingException {

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", HOST);
        props.put("mail.imaps.port", PORT);
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "20000");
        props.put("mail.imaps.timeout", "20000");

        MessagingException lastFailure = null;

        for (Map.Entry<String, String> candidate : MailCredentials.candidates().entrySet()) {

            if (REFUSED.contains(candidate.getKey())) {
                continue;
            }

            Store store = Session.getInstance(props).getStore("imaps");

            try {
                store.connect(HOST, MAILBOX, candidate.getValue());
                System.out.println("Mailbox opened with the credential from " + candidate.getKey());
                return store;

            } catch (MessagingException rejected) {
                // Not a failure of the run - the next credential is tried and one of them
                // works. Said plainly here because a bare "Invalid credentials" in the middle
                // of a passing run reads like the cause of whatever fails later, and it is not.
                REFUSED.add(candidate.getKey());

                System.out.println("Skipping " + candidate.getKey()
                        + ": Gmail refused it (" + rejected.getMessage() + ")."
                        + " This is not fatal - the next configured credential is tried."
                        + " It looks like a revoked app password; clearing it removes this"
                        + " message and a slow failed login on every run.");

                closeQuietly(store);
                lastFailure = rejected;
            }
        }

        throw lastFailure == null
                ? new MessagingException("No mailbox password is configured anywhere")
                : lastFailure;
    }

    /**
     * Scans the newest messages for one addressed to the alias.
     *
     * The folder is reopened every pass on purpose: an IMAP folder held open keeps reporting
     * the message count it had when it was opened, so a folder opened before the mail
     * arrived would never see it and this would poll until the timeout.
     */
    private static String findCode(Store store, String recipient)
            throws MessagingException, IOException {

        Folder inbox = store.getFolder("INBOX");

        try {
            inbox.open(Folder.READ_ONLY);

            int total = inbox.getMessageCount();
            int from = Math.max(1, total - MESSAGES_TO_SCAN + 1);

            if (total == 0) {
                return "";
            }

            Message[] messages = inbox.getMessages(from, total);

            // Newest first: the wanted message is the most recent one, and on a busy mailbox
            // scanning forwards would read a pile of unrelated mail before reaching it.
            for (int i = messages.length - 1; i >= 0; i--) {

                if (addressedTo(messages[i], recipient)) {

                    String code = codeIn(textOf(messages[i]));

                    if (!code.isEmpty()) {
                        return code;
                    }
                }
            }

            return "";

        } finally {
            if (inbox.isOpen()) {
                inbox.close(false);
            }
        }
    }

    /** Whether the alias appears in any recipient header. */
    private static boolean addressedTo(Message message, String recipient) throws MessagingException {

        Address[] recipients = message.getAllRecipients();

        if (recipients == null) {
            return false;
        }

        for (Address address : recipients) {
            if (address.toString().toLowerCase().contains(recipient.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /** A labelled code wins over a bare one: "expires in 900000" should not be read as a code. */
    private static String codeIn(String text) {

        Matcher labelled = LABELLED_CODE.matcher(text);

        if (labelled.find()) {
            return labelled.group(1);
        }

        Matcher bare = BARE_CODE.matcher(text);

        return bare.find() ? bare.group(1) : "";
    }

    /** Flattens whatever the message is made of into text the patterns can be run over. */
    private static String textOf(Part part) throws MessagingException, IOException {

        Object content = part.getContent();

        if (content instanceof String) {
            return (String) content;
        }

        if (content instanceof Multipart) {

            Multipart multipart = (Multipart) content;
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(textOf(multipart.getBodyPart(i))).append('\n');
            }

            return text.toString();
        }

        return "";
    }

    private static void closeQuietly(Store store) {

        if (store == null) {
            return;
        }

        try {
            store.close();
        } catch (MessagingException ignored) {
            // Nothing useful to do while tidying up.
        }
    }
}
