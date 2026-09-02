package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The deposit, withdraw, transfer and convert forms.
 *
 * One class for all four because they are the same form: a heading, an amount box, some
 * pickers, and a Confirm at the bottom. Splitting them would give four near-identical
 * files whose only difference is the word in the heading.
 *
 * NOTHING HERE PRESSES CONFIRM
 *
 * There is deliberately no method that submits one of these forms. Confirming moves real
 * money on the test environment, and it cannot be undone from a test. The scenarios fill
 * the form and assert it is complete and ready, which is the part that can actually break;
 * the transfer itself is somebody's deliberate decision, not an unattended suite's. This
 * mirrors the safety rule the class this replaced kept as a comment - the difference is
 * that a missing method cannot be ignored by accident, whereas a comment can.
 */
public class MoneyFormPage extends BasePage {

	/**
	 * The amount box.
	 *
	 * First input on every one of these forms. Like the login fields it carries no label,
	 * so position is the only handle the application offers.
	 */
	private static final By AMOUNT_FIELD = MobileLocators.input(0);

	private static final By CONFIRM = MobileLocators.tappable("Confirm");

	public MoneyFormPage(AndroidDriver driver) {
		super(driver);
	}

	/** True once the form named in the heading is on screen. */
	public boolean isShowing(String heading) {

		return isPresent(MobileLocators.labelled(heading), Duration.ofSeconds(20));

	}

	public boolean hasAmountField() {

		return isPresent(AMOUNT_FIELD, Duration.ofSeconds(10));

	}

	public void enterAmount(String amount) {

		type(AMOUNT_FIELD, amount, "the amount field");
		hideKeyboard();

	}

	public String getAmount() {

		return waitFor(AMOUNT_FIELD, "the amount field").getText();

	}

	/**
	 * Whether the form's action button exists at all, enabled or not.
	 *
	 * Each form names its own action rather than sharing one word: Deposit and Withdrawal
	 * say Confirm, Convert says Convert - the same label as its own title. The caller
	 * passes the label from the feature file so the difference is visible to a reader
	 * instead of buried here.
	 */
	public boolean hasAction(String label) {

		return isPresent(MobileLocators.labelled(label), Duration.ofSeconds(15));

	}

	/**
	 * Whether the action is enabled, which is what says the form accepted what was typed.
	 *
	 * The button is present from the moment the form opens but starts disabled -
	 * clickable="false" - and only becomes clickable once the amount passes the form's own
	 * rules, which the screen states as "Limit Min 100.00 HKD". Asking whether it is
	 * clickable is therefore a real test of the form's validation, where asking merely
	 * whether the button exists would pass on an empty form.
	 */
	public boolean isActionEnabled(String label) {

		return isPresent(MobileLocators.tappable(label), Duration.ofSeconds(15));

	}

	/** Picks an option from a currency or account sheet by its label. */
	public void chooseOption(String label) {

		tap(MobileLocators.tappable(label), "the " + label + " option");

	}

	public boolean offersOption(String label) {

		return isPresent(MobileLocators.tappable(label), Duration.ofSeconds(10));

	}

	/**
	 * A heading or block on the form, whether or not it can be tapped.
	 *
	 * Section headings are labels rather than controls, so they do not satisfy the
	 * clickable filter that {@link #offersOption} applies.
	 */
	public boolean hasSection(String heading) {

		return isPresent(MobileLocators.labelled(heading), Duration.ofSeconds(10));

	}
}
