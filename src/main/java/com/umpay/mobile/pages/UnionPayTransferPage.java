package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The UnionPay transfer form, shared by the China and Global routes.
 *
 * One class for both because they are the same screen - both titled "To UnionPay", both
 * asking for an amount against the same stated limits. What differs is what each does with
 * it: the China route converts, showing a Receive Currency and quoting a rate into CNY,
 * while Global has no receive currency at all. That difference is asserted from the feature
 * file rather than hidden in here.
 *
 * WHAT THE FORM DOES WITH AN AMOUNT
 *
 * Entering one above the minimum makes an Order Details block appear - original amount,
 * estimated rate, fee, total and approximate received - and the Transfer button becomes
 * clickable at the same moment. That is the behaviour worth testing: the form prices the
 * transfer, and only then offers to send it.
 *
 * NOTHING HERE SENDS THE TRANSFER.
 */
public class UnionPayTransferPage extends BasePage {

	private static final By TITLE = MobileLocators.exactly("To UnionPay");

	/** First input on the form; its hint carries the currency and the minimum. */
	private static final By AMOUNT_FIELD = MobileLocators.input(0);

	/**
	 * Exact, and the reason that locator exists at all.
	 *
	 * "Please Input Transfer Amount" contains "Transfer" and is not clickable, so a
	 * contains match reports a ready form as unable to send.
	 */
	private static final By TRANSFER_ACTION = MobileLocators.exactTappable("Transfer");

	private static final By ORDER_DETAILS = MobileLocators.labelled("Order Details");

	public UnionPayTransferPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(TITLE, Duration.ofSeconds(20));

	}

	/** A label on the form - a limit, a currency heading, an order detail row. */
	public boolean shows(String label) {

		return isPresent(MobileLocators.labelled(label), Duration.ofSeconds(15));

	}

	/**
	 * Whether a label is absent, waiting long enough to be sure of it.
	 *
	 * Used for the Global route's missing Receive Currency. Reading straight away would
	 * pass while the screen was still drawing and prove nothing.
	 */
	public boolean doesNotShow(String label) {

		return !isPresent(MobileLocators.labelled(label), Duration.ofSeconds(8));

	}

	public void enterAmount(String amount) {

		type(AMOUNT_FIELD, amount, "the transfer amount");
		hideKeyboard();

	}

	public String enteredAmount() {

		return waitFor(AMOUNT_FIELD, "the transfer amount").getText();

	}

	/** True once the form has priced what was entered. */
	public boolean showsOrderDetails() {

		return isPresent(ORDER_DETAILS, Duration.ofSeconds(15));

	}

	/** True while the form is still refusing to price the amount. */
	public boolean hidesOrderDetails() {

		return !isPresent(ORDER_DETAILS, Duration.ofSeconds(8));

	}

	public boolean canSendTransfer() {

		hideKeyboard();

		return isPresent(TRANSFER_ACTION, Duration.ofSeconds(8));

	}

	/**
	 * The action exists, enabled or not.
	 *
	 * The keyboard is closed first, and that is not a precaution. The app opens this form
	 * with the amount box already focused, so the numeric keyboard is up before a test has
	 * touched anything, and it covers the bottom of the screen where Transfer sits. Flutter
	 * does not publish a widget that is not on screen, so the action is genuinely absent
	 * from the accessibility tree rather than merely hidden - the question cannot be
	 * answered honestly without closing the keyboard first.
	 *
	 * A scenario that types an amount never saw this, because enterAmount closes the
	 * keyboard on its way out. Only the scenarios that ask about the empty form reach it.
	 */
	public boolean hasTransferAction() {

		hideKeyboard();

		return isPresent(MobileLocators.exactly("Transfer"), Duration.ofSeconds(15));

	}
}
