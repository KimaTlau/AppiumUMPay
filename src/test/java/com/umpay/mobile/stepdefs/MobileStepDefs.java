package com.umpay.mobile.stepdefs;

import com.umpay.mobile.pages.DashboardPage;
import com.umpay.mobile.pages.LanguagePage;
import com.umpay.mobile.pages.LoginPage;
import com.umpay.mobile.pages.MoneyFormPage;
import com.umpay.mobile.pages.ProfilePage;
import com.umpay.mobile.utility.DeviceFactory;
import com.umpay.mobile.utility.ExcelDataProvider;
import com.umpay.mobile.utility.MobileBaseClass;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The steps every mobile feature is written from.
 *
 * The page objects are built lazily rather than in a constructor: Cucumber creates this
 * class per scenario while the session is opened by the Before hook, so a field
 * initialised too early would capture a driver that does not exist yet.
 */
public class MobileStepDefs {

	private LanguagePage languagePage;
	private LoginPage loginPage;
	private DashboardPage dashboardPage;
	private MoneyFormPage moneyForm;
	private ProfilePage profilePage;

	/** The form the current scenario is filling in, so later steps know what they are on. */
	private String openForm;

	/**
	 * The amount the scenario took out of its test data, so the step that checks the form
	 * accepted it does not have to name the figure a second time.
	 */
	private String enteredAmount;

	/*
	 * Where each workbook keeps what these steps read.
	 *
	 * Every workbook under TestData/ holds the login id in column 1 and the password in
	 * column 2, which is what lets one login step serve every feature: a scenario names its
	 * own module's workbook and the credentials come out of it. The amount sits in a
	 * different column per module, so each step that reads one names its own.
	 *
	 *   Login_TestData     0 Scenario | 1 UserName | 2 Password | 3 FirstName | 4 LastName | 5 Zip
	 *   Deposit_TestData   0 Scenario | 1 LoginID  | 2 Password | 3 Amount    | 4 Currency | ...
	 *   Withdraw_TestData  0 Scenario | 1 LoginID  | 2 Password | 3 Amount    | 4 Currency | ...
	 *   Convert_TestData   0 Scenario | 1 LoginID  | 2 Password | 3 FromCurrency | 4 ToCurrency | 5 Amount | 6 ExpectedMessage
	 */
	private static final int LOGIN_ID = 1;
	private static final int PASSWORD = 2;
	private static final int DEPOSIT_AMOUNT = 3;
	private static final int WITHDRAW_AMOUNT = 3;
	private static final int CONVERT_AMOUNT = 5;

	private LanguagePage language() {
		if (languagePage == null) {
			languagePage = new LanguagePage(MobileBaseClass.driver);
		}
		return languagePage;
	}

	private LoginPage login() {
		if (loginPage == null) {
			loginPage = new LoginPage(MobileBaseClass.driver);
		}
		return loginPage;
	}

	private DashboardPage dashboard() {
		if (dashboardPage == null) {
			dashboardPage = new DashboardPage(MobileBaseClass.driver);
		}
		return dashboardPage;
	}

	private MoneyFormPage form() {
		if (moneyForm == null) {
			moneyForm = new MoneyFormPage(MobileBaseClass.driver);
		}
		return moneyForm;
	}

	private ProfilePage profile() {
		if (profilePage == null) {
			profilePage = new ProfilePage(MobileBaseClass.driver);
		}
		return profilePage;
	}

	// ------------------------------------------------------------------
	// Getting to a known starting point
	// ------------------------------------------------------------------

	/**
	 * Puts the app on the login screen whatever state it started in.
	 *
	 * The app keeps its session between runs, so it may open on the language chooser, the
	 * login screen, or already signed in. Rather than assume, each possibility is handled:
	 * a scenario that begins by asserting where it is would otherwise fail for a reason
	 * that has nothing to do with what it tests.
	 */
	@Given("the UMPay app is open on the login screen")
	public void theAppIsOnTheLoginScreen() {

		/*
		 * Before anything is believed about what is on screen.
		 *
		 * The notification shade is drawn over the app rather than replacing it, so the
		 * dashboard's tiles stay in the accessibility tree behind it and every check below
		 * reports a healthy screen that cannot actually be tapped. Clearing it first is what
		 * makes the rest of this method's reasoning sound.
		 */
		dashboard().closeNotificationShade();

		language().chooseIfAsked("English");

		if (login().isShowing()) {
			return;
		}

		/*
		 * The app keeps its task between sessions, so a scenario can open on whatever the
		 * previous one left behind rather than on a launch screen. An earlier version
		 * handled only "signed in on the dashboard", and when a scenario died on the
		 * profile panel every scenario after it failed saying the login screen had not
		 * appeared - six identical failures that were all one piece of leftover state.
		 *
		 * So: sign out from wherever we are. Already on the profile panel, go straight
		 * out; anywhere else, work back to the dashboard first.
		 */
		if (profile().isShowing()) {
			profile().signOut();

		} else {
			/*
			 * Back out until something recognisable is in front of us.
			 *
			 * Deliberately not a fixed number of presses. A previous version pressed Back
			 * six times looking only for the dashboard, so a run that had been left on the
			 * sign-up screen went back to login on the first press and then kept going,
			 * walking straight out of the app - and the step failed saying the login screen
			 * had not appeared while the app was no longer running.
			 *
			 * Either destination is fine here: login is where we want to be, and the
			 * dashboard means we are signed in and can sign out below.
			 */
			for (int press = 0; press < 5; press++) {

				if (login().isShowing() || dashboard().isShowing(Duration.ofSeconds(2))) {
					break;
				}

				/*
				 * Stop backing out the moment the app is no longer in front.
				 *
				 * Back from the app's first screen leaves the phone on its launcher, and no
				 * amount of further pressing brings it back - a previous version kept going
				 * and ended the run on the MIUI home screen, reporting that the login screen
				 * had not appeared while UMPay was not even running.
				 */
				if (!dashboard().isAppInForeground()) {
					dashboard().bringAppToFront();
					continue;
				}

				dashboard().pressBackFromAnywhere();
			}

			if (!dashboard().isAppInForeground()) {
				dashboard().bringAppToFront();
			}

			if (dashboard().isShowing(Duration.ofSeconds(5))) {
				dashboard().openProfile();
				profile().signOut();
			}
		}

		assertTrue(login().isShowing(),
				"The app did not reach the login screen");

	}

	@Given("the UMPay app is open")
	public void theAppIsOpen() {

		language().chooseIfAsked("English");

	}

	/**
	 * Signs in with the credentials held in a test data workbook.
	 *
	 * Phrased the way the web suite phrases it, and reading the same two columns, so a
	 * scenario reads identically in either project. The workbook named is the module's own
	 * rather than Login_TestData: every workbook carries the credentials in columns 1 and 2
	 * for exactly this reason, which keeps a scenario down to one Examples row instead of
	 * having to name two files.
	 */
	@Given("I log into the UMPay application with valid credentials using {string} of {string} of {string}")
	public void iAmSignedIn(String rowNumber, String excelSheetName, String excelFileName) {

		int row = Integer.parseInt(rowNumber);
		ExcelDataProvider excel = new ExcelDataProvider(excelFileName, excelSheetName);

		theAppIsOnTheLoginScreen();

		login().loginWithEmail(excel.getStringData(excelSheetName, row, LOGIN_ID),
				excel.getStringData(excelSheetName, row, PASSWORD));

		assertTrue(dashboard().isShowing(), "Signing in did not reach the dashboard");

	}

	// ------------------------------------------------------------------
	// Language
	// ------------------------------------------------------------------

	@Then("the language chooser should offer {string}")
	public void theLanguageChooserShouldOffer(String languageName) {

		assertTrue(language().isShowing(), "The language chooser is not on screen");

	}

	@When("I choose the language {string}")
	public void iChooseTheLanguage(String languageName) {

		language().chooseIfAsked(languageName);

	}

	// ------------------------------------------------------------------
	// Login
	// ------------------------------------------------------------------

	@Then("the login screen should offer both sign in methods")
	public void theLoginScreenShouldOfferBothMethods() {

		assertTrue(login().isShowing(), "The login screen is not on screen");
		assertTrue(login().offersRegistration(), "The login screen has no Register now link");
		assertTrue(login().offersPasswordRecovery(), "The login screen has no Forgot Password link");

	}

	/**
	 * Signs in from the login screen, for a scenario that asserted it was there first.
	 *
	 * The difference from the step above is where it starts: this one expects the login
	 * screen to be on display already and does nothing to get there, so a scenario about
	 * signing in tests signing in rather than the recovery that precedes it.
	 */
	@When("I sign in with the credentials in {string} of {string} of {string}")
	public void iSignInWithEmail(String rowNumber, String excelSheetName, String excelFileName) {

		int row = Integer.parseInt(rowNumber);
		ExcelDataProvider excel = new ExcelDataProvider(excelFileName, excelSheetName);

		login().loginWithEmail(excel.getStringData(excelSheetName, row, LOGIN_ID),
				excel.getStringData(excelSheetName, row, PASSWORD));

	}

	@Then("I should reach the dashboard")
	public void iShouldReachTheDashboard() {

		assertTrue(dashboard().isShowing(), "The dashboard did not appear after signing in");

	}

	@Then("the dashboard should offer the money actions")
	public void theDashboardShouldOfferTheMoneyActions() {

		assertTrue(dashboard().offersTiles(List.of(
						DashboardPage.DEPOSIT, DashboardPage.WITHDRAW,
						DashboardPage.TRANSFER, DashboardPage.CONVERT)),
				"The dashboard is missing one of its money actions");

	}

	// ------------------------------------------------------------------
	// The money forms
	// ------------------------------------------------------------------

	@When("I open the {string} form from the dashboard")
	public void iOpenTheForm(String tile) {

		dashboard().returnHome();
		dashboard().openTile(tile);

		openForm = tile;

	}

	@Then("the {string} form should be shown")
	public void theFormShouldBeShown(String heading) {

		assertTrue(form().isShowing(heading), "The " + heading + " form did not open");

	}

	/**
	 * The three money forms each take their amount from their own module's workbook.
	 *
	 * They are three steps rather than one because the column differs per workbook -
	 * Deposit and Withdraw keep the amount at column 3, Convert at column 5, the same
	 * layouts the web suite reads. A single step would have to guess which workbook it had
	 * been handed, and guessing wrong reads a currency code as an amount.
	 */
	@When("I enter the deposit amount in {string} of {string} of {string}")
	public void iEnterTheDepositAmount(String rowNumber, String excelSheetName, String excelFileName) {

		enterAmountFromData(rowNumber, excelSheetName, excelFileName, DEPOSIT_AMOUNT);

	}

	@When("I enter the withdrawal amount in {string} of {string} of {string}")
	public void iEnterTheWithdrawalAmount(String rowNumber, String excelSheetName, String excelFileName) {

		enterAmountFromData(rowNumber, excelSheetName, excelFileName, WITHDRAW_AMOUNT);

	}

	@When("I enter the conversion amount in {string} of {string} of {string}")
	public void iEnterTheConversionAmount(String rowNumber, String excelSheetName, String excelFileName) {

		enterAmountFromData(rowNumber, excelSheetName, excelFileName, CONVERT_AMOUNT);

	}

	private void enterAmountFromData(String rowNumber, String excelSheetName,
			String excelFileName, int column) {

		int row = Integer.parseInt(rowNumber);
		ExcelDataProvider excel = new ExcelDataProvider(excelFileName, excelSheetName);

		enteredAmount = excel.getStringData(excelSheetName, row, column);

		assertTrue(form().hasAmountField(),
				"The " + openForm + " form has no amount field to type into");

		form().enterAmount(enteredAmount);

	}

	@Then("the {string} action should be disabled")
	public void theActionShouldBeDisabled(String label) {

		assertTrue(form().hasAction(label),
				"The " + openForm + " form has no " + label + " action at all");

		assertFalse(form().isActionEnabled(label),
				"The " + label + " action is already enabled on an empty " + openForm + " form");

	}

	@Then("the {string} action should become enabled")
	public void theActionShouldBecomeEnabled(String label) {

		assertTrue(form().isActionEnabled(label),
				"The " + label + " action never became enabled on the " + openForm
						+ " form after entering an amount");

	}

	/**
	 * Checks the form kept what the previous step typed into it.
	 *
	 * The figure is not named again here: it came out of the workbook a moment ago and
	 * repeating it in the scenario would let the two drift apart, so the step that entered
	 * it remembers it for this one.
	 */
	@Then("the amount should be accepted")
	public void theAmountShouldBeAccepted() {

		assertTrue(enteredAmount != null,
				"No amount has been entered yet, so there is nothing to check");

		String shown = form().getAmount();

		assertTrue(shown != null && shown.contains(enteredAmount),
				"The amount field shows " + shown + " rather than " + enteredAmount);

	}

	/**
	 * The scenarios stop here on purpose.
	 *
	 * Confirming moves real money on the test environment and cannot be undone by a test,
	 * so no step and no page object method presses it. This step exists to say so in the
	 * feature file, where the reader is, rather than only in a comment in the code.
	 */
	@Then("the transaction is deliberately not submitted")
	public void theTransactionIsDeliberatelyNotSubmitted() {

		System.out.println("Stopping on a completed " + openForm
				+ " form: submitting would move real money and cannot be undone by a test.");

	}

	/**
	 * A section that only appears once the form has enough to ask for the next thing.
	 *
	 * On Deposit the Payment information block does not exist until an amount is entered,
	 * so finding it is proof the amount registered and the form moved on - which is more
	 * than reading the field back tells you.
	 */
	@Then("the form should ask for {string}")
	public void theFormShouldAskFor(String section) {

		assertTrue(form().offersOption(section) || form().hasSection(section),
				"The " + openForm + " form never showed the " + section + " section");

	}

	@Then("the transfer hub should offer {string}")
	public void theTransferHubShouldOffer(String route) {

		assertTrue(form().offersOption(route),
				"The transfer hub does not offer the " + route + " route");

	}

	@When("I select the option {string}")
	public void iSelectTheOption(String label) {

		form().chooseOption(label);

	}

	// ------------------------------------------------------------------
	// Signing out
	// ------------------------------------------------------------------

	@When("I sign out")
	public void iSignOut() {

		dashboard().returnHome();
		dashboard().openProfile();
		profile().signOut();

	}

	@Then("I should be back on the login screen")
	public void iShouldBeBackOnTheLoginScreen() {

		assertTrue(login().isShowing(), "The app did not return to the login screen");
		assertFalse(dashboard().isShowing(Duration.ofSeconds(3)),
				"The dashboard is still showing after signing out");

	}

	// ------------------------------------------------------------------
	// Where the run is pointed
	// ------------------------------------------------------------------

	@Then("the session should be running against the configured device")
	public void theSessionShouldBeRunningAgainstTheConfiguredDevice() {

		System.out.println("Running against " + DeviceFactory.deviceName()
				+ " (" + DeviceFactory.udid() + ")");

		// Asked of a page rather than of the driver: a step reaching for the session
		// directly is the one bit of coupling the page objects exist to avoid.
		assertTrue(dashboard().isAppInForeground() || login().isShowing(),
				"No session reached the application");

	}
}
