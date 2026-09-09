package com.umpay.mobile.utility;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.java.Scenario;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The run's HTML report, and the email it goes out in.
 *
 * The web suites do this from their BaseClass; this is the same thing for Cucumber on
 * Android, kept in its own class because the mobile hooks already carry the session
 * handling and adding the reporting to them would leave one class doing two jobs.
 *
 * It is written against Cucumber's own hooks rather than TestNG's listeners on purpose.
 * A TestNG listener sees one test method for the whole run - cucumber-testng drives every
 * scenario through the single {@code runScenario} method - so a report built that way
 * would show one entry called "runScenario" instead of the eight scenarios that ran.
 *
 * Screenshots are embedded as base64 rather than linked. The report is emailed, so a file
 * that referenced a screenshots folder on the run machine would arrive with every image
 * broken; one that carries them inline opens anywhere.
 */
public final class MobileReport {

	private static final String REPORT_DIR = "Reports";

	private static ExtentReports extent;

	private static String reportPath;

	/** The scenario currently running, so the After hook knows what to write to. */
	private static ExtentTest current;

	private static int passed;
	private static int failed;

	/** One email per run, however many hooks reach the send. */
	private static final AtomicBoolean REPORT_SENT = new AtomicBoolean(false);

	private MobileReport() {
		// Static holder; there is nothing to construct.
	}

	/** Creates the report on first use, so a run that opens no scenario writes no file. */
	private static synchronized ExtentReports report() {

		if (extent != null) {
			return extent;
		}

		new File(REPORT_DIR).mkdirs();

		String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		reportPath = REPORT_DIR + File.separator + "UMPay_Mobile_QA_" + stamp + ".html";

		ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
		spark.config().setTheme(Theme.DARK);
		spark.config().setDocumentTitle("UMPay Mobile QA Report");
		spark.config().setReportName("UMPay Android - BDD suite");
		spark.config().setEncoding("utf-8");
		spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

		extent = new ExtentReports();
		extent.attachReporter(spark);

		// Read from the factory rather than written out here, so a run pointed at the
		// emulator does not produce a report that claims it ran on the phone.
		extent.setSystemInfo("Application", "UMPay Android (Flutter)");
		extent.setSystemInfo("Package", "com.umpay.me");
		extent.setSystemInfo("Device", DeviceFactory.deviceName());
		extent.setSystemInfo("UDID", DeviceFactory.udid());
		extent.setSystemInfo("Android", DeviceFactory.platformVersion());
		extent.setSystemInfo("Framework", "Cucumber BDD / Appium UiAutomator2");
		extent.setSystemInfo("Safety",
				"Money-moving scenarios stop before the final Confirm - no funds move");

		return extent;
	}

	/** Opens a report entry for a scenario that is about to run. */
	public static void beginScenario(Scenario scenario) {

		try {
			current = report().createTest(scenario.getName());

			for (String tag : scenario.getSourceTagNames()) {
				current.assignCategory(tag);
			}

		} catch (Exception e) {
			// A report problem must not take a scenario down with it.
			System.out.println("Could not open the report entry: " + e.getMessage());
			current = null;
		}
	}

	/**
	 * Closes the entry for a finished scenario and writes the report out.
	 *
	 * Flushed every scenario rather than once at the end, so a run that is killed part way
	 * through still leaves a readable report of what it got through.
	 *
	 * @param screenshot the end-of-scenario screen, or null if the session had already gone
	 */
	public static void finishScenario(Scenario scenario, byte[] screenshot) {

		try {
			if (scenario.isFailed()) {
				failed++;
			} else {
				passed++;
			}

			if (current == null) {
				return;
			}

			String message = "Scenario " + scenario.getStatus().name().toLowerCase();

			if (screenshot != null && screenshot.length > 0) {
				String base64 = Base64.getEncoder().encodeToString(screenshot);
				current.log(scenario.isFailed() ? Status.FAIL : Status.PASS, message,
						MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
			} else {
				current.log(scenario.isFailed() ? Status.FAIL : Status.PASS,
						message + " (no screenshot - the session had already ended)");
			}

		} catch (Exception e) {
			System.out.println("Could not record the end of the scenario: " + e.getMessage());

		} finally {
			current = null;

			try {
				if (extent != null) {
					extent.flush();
				}
			} catch (Exception e) {
				System.out.println("Could not flush the report: " + e.getMessage());
			}
		}
	}

	/**
	 * Emails the report, at most once per run.
	 *
	 * Nothing in here may throw. It runs after the tests have finished, so losing a run's
	 * result to a mail problem would be worse than not sending the mail - which is also why
	 * {@link ReportMailer} treats a refused send as something to report, not to fail on.
	 */
	public static void sendReportEmail() {

		if (!REPORT_SENT.compareAndSet(false, true)) {
			return;
		}

		try {
			if (extent == null || reportPath == null) {
				System.out.println("No scenarios ran, so there is no report to email.");
				return;
			}

			extent.flush();

			File file = new File(reportPath);

			System.out.println("Report written to: " + file.getAbsolutePath());

			String outcome = failed == 0 ? "PASSED" : "FAILED";

			String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

			String subject = "UMPay Android QA - " + outcome + " - " + passed + " passed, "
					+ failed + " failed - " + stamp;

			String body = "<h3>UMPay Android automation report</h3>"
					+ "<p><b>" + outcome + "</b> &mdash; " + passed + " passed, "
					+ failed + " failed.</p>"
					+ "<p>Device: " + DeviceFactory.deviceName()
					+ " (" + DeviceFactory.udid() + "), Android "
					+ DeviceFactory.platformVersion() + ".</p>"
					+ "<p>The attached report carries its screenshots inline, so it opens"
					+ " anywhere without the run machine's folders.</p>";

			List<File> attachments = new ArrayList<>();

			if (file.isFile()) {
				attachments.add(file);
			}

			ReportMailer.send(subject, body, attachments);

		} catch (Exception e) {
			System.out.println("The report email could not be prepared: " + e.getMessage());
		}
	}

	/** Where this run's report was written, or null if none was created. */
	public static String reportPath() {
		return reportPath;
	}
}
