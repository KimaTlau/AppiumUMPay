package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Where the mailbox password comes from, for reading the emailed registration code.
 *
 * Looked for in three places, most explicit first:
 *
 * <ol>
 *   <li>{@code -Dumpay.mail.password=...} on the command line, for a one-off run or CI</li>
 *   <li>the {@code UMPAY_MAIL_PASSWORD} environment variable</li>
 *   <li>a properties file holding {@code mail.password}</li>
 * </ol>
 *
 * Precedence alone is not enough, which is why {@link #candidates()} exists. On this
 * machine UMPAY_MAIL_PASSWORD holds a revoked Gmail app password that Gmail rejects, while
 * a working one sits in the file — so the most explicit source is not the usable one, and
 * a caller that can tell the difference should try them in turn. Logging in is that test.
 *
 * The file defaults to the UMPay web suite's copy because that is the one already kept up
 * to date on this machine, and duplicating a secret into a second checkout would only give
 * it two places to go stale. Point somewhere else with
 * {@code -Dumpay.mail.secrets=/path/to/secrets.properties}.
 *
 * Nothing here ever prints the password, only where it was found.
 */
public class MailCredentials {

    public static final String SYSTEM_PROPERTY = "umpay.mail.password";
    public static final String ENVIRONMENT_VARIABLE = "UMPAY_MAIL_PASSWORD";

    private static final String SECRETS_PATH = System.getProperty("umpay.mail.secrets",
            "C:\\Users\\Hp15s-fq2\\IdeaProjects\\UMPay\\Config\\secrets.properties");

    private static final String SECRETS_KEY = "mail.password";

    private MailCredentials() {
        // Static holder; there is nothing to construct.
    }

    /**
     * Every configured password, best first, keyed by where it came from.
     * Duplicates are dropped: a failed Gmail login is slow and counts against the account.
     */
    public static Map<String, String> candidates() {

        Map<String, String> found = new LinkedHashMap<>();

        add(found, "the " + SYSTEM_PROPERTY + " system property", System.getProperty(SYSTEM_PROPERTY));
        add(found, "the " + ENVIRONMENT_VARIABLE + " environment variable", System.getenv(ENVIRONMENT_VARIABLE));
        add(found, SECRETS_PATH, fromSecretsFile());

        return found;
    }

    /** Whether there is anything at all to try. */
    public static boolean isConfigured() {

        return !candidates().isEmpty();
    }

    /** For a log line that has to explain itself when nothing is configured. */
    public static String describe() {

        Map<String, String> candidates = candidates();

        return candidates.isEmpty()
                ? "nowhere - set " + ENVIRONMENT_VARIABLE + " or create " + SECRETS_PATH
                : String.join(", ", candidates.keySet());
    }

    private static void add(Map<String, String> found, String source, String rawValue) {

        String value = clean(rawValue);

        if (!value.isEmpty() && !found.containsValue(value)) {
            found.put(source, value);
        }
    }

    private static String fromSecretsFile() {

        File secrets = new File(SECRETS_PATH);

        if (!secrets.isFile()) {
            return "";
        }

        Properties properties = new Properties();

        try (FileInputStream in = new FileInputStream(secrets)) {
            properties.load(in);
        } catch (IOException unreadable) {
            System.out.println("Could not read " + secrets + ": " + unreadable.getMessage());
            return "";
        }

        return properties.getProperty(SECRETS_KEY, "");
    }

    /** Google shows app passwords in groups of four; the spaces are not part of it. */
    private static String clean(String value) {

        return value == null ? "" : value.replace(" ", "").trim();
    }
}
