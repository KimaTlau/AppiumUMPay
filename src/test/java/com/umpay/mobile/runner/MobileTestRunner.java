package com.umpay.mobile.runner;

import com.umpay.mobile.utility.MobileReport;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.DataProvider;

@CucumberOptions(
		features = "src/test/resources/features",
		glue = {"com.umpay.mobile.stepdefs", "com.umpay.mobile.utility"},
		// @register is held back from an unattended run: it creates a real account, needs a
		// captcha solved and a verification code read out of a mailbox, and cannot be
		// repeated with the same address. Run it deliberately:
		//   mvn test -Dcucumber.filter.tags="@register"
		//
		// @e2e is excluded because EndToEndTest.feature walks the same flows the per-feature
		// files already cover. Running both would drive every form twice on the one device
		// this suite has. Run the journey on its own with:
		//   mvn test -Dcucumber.filter.tags="@e2e"
		tags = "not @register and not @e2e",
		plugin = {"pretty",
				"html:target/cucumber-reports.html",
				"json:target/cucumber.json",
				"junit:target/cucumber.xml"},
		publish = false
)
public class MobileTestRunner extends AbstractTestNGCucumberTests {

	/**
	 * One scenario at a time.
	 *
	 * Parallel is not an option here the way it might be on the web: there is one device,
	 * and two sessions cannot drive it at once.
	 */
	@Override
	@DataProvider(parallel = false)
	public Object[][] scenarios() {
		return super.scenarios();
	}

	/**
	 * Emails the report when a TestNG suite finishes.
	 *
	 * Paired with MobileBaseClass's Cucumber AfterAll, which covers the case this one
	 * cannot: a feature launched straight from the IDE runs Cucumber without ever creating
	 * a TestNG suite. Both call the same guarded method, so a run that fires both still
	 * sends one email.
	 */
	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		MobileReport.sendReportEmail();
	}
}
