package com.umpay.mobile.utility;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a self-contained HTML report for the mobile QA suite.
 *
 * Screenshots are embedded as base64 rather than linked, so the report is a single
 * file that can be emailed or opened anywhere without the screenshots/ folder.
 */
public class ExtentReportListener implements ITestListener, ISuiteListener {

    private static final String REPORT_DIR = "Reports";

    private static ExtentReports extent;

    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private static String reportPath;

    /** Human-readable case ids and intent, keyed by test method name. */
    private static final Map<String, String[]> CASES = new HashMap<>();

    static {
        CASES.put("testLogin", new String[]{"TC-M-01 Login", "Sign in with a valid email and password and land on the wallet home screen."});
        CASES.put("testDeposit", new String[]{"TC-M-02 Deposit", "Open Deposit, choose a currency and payment type, and enter an amount. Stops before Confirm."});
        CASES.put("testWithdraw", new String[]{"TC-M-03 Withdraw", "Open Withdraw, enter an amount and select a saved payout account. Stops before Confirm."});
        CASES.put("testTransfer", new String[]{"TC-M-04 Transfer", "Open Transfer, pick a saved UMPay template and enter an amount. Stops before Confirm."});
        CASES.put("testConvert", new String[]{"TC-M-05 Convert", "Open Convert, choose a target currency and enter an amount to price the conversion. Stops before Convert."});
        CASES.put("testLogout", new String[]{"TC-M-06 Logout", "Sign out from the profile panel and return to the login screen."});
        CASES.put("testRegister", new String[]{"TC-M-07 Register", "Open Register from the login screen and complete the sign-up form. Stops before submission, so no account is created."});
    }

    @Override
    public void onStart(ITestContext context) {

        new File(REPORT_DIR).mkdirs();

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        reportPath = REPORT_DIR + File.separator + "UMPay_Mobile_QA_" + stamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("UMPay Mobile QA Report");
        spark.config().setReportName("UMPay Android - Login, Deposit, Withdraw, Transfer, Convert, Logout");
        spark.config().setEncoding("utf-8");
        spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);

        extent.setSystemInfo("Application", "UMPay Android (Flutter)");
        extent.setSystemInfo("Package", "com.umpay.me");
        extent.setSystemInfo("Build", "UMPay_3.2.3_test_2.9.apk");
        extent.setSystemInfo("Device", "Redmi 13C (23106RN0DA)");
        extent.setSystemInfo("Android", "15");
        extent.setSystemInfo("Automation", "Appium 2.18.0 / UiAutomator2 4.2.3");
        extent.setSystemInfo("Environment", "test.umpay.io");
        extent.setSystemInfo("Safety", "Money-moving flows stop before the final Confirm - no funds move");
    }

    @Override
    public void onTestStart(ITestResult result) {

        String method = result.getMethod().getMethodName();
        String[] meta = CASES.getOrDefault(method, new String[]{method, ""});

        ExtentTest test = extent.createTest(meta[0], meta[1]);
        test.assignCategory("UMPay Mobile");
        CURRENT.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        CURRENT.get().log(Status.PASS, "Completed as expected.");
    }

    @Override
    public void onTestFailure(ITestResult result) {

        ExtentTest test = CURRENT.get();

        Throwable error = result.getThrowable();
        test.log(Status.FAIL, error != null ? error.toString() : "Test failed with no throwable.");

        if (error != null) {
            test.log(Status.INFO, "<pre>" + firstFrames(error) + "</pre>");
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {

        ExtentTest test = CURRENT.get();

        if (test == null) {
            String method = result.getMethod().getMethodName();
            String[] meta = CASES.getOrDefault(method, new String[]{method, ""});
            test = extent.createTest(meta[0], meta[1]);
        }

        Throwable error = result.getThrowable();
        test.log(Status.SKIP, error != null ? error.toString() : "Skipped - a test it depends on did not pass.");
    }

    @Override
    public void onFinish(ITestContext context) {

        if (extent != null) {
            extent.flush();
            System.out.println("Report written to: " + new File(reportPath).getAbsolutePath());
        }
    }

    /**
     * Emails the finished report once the whole suite is done.
     *
     * This is on the suite hook rather than {@link #onFinish(ITestContext)} deliberately.
     * ITestListener fires per &lt;test&gt; block, so a suite with more than one would send an
     * email per block; ISuiteListener fires exactly once, after every block has flushed.
     */
    @Override
    public void onFinish(ISuite suite) {

        int passed = 0;
        int failed = 0;
        int skipped = 0;

        for (ISuiteResult result : suite.getResults().values()) {
            ITestContext context = result.getTestContext();
            passed += context.getPassedTests().size();
            failed += context.getFailedTests().size();
            skipped += context.getSkippedTests().size();
        }

        String outcome = failed == 0 ? "PASSED" : "FAILED";

        String subject = "UMPay Android QA - " + outcome + " - " + passed + " passed, "
                + failed + " failed, " + skipped + " skipped - "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String body = "<h3>UMPay Android automation report</h3>"
                + "<p><b>" + outcome + "</b> &mdash; " + passed + " passed, " + failed
                + " failed, " + skipped + " skipped.</p>"
                + "<p>Suite: " + suite.getName() + "</p>"
                + "<p>The attached HTML report carries the screenshots inline, so it opens"
                + " anywhere without the screenshots folder.</p>";

        List<File> attachments = new ArrayList<>();

        if (reportPath != null) {
            attachments.add(new File(reportPath));
        }

        ReportMailer.send(subject, body, attachments);
    }

    /**
     * Called by the test's screenshot helper so each captured screen lands in the
     * report entry for the test that was running at the time.
     */
    public static void attachScreenshot(File png, String label) {

        ExtentTest test = CURRENT.get();

        if (test == null || png == null || !png.exists()) {
            return;
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(png.toPath()));
            test.info(label.replace('_', ' '),
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        } catch (IOException e) {
            test.info("Could not attach screenshot " + label + ": " + e.getMessage());
        }
    }

    /** Keeps the report readable - the full Appium stack trace is hundreds of frames. */
    private static String firstFrames(Throwable error) {

        StringBuilder sb = new StringBuilder();
        StackTraceElement[] frames = error.getStackTrace();

        for (int i = 0; i < Math.min(8, frames.length); i++) {
            sb.append("at ").append(frames[i]).append("\n");
        }

        return sb.toString();
    }

    public static String getReportPath() {
        return reportPath;
    }
}
