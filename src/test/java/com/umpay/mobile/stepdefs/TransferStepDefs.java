package com.umpay.mobile.stepdefs;

import com.umpay.mobile.pages.DashboardPage;
import com.umpay.mobile.pages.TransferHubPage;
import com.umpay.mobile.pages.TransferTemplatesPage;
import com.umpay.mobile.pages.UnionPayTransferPage;
import com.umpay.mobile.pages.WalletTransferPage;
import com.umpay.mobile.utility.ExcelDataProvider;
import com.umpay.mobile.utility.MobileBaseClass;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The steps the Transfer feature is written from.
 *
 * Kept apart from MobileStepDefs because Transfer is a module rather than a form: a hub,
 * a template list and three different screens behind it. Putting these in the shared class
 * would have made it the place everything goes, which is how step definition files stop
 * being readable.
 *
 * The pages are built lazily for the same reason as the other step class: Cucumber creates
 * this object per scenario while the session is opened by the Before hook, so a field
 * initialised in a constructor would capture a driver that does not exist yet.
 */
public class TransferStepDefs {

	private DashboardPage dashboardPage;
	private TransferHubPage hubPage;
	private TransferTemplatesPage templatesPage;
	private WalletTransferPage walletPage;
	private UnionPayTransferPage unionPayPage;

	private DashboardPage dashboard() {
		if (dashboardPage == null) {
			dashboardPage = new DashboardPage(MobileBaseClass.driver);
		}
		return dashboardPage;
	}

	private TransferHubPage hub() {
		if (hubPage == null) {
			hubPage = new TransferHubPage(MobileBaseClass.driver);
		}
		return hubPage;
	}

	private TransferTemplatesPage templates() {
		if (templatesPage == null) {
			templatesPage = new TransferTemplatesPage(MobileBaseClass.driver);
		}
		return templatesPage;
	}

	private WalletTransferPage wallet() {
		if (walletPage == null) {
			walletPage = new WalletTransferPage(MobileBaseClass.driver);
		}
		return walletPage;
	}

	private UnionPayTransferPage unionPay() {
		if (unionPayPage == null) {
			unionPayPage = new UnionPayTransferPage(MobileBaseClass.driver);
		}
		return unionPayPage;
	}

	// ------------------------------------------------------------------
	// The hub
	// ------------------------------------------------------------------

	@When("I open the transfer hub")
	public void iOpenTheTransferHub() {

		dashboard().returnHome();
		dashboard().openTile(DashboardPage.TRANSFER);

		assertTrue(hub().isShowing(), "The transfer hub did not open");

	}

	@Then("the transfer hub should offer the route {string}")
	public void theHubShouldOfferTheRoute(String route) {

		assertTrue(hub().offersRoute(route),
				"The transfer hub does not offer the " + route + " route");

	}

	@When("I take the {string} route")
	public void iTakeTheRoute(String route) {

		hub().openRoute(route);

	}

	@Then("the app should say the service is unavailable")
	public void theAppShouldSayTheServiceIsUnavailable() {

		assertTrue(hub().showsUnavailableWarning(),
				"Taking a route under maintenance raised no warning at all");

		assertTrue(hub().warningSays("The service is currently unavailable"),
				"The warning did not say the service is unavailable");

		// Cleared here so the scenario does not hand the next one a dialog to work out.
		hub().dismissWarning();

	}

	// ------------------------------------------------------------------
	// Saved templates
	// ------------------------------------------------------------------

	@Then("the saved template list should be shown")
	public void theSavedTemplateListShouldBeShown() {

		assertTrue(templates().isShowing(), "The Select Template list did not open");

	}

	@Then("the template list should hold at least one saved payee")
	public void theTemplateListShouldHoldAtLeastOnePayee() {

		int count = templates().templateCount();

		System.out.println("Saved templates on this account: " + count);

		assertTrue(count > 0, "The template list opened but has nothing in it");

	}

	// ------------------------------------------------------------------
	// UMPay wallet to wallet
	// ------------------------------------------------------------------

	@Then("the wallet transfer form should be shown")
	public void theWalletTransferFormShouldBeShown() {

		assertTrue(wallet().isShowing(), "The wallet transfer form did not open");

	}

	@Then("the wallet form should show the sending wallet and its balance")
	public void theWalletFormShouldShowTheSendingWallet() {

		assertTrue(wallet().hasSection("From Wallet"),
				"The form does not name the wallet being sent from");

		assertTrue(wallet().showsSourceWalletBalance(),
				"The sending wallet is shown without a balance on it");

	}

	@Then("the wallet form should ask for {string}")
	public void theWalletFormShouldAskFor(String section) {

		assertTrue(wallet().hasSection(section),
				"The wallet form has no " + section + " section");

	}

	@Then("the wallet form should offer a destination wallet field")
	public void theWalletFormShouldOfferADestinationField() {

		assertTrue(wallet().hasDestinationField(),
				"The wallet form has no box for the destination wallet");

	}

	@Then("the wallet transfer should not be sendable yet")
	public void theWalletTransferShouldNotBeSendableYet() {

		assertTrue(wallet().hasTransferAction(),
				"The wallet form has no Transfer action at all");

		assertFalse(wallet().canSendTransfer(),
				"The Transfer action is already enabled on an empty wallet form");

	}

	@Then("the wallet form should offer both parties as the fee payer")
	public void theWalletFormShouldOfferBothFeePayers() {

		assertTrue(wallet().offersFeePayer(WalletTransferPage.FEE_PAID_BY_ME),
				"The form does not offer to charge the fee to me");

		assertTrue(wallet().offersFeePayer(WalletTransferPage.FEE_PAID_BY_OTHER),
				"The form does not offer to charge the fee to the other party");

	}

	@When("I choose {string} to pay the fee")
	public void iChooseWhoPaysTheFee(String option) {

		wallet().chooseFeePayer(option);

	}

	@Then("the fee payer choice {string} should still be offered")
	public void theFeePayerChoiceShouldStillBeOffered(String option) {

		assertTrue(wallet().offersFeePayer(option),
				"The " + option + " choice disappeared after being chosen");

	}

	@When("I write {string} in the wallet remark box")
	public void iWriteInTheRemarkBox(String remark) {

		wallet().enterRemark(remark);

	}

	@Then("the remark box should report {string}")
	public void theRemarkBoxShouldReport(String expected) {

		String actual = wallet().remarkCounter();

		assertEquals(actual, expected,
				"The remark counter reads " + actual + " rather than " + expected);

	}

	// ------------------------------------------------------------------
	// UnionPay
	// ------------------------------------------------------------------

	@Then("the UnionPay transfer form should be shown")
	public void theUnionPayFormShouldBeShown() {

		assertTrue(unionPay().isShowing(), "The UnionPay transfer form did not open");

	}

	@Then("the UnionPay form should state {string}")
	public void theUnionPayFormShouldState(String label) {

		assertTrue(unionPay().shows(label),
				"The UnionPay form does not show " + label);

	}

	@Then("the UnionPay form should not offer {string}")
	public void theUnionPayFormShouldNotOffer(String label) {

		assertTrue(unionPay().doesNotShow(label),
				"The UnionPay form shows " + label + ", which this route is not expected to have");

	}

	/*
	 * Where GlobalTransfer_TestData keeps what this step reads.
	 *
	 * The first eleven columns are the web workbook's, unchanged. Amount is the twelfth and
	 * has no counterpart there: the web flow picks a saved template and sends it, while the
	 * scenarios here type an amount and assert how the form prices it.
	 *
	 *   0 Scenario | 1 LoginID | 2 Password | 3 Currency | 4 ReceiverCardNumber | 5 Pin
	 *   6 FirstName | 7 SurName | 8 Purpose | 9 SourceOfFund | 10 Address | 11 Amount
	 */
	private static final int UNIONPAY_AMOUNT = 11;

	@When("I enter the UnionPay amount in {string} of {string} of {string}")
	public void iEnterTheUnionPayAmount(String rowNumber, String excelSheetName, String excelFileName) {

		int row = Integer.parseInt(rowNumber);
		ExcelDataProvider excel = new ExcelDataProvider(excelFileName, excelSheetName);

		unionPay().enterAmount(excel.getStringData(excelSheetName, row, UNIONPAY_AMOUNT));

	}

	@Then("the UnionPay transfer should not be sendable yet")
	public void theUnionPayTransferShouldNotBeSendableYet() {

		assertTrue(unionPay().hasTransferAction(),
				"The UnionPay form has no Transfer action at all");

		assertFalse(unionPay().canSendTransfer(),
				"The Transfer action is enabled when it should not be");

	}

	@Then("the transfer should be priced")
	public void theTransferShouldBePriced() {

		assertTrue(unionPay().showsOrderDetails(),
				"The form never priced the amount that was entered");

	}

	@Then("the transfer should not be priced")
	public void theTransferShouldNotBePriced() {

		assertTrue(unionPay().hidesOrderDetails(),
				"The form priced an amount it should have rejected");

	}

	@Then("the priced transfer should show {string}")
	public void thePricedTransferShouldShow(String row) {

		assertTrue(unionPay().shows(row),
				"The order details do not include " + row);

	}

	/**
	 * The form priced it, so it should offer to send it.
	 *
	 * The account is asked about first. This case failed for a while reading as a defect - a
	 * fully priced transfer whose Transfer action never became enabled - and it was not one:
	 * the card held 4.75 USD, the scenario asked to send 50, and the app priced the transfer
	 * anyway while saying "Insufficient Balance" and leaving Transfer unclickable. That is
	 * the application being right. Naming the balance against the amount is what tells the
	 * difference between an unfunded account and a broken form, and without it this case
	 * quietly accuses the app of the wrong thing.
	 */
	@Then("the UnionPay transfer should become sendable")
	public void theUnionPayTransferShouldBecomeSendable() {

		if (unionPay().saysInsufficientBalance()) {

			throw new AssertionError(
					"BLOCKED BY FUNDING, not a defect: the form priced the transfer correctly and"
							+ " then declined it. The form says \"" + unionPay().availableBalance()
							+ "\" and \"" + unionPay().minimumAccepted() + "\", while this"
							+ " scenario asked to send " + unionPay().enteredAmount() + ". Fund the"
							+ " card, or point the scenario at an account that can cover it,"
							+ " before reading anything into the Transfer action.");
		}

		assertTrue(unionPay().canSendTransfer(),
				"The Transfer action never became enabled on a priced form the account can"
						+ " afford. The form says \"" + unionPay().availableBalance() + "\" and"
						+ " the scenario asked to send " + unionPay().enteredAmount());

	}

	/**
	 * Says in the feature file where these scenarios stop.
	 *
	 * Same rule as the other money forms: no step and no page object method presses
	 * Transfer, because a transfer cannot be undone by a test.
	 */
	@Then("the transfer is deliberately not sent")
	public void theTransferIsDeliberatelyNotSent() {

		System.out.println("Stopping on a ready transfer form: sending would move real money"
				+ " and cannot be undone by a test.");

	}
}
