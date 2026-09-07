package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The wallet-to-wallet transfer form, behind "UMPay to UMPay Wallet".
 *
 * A different form from the UnionPay ones, which is why it is a different class: it moves
 * money between UMPay wallets by their UUID, it asks which side pays the fee, and it can
 * save what was filled in as a template.
 *
 * THE AMOUNT BOX CANNOT BE TYPED INTO
 *
 * Everything on this screen is reachable except the amount. The box under "Transfer
 * Amount" is an android.view.View with no description, no text and clickable="false" -
 * there is no accessibility node to send text to, and a tap where it sits produces no
 * input field. The other three money forms in the app expose theirs as an ordinary
 * EditText, so this is particular to this screen rather than something Flutter does
 * everywhere.
 *
 * That is why nothing here enters an amount, and why the scenarios cover the parts of the
 * form that can be driven. It is a testability defect worth raising with the app team: one
 * Semantics wrapper on that field would make the whole route automatable.
 *
 * NOTHING HERE SENDS THE TRANSFER. There is no method that taps Transfer, for the same
 * reason the deposit and withdrawal forms have none.
 */
public class WalletTransferPage extends BasePage {

	private static final By TITLE = MobileLocators.exactly("To UMPay Wallet");

	/** The wallet being sent from, shown as a card carrying its balance. */
	private static final By FROM_WALLET = MobileLocators.labelled("Available Balance");

	/** First input on the form: the destination wallet's UUID. */
	private static final By UUID_FIELD = MobileLocators.input(0);

	/** Second input: the remark, which counts down from 150. */
	private static final By REMARK_FIELD = MobileLocators.input(1);

	private static final By REMARK_COUNTER = MobileLocators.labelled("characters remaining");

	public static final String FEE_PAID_BY_ME = "Fee will be paid by me";
	public static final String FEE_PAID_BY_OTHER = "Fee will be paid by other party";

	/** Exact: the heading "Transfer Amount" contains the button's whole label. */
	private static final By TRANSFER_ACTION = MobileLocators.exactTappable("Transfer");

	public WalletTransferPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(TITLE, Duration.ofSeconds(20));

	}

	/** A heading or block on the form, whether or not it can be tapped. */
	public boolean hasSection(String label) {

		return isPresent(MobileLocators.labelled(label), Duration.ofSeconds(15));

	}

	/** Whether the source wallet is shown with a balance on it. */
	public boolean showsSourceWalletBalance() {

		return isPresent(FROM_WALLET, Duration.ofSeconds(15));

	}

	public boolean hasDestinationField() {

		return isPresent(UUID_FIELD, Duration.ofSeconds(15));

	}

	public void enterDestinationWallet(String uuid) {

		type(UUID_FIELD, uuid, "the destination wallet UUID");
		hideKeyboard();

	}

	public boolean offersFeePayer(String option) {

		return isPresent(MobileLocators.tappable(option), Duration.ofSeconds(15));

	}

	public void chooseFeePayer(String option) {

		tap(MobileLocators.tappable(option), "the '" + option + "' choice");

	}

	public void enterRemark(String remark) {

		type(REMARK_FIELD, remark, "the remark box");
		hideKeyboard();

	}

	/**
	 * What the counter under the remark box currently says.
	 *
	 * Returned whole rather than as a number, so the scenario asserts on the sentence the
	 * user actually reads - "147 characters remaining" - instead of on a parse of it.
	 */
	public String remarkCounter() {

		return waitFor(REMARK_COUNTER, "the remark character counter").getAttribute("content-desc");

	}

	/**
	 * Whether the form will let the transfer be sent.
	 *
	 * The button is on screen from the moment the form opens but starts with
	 * clickable="false", so asking whether it can be tapped is a real test of the form's
	 * state where asking whether it exists would pass on a completely empty one.
	 */
	public boolean canSendTransfer() {

		return isPresent(TRANSFER_ACTION, Duration.ofSeconds(8));

	}

	/** The action exists, enabled or not. */
	public boolean hasTransferAction() {

		return isPresent(MobileLocators.exactly("Transfer"), Duration.ofSeconds(15));

	}
}
