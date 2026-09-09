package com.umpay.mobile.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Reads the registration captcha with ddddocr, the offline recogniser that lives
 * in the sibling Captcha project.
 *
 * The model runs as a short lived Python process rather than inside the JVM:
 * ddddocr has no Java port, and shelling out keeps 85MB of model weights out of
 * this repository. Nothing leaves the device or the machine.
 *
 * Recognition is far from perfect. On a sample of real UMPay captchas it read
 * some exactly, dropped digits on about half, and occasionally returned a
 * confident but wrong four digit answer. The length check below catches the
 * dropped-digit cases; nothing catches a confident misread. The caller is
 * therefore expected to submit the answer and, when the app rejects it, refresh
 * the image and ask again - a single reading is a guess, not the code.
 *
 * The script is looked for inside this project, which is where the captcha tool
 * now lives; a run started from the project directory finds it without any
 * configuration. Both paths can still be overridden per run, for example
 * -Dumpay.ocr.script=/somewhere/else/ocr.py.
 */
public class CaptchaSolver {

    private static final String PYTHON = System.getProperty("umpay.ocr.python",
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python312\\python.exe");

    private static final String SCRIPT = System.getProperty("umpay.ocr.script",
            "Captcha/ocr.py");

    /** 0 restricts the model to digits, which is what this app's captcha issues. */
    private static final String RANGES = System.getProperty("umpay.ocr.ranges", "0");

    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("umpay.ocr.enabled", "true"));

    /** Long enough for a cold start: each call reloads the 54MB model from disk. */
    private static final int TIMEOUT_SECONDS = 120;

    /** Whether the OCR path is switched on and its two files are actually there. */
    public static boolean isAvailable() {

        return ENABLED && new File(PYTHON).isFile() && new File(SCRIPT).isFile();
    }

    /** Says what is missing, for a run log that has to explain itself. */
    public static String availability() {

        if (!ENABLED) {
            return "disabled by -Dumpay.ocr.enabled=false";
        }
        if (!new File(PYTHON).isFile()) {
            return "python not found at " + PYTHON;
        }
        if (!new File(SCRIPT).isFile()) {
            return "ocr.py not found at " + SCRIPT;
        }
        return "ready (" + SCRIPT + ")";
    }

    /**
     * Reads the image and returns the digits, or an empty string when OCR is off,
     * failed, or produced something that is not a code of the expected length.
     * An empty return means "take a fresh captcha", never "fail the test".
     */
    public static String solve(File image, int expectedLength) {

        if (!isAvailable()) {
            System.out.println("Captcha OCR unavailable: " + availability());
            return "";
        }

        if (image == null || !image.isFile()) {
            System.out.println("Captcha OCR has no image to read");
            return "";
        }

        List<String> command = new ArrayList<>();
        command.add(PYTHON);
        command.add(SCRIPT);
        command.add(image.getAbsolutePath());
        command.add("--quiet");
        // tesseract is not installed, so its fallback would only cost an import attempt.
        command.add("--no-fallback");
        command.add("--ranges");
        command.add(RANGES);

        String reading = run(command);

        if (reading.isEmpty()) {
            return "";
        }

        if (reading.length() != expectedLength) {
            System.out.println("Captcha OCR read \"" + reading + "\", which is not "
                    + expectedLength + " characters - discarding it as a misread");
            return "";
        }

        return reading;
    }

    private static String run(List<String> command) {

        Process process = null;

        try {
            long startedAt = System.currentTimeMillis();

            process = new ProcessBuilder(command).start();

            String output = read(process.getInputStream());
            String errors = read(process.getErrorStream());

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                System.out.println("Captcha OCR did not finish within " + TIMEOUT_SECONDS + " seconds");
                return "";
            }

            if (process.exitValue() != 0) {
                System.out.println("Captcha OCR failed with exit code " + process.exitValue()
                        + (errors.isBlank() ? "" : ": " + firstLine(errors)));
                return "";
            }

            String reading = output.trim();

            System.out.println("Captcha OCR read \"" + reading + "\" in "
                    + (System.currentTimeMillis() - startedAt) + "ms");

            return reading;

        } catch (IOException cannotStart) {
            System.out.println("Could not start the captcha OCR process: " + cannotStart.getMessage());
            return "";
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return "";
        }
    }

    private static String read(InputStream stream) throws IOException {

        StringBuilder text = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append(System.lineSeparator());
            }
        }

        return text.toString();
    }

    /** ddddocr prints a long remediation banner on failure; the reason is on line one. */
    private static String firstLine(String text) {

        String[] lines = text.trim().split("\\R");
        return lines.length == 0 ? "" : lines[0];
    }
}
