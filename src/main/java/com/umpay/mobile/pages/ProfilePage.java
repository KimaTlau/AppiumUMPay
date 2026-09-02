package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The Profile Setting panel, and the way out of the app.
 *
 * The panel is a long scrolling list - Trade Record, Referral Code, User List, Commission
 * Listing, Fee Listing, Wallets, Payment, Templates, Transfer Fee Setting, Security,
 * Document verification, Language, Setting - and Log Out is below the fold on every screen
 * size worth testing. It is reached with a scrolling locator rather than a loop of swipes,
 * so nothing here has to guess how long the list is.
 *
 * Two labels here are exact and were taken from the app rather than assumed, having been
 * guessed wrongly once: the entry is "Log Out" (not "Sign Out"), and the confirmation is
 * "Yes, Logout" (not "Confirm" or "Yes"). descriptionContains is case sensitive, so the
 * capitalisation matters as much as the words.
 */
public class ProfilePage extends BasePage {

	private static final By PANEL_TITLE = MobileLocators.labelled("Profile Setting");

	/** Below the fold, so brought into view by scrolling to it. */
	private static final By SCROLL_TO_LOG_OUT = MobileLocators.scrollTo("Log Out");

	/**
	 * The same entry once it is on screen.
	 *
	 * Kept apart from the scrolling locator on purpose. UiScrollable's scrollIntoView goes
	 * back to the top of the list and searches down again every time it is evaluated, so
	 * re-finding through it moves the list under the coordinates that were just measured -
	 * which is how a tap on Log Out landed on nothing and left the run sitting on the
	 * profile panel waiting for a confirmation that was never asked for. Scroll once with
	 * the locator above, then work with this one.
	 */
	private static final By LOG_OUT = MobileLocators.labelled("Log Out");

	private static final By CONFIRM_LOGOUT = MobileLocators.labelled("Yes, Logout");

	public ProfilePage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(PANEL_TITLE, Duration.ofSeconds(15));

	}

	/**
	 * Signs out and answers the confirmation.
	 *
	 * The confirmation is answered here rather than left to the caller: a half finished
	 * sign out leaves the app on a dialog that the next scenario has no way to interpret,
	 * and it surfaces as an unrelated failure one screen later.
	 */
	public void signOut() {

		/*
		 * Both of these are ordinary clickable controls - the problem was never what they
		 * are, it was where Log Out ends up.
		 *
		 * As the last entry in a long list, scrollIntoView leaves it only just on screen and
		 * clipped, with its centre inside the gesture strip Android keeps along the bottom.
		 * Taps there are swallowed by the system: the element click, a pointer gesture and a
		 * raw adb tap all left the screen byte for byte unchanged. tapAtCentre notices the
		 * position and scrolls it clear before tapping.
		 */
		// Scroll it into view once, then leave the scrolling locator alone.
		waitFor(SCROLL_TO_LOG_OUT, "the Log Out entry in Profile Setting");

		tapAtCentre(LOG_OUT, "the Log Out entry in Profile Setting");

		/*
		 * A tap on this entry does not always open the dialog, and the failure is silent:
		 * the panel simply stays as it was. One retry costs a second and turns an
		 * occasional red run into a green one; if the second tap does not raise the dialog
		 * either, the wait below reports it properly rather than being papered over.
		 */
		if (!isPresent(CONFIRM_LOGOUT, Duration.ofSeconds(5))) {

			System.out.println("Log Out did not raise the confirmation; tapping it once more");

			tapAtCentre(LOG_OUT, "the Log Out entry in Profile Setting");
		}

		tapAtCentre(CONFIRM_LOGOUT, "the 'Yes, Logout' confirmation");

	}
}
