package org.example;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Emails the run's HTML report when the suite finishes, the same way the UMPay web suite
 * does, so a run that nobody watched still reports itself.
 *
 * Sending is best effort and never fails a run. A suite that passed did pass whether or not
 * the mail went out, and turning a mail problem into a red build would hide the actual
 * result. Everything here reports what happened and returns.
 *
 * Credentials come from {@link MailCredentials}, which is also what the mailbox reader uses,
 * so one working app password serves both halves. Every configured credential is tried
 * rather than only the highest-precedence one: a revoked password sitting in an environment
 * variable used to shadow a working one in the secrets file and take the report mail down
 * with it. Only the source is ever logged, never the password.
 */
public final class ReportMailer {

    /** Set -Dumpay.report.mail=false for a run that should stay quiet. */
    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("umpay.report.mail", "true"));

    private static final String HOST = System.getProperty("umpay.smtp.host", "smtp.gmail.com");

    /** 465 is implicit SSL, which needs no STARTTLS negotiation. */
    private static final String PORT = System.getProperty("umpay.smtp.port", "465");

    private static final String FROM =
            System.getProperty("umpay.mail.address", "lawma195.infinity@gmail.com");

    private static final String TO = System.getProperty("umpay.report.mail.to", FROM);

    /**
     * Largest attachment worth trying, in bytes.
     *
     * Gmail rejects a message over 25MB, and MIME encodes attachments as base64, which is
     * four bytes out for every three in. So the real ceiling is about 18MB of file, and the
     * failure when it is crossed is not a polite error - the connection drops mid-send and
     * jakarta.mail reports "Exception reading response", which looks like a network fault
     * rather than a size limit. The iOS suite's reports run to 22MB because every screenshot
     * is inlined as base64, so this is routine there rather than theoretical.
     */
    private static final long MAX_ATTACHMENT_BYTES =
            Long.getLong("umpay.report.mail.maxBytes", 18L * 1024 * 1024);

    private ReportMailer() {
        // Static holder; there is nothing to construct.
    }

    /**
     * Sends the report.
     *
     * @param subject     subject line
     * @param bodyHtml    HTML body
     * @param attachments files to attach; missing ones are skipped rather than failing
     */
    public static void send(String subject, String bodyHtml, List<File> attachments) {

        if (!ENABLED) {
            System.out.println("Report email disabled by -Dumpay.report.mail=false");
            return;
        }

        Map<String, String> candidates = MailCredentials.candidates();

        if (candidates.isEmpty()) {
            System.out.println("No mail password configured, so no report email. Looked in "
                    + MailCredentials.describe());
            return;
        }

        // Oversized reports are left on disk and named in the body rather than silently
        // dropped or allowed to kill the send. A summary that arrives beats a perfect email
        // that does not, and the report is still sitting on the machine that produced it.
        List<File> sendable = new ArrayList<>();
        StringBuilder notes = new StringBuilder();

        if (attachments != null) {
            for (File file : attachments) {

                if (file == null || !file.isFile()) {
                    continue;
                }

                if (file.length() > MAX_ATTACHMENT_BYTES) {
                    notes.append("<p><b>Report not attached:</b> ")
                            .append(file.getName())
                            .append(" is ")
                            .append(file.length() / (1024 * 1024))
                            .append("MB, over the ")
                            .append(MAX_ATTACHMENT_BYTES / (1024 * 1024))
                            .append("MB limit that survives base64 encoding into Gmail's 25MB"
                                    + " cap. It is on the run machine at<br><code>")
                            .append(file.getAbsolutePath())
                            .append("</code></p>");

                    System.out.println("Report " + file.getName() + " is "
                            + (file.length() / (1024 * 1024)) + "MB and too large to email;"
                            + " sending the summary without it.");
                } else {
                    sendable.add(file);
                }
            }
        }

        String body = bodyHtml + notes;

        Properties props = new Properties();
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", PORT);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.connectiontimeout", "20000");
        props.put("mail.smtp.timeout", "20000");

        for (Map.Entry<String, String> candidate : candidates.entrySet()) {

            if (deliver(props, candidate.getValue(), subject, body, sendable)) {
                System.out.println("Report emailed to " + TO + " using the credential from "
                        + candidate.getKey());
                return;
            }

            System.out.println("The credential from " + candidate.getKey()
                    + " could not send the report; trying the next one if there is one.");
        }

        System.out.println("No configured credential could send the report email.");
    }

    /** One attempt with one credential. Returns whether it went out. */
    private static boolean deliver(Properties props, final String password, String subject,
                                   String bodyHtml, List<File> attachments) {

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO));
            message.setSubject(subject);

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart body = new MimeBodyPart();
            body.setContent(bodyHtml, "text/html; charset=utf-8");
            multipart.addBodyPart(body);

            if (attachments != null) {
                for (File file : attachments) {

                    if (file == null || !file.isFile()) {
                        continue;
                    }

                    MimeBodyPart part = new MimeBodyPart();
                    DataSource source = new FileDataSource(file);
                    part.setDataHandler(new DataHandler(source));
                    part.setFileName(file.getName());
                    multipart.addBodyPart(part);
                }
            }

            message.setContent(multipart);
            Transport.send(message);

            return true;

        } catch (Exception refused) {
            // Expected when a credential is stale; the caller moves to the next one. No stack
            // trace, because this is not the interesting failure in a test run's output.
            System.out.println("Report email attempt failed: " + refused.getMessage());
            return false;
        }
    }
}
