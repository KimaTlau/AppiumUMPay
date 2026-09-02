package com.umpay.mobile.utility;

import io.appium.java_client.android.AndroidDriver;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;

/**
 * Holds the one session a scenario runs against, and the screenshot it leaves behind.
 *
 * The fields are static because Cucumber builds every step definition class fresh for each
 * scenario, and they all have to reach the same session.
 *
 * One session per scenario, as the web suites do one browser per scenario. It costs a few
 * seconds and it means no scenario can be broken by what a previous one left on screen.
 */
public class MobileBaseClass {

	public static AndroidDriver driver;

	@Before(order = 0)
	public void openApp(Scenario scenario) {

		MobileReport.beginScenario(scenario);

		if (driver == null) {
			driver = DeviceFactory.startApp();
		}
	}

	/**
	 * Attaches a screenshot to the report and closes the session.
	 *
	 * The screenshot is wrapped so the quit in the finally block always runs. A session
	 * that has already died throws on the screenshot, and if that skipped the quit the
	 * device would be left held by an abandoned session - which the next run then cannot
	 * start against.
	 */
	@After
	public void closeApp(Scenario scenario) {

		byte[] screenshot = null;

		if (scenario.isFailed()) {

			String failureReport = FailureReport.of(scenario);

			// To the console, so a terminal run shows it without opening anything, and to
			// the Cucumber report so CI carries it beside the screenshot below.
			System.out.println(failureReport);

			scenario.attach(failureReport.getBytes(java.nio.charset.StandardCharsets.UTF_8),
					"text/plain", "how to reproduce this failure");
		}

		try {
			if (driver != null) {
				screenshot = driver.getScreenshotAs(OutputType.BYTES);
				scenario.attach(screenshot, "image/png", scenario.getName());
			}

		} catch (Exception e) {
			System.out.println("Could not attach the end-of-scenario screenshot: " + e.getMessage());

		} finally {

			// Before the quit, because the report wants the screen as the scenario left it
			// and a screenshot taken after the session has gone is not available at all.
			MobileReport.finishScenario(scenario, screenshot);

			DeviceFactory.quitApp(driver);
			driver = null;
		}
	}

	/**
	 * Emails the run's report when Cucumber finishes.
	 *
	 * This and MobileTestRunner's AfterSuite call the same guarded method on purpose,
	 * because they cover different ways of starting a run and neither covers both:
	 *
	 *   mvn test, or a TestNG run configuration  - both fire
	 *   a .feature launched straight from the IDE - only this one, because IntelliJ runs
	 *                                              Cucumber itself and no TestNG suite
	 *                                              ever exists
	 *
	 * Whichever arrives first sends the complete report; the guard makes the other a
	 * no-op, so a plain run does not send two copies.
	 */
	@AfterAll
	public static void cucumberAfterAll() {
		MobileReport.sendReportEmail();
	}
}
