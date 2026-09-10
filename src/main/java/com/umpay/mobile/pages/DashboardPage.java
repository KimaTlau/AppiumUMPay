package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.List;

/**
 * The signed in home screen, and the tiles that lead everywhere else.
 *
 * The tiles are matched on their exact label rather than a substring. Several of them
 * share a word, and "Transfer" as a substring finds "Transfer to Mainland China" first -
 * which opens the domestic hub instead of the transfer form, and the failure then shows up
 * two screens later as a missing field rather than as a mis-tap.
 */
public class DashboardPage extends BasePage {

	public static final String DEPOSIT = "Deposit";

	/**
	 * "Withdrawal", not "Withdraw".
	 *
	 * The tile's label really is the longer word. The class this replaced matched labels
	 * as substrings, so "Withdraw" found it by accident; matching exactly is what makes
	 * the Transfer tiles distinguishable, and it means the label has to be exactly right.
	 */
	public static final String WITHDRAW = "Withdrawal";

	public static final String TRANSFER = "Transfer";
	public static final String CONVERT = "Convert";

	/**
	 * Something only the signed in home screen has.
	 *
	 * Deposit is used as the marker rather than a heading because the dashboard's title
	 * carries the account name and the time it was last updated, so it is different on
	 * every run, while the action tiles are always present and always worded the same.
	 */
	private static final By DEPOSIT_TILE = MobileLocators.tile(DEPOSIT);

	/**
	 * The avatar in the top left, which is the way into the profile panel.
	 *
	 * It carries no label of any kind - descriptionContains("Profile") finds nothing
	 * anywhere in the tree - so like the login inputs it can only be reached by position.
	 * It is the first clickable ImageView on the screen, which is at least stated as a
	 * locator here rather than as a tap at fixed coordinates.
	 *
	 * A Semantics label on the avatar in the Flutter source would replace this.
	 */
	private static final By PROFILE_AVATAR = MobileLocators.firstClickableImage();

	/**
	 * How long to allow for the dashboard after signing in.
	 *
	 * Longer than the usual page timeout on purpose. Signing in is a round trip to the
	 * payments backend followed by the dashboard fetching balances, and on the emulator -
	 * where the app's arm64 code is being translated - the whole sequence runs well past
	 * the twenty seconds that is ample everywhere else.
	 */
	private static final Duration AFTER_LOGIN = Duration.ofSeconds(60);

	public DashboardPage(AndroidDriver driver) {
		super(driver);
	}

	/**
	 * A caption that belongs to the notification shade and to nothing in UMPay, so its
	 * presence means the shade is down over whatever the app is showing.
	 */
	private static final By SHADE = MobileLocators.labelled("Clear all notifications");

	public boolean isShowing(Duration timeout) {

		return isPresent(DEPOSIT_TILE, timeout);

	}

	public boolean isShowing() {

		return isShowing(AFTER_LOGIN);

	}

	public void openTile(String label) {

		tap(MobileLocators.tile(label), "the " + label + " tile on the dashboard");

	}

	/** Whether every tile the dashboard is supposed to offer is actually there. */
	public boolean offersTiles(List<String> labels) {

		for (String label : labels) {
			if (!isPresent(MobileLocators.tile(label), Duration.ofSeconds(10))) {
				System.out.println("The dashboard is missing the " + label + " tile. On screen: "
						+ labelsOnScreen());
				return false;
			}
		}
		return true;

	}

	/**
	 * What the dashboard writes in place of an amount it is not showing.
	 *
	 * Four asterisks rather than the usual mask of one per digit, so a balance and a blocked
	 * amount look the same while hidden and neither says how long it is.
	 */
	private static final String HIDDEN = "****";

	/**
	 * True while the dashboard is masking what the account holds.
	 *
	 * Asked of the dashboard rather than of a general reader of whatever is on screen: this
	 * is a statement about the home screen's own behaviour, and a step that read the labels
	 * for itself would go on quietly answering after a mis-tap had left some other screen in
	 * front of it.
	 */
	public boolean amountsAreHidden() {

		return labelsOnScreen().contains(HIDDEN);

	}

	/** Everything the dashboard says, for a message that has to name what was seen. */
	public String text() {

		return labelsOnScreen();

	}

	/**
	 * Returns to the dashboard from wherever the app currently is.
	 *
	 * Backing out rather than restarting the app: a restart would lose the session and
	 * make the next scenario sign in again, which is slow and tests nothing.
	 */
	public void returnHome() {

		for (int attempt = 0; attempt < 6 && !isShowing(Duration.ofSeconds(2)); attempt++) {
			pressBack();
		}
	}

	/** One Back press, for callers deciding for themselves when to stop. */
	public void pressBackFromAnywhere() {

		pressBack();

	}

	/**
	 * Closes the notification shade if the phone has left it pulled down.
	 *
	 * The shade is a separate window drawn over the app, so everything underneath stays in
	 * the accessibility tree: isShowing finds the Deposit tile and reports the dashboard
	 * perfectly healthy while nothing on it can actually be tapped. The recovery in the
	 * setup step then stops early, taps where the avatar sits, hits the shade instead, and
	 * the run fails several steps later with "Could not find the Log Out entry" and a list
	 * of labels that are all the status bar's - which is what happened here.
	 *
	 * Detected by a caption only the shade has rather than by pressing Back on principle:
	 * a Back press that was not needed would walk the app back a screen, which is the
	 * opposite of getting to a known state.
	 */
	public void closeNotificationShade() {

		/*
		 * Collapsed unconditionally, without first deciding whether the shade is down.
		 *
		 * It used to be gated on the SHADE caption, and that gate was the bug: "Clear all
		 * notifications" is only drawn when the shade holds several notifications, so a
		 * shade carrying one - which is what a chat message arriving mid-run leaves - was
		 * not recognised as a shade at all. Two scenarios failed that way after the check
		 * itself had been fixed, listing "Alerted | Collapse | Expand" and no clear-all.
		 *
		 * Asking the system to collapse a status bar that is already collapsed does
		 * nothing, so there is no reason to guess first. The guess was the only thing that
		 * could be wrong here, so it is gone.
		 */
		collapseStatusBar();

		// Back only if the shade is still there, and only then is it worth saying so.
		for (int attempt = 0; attempt < 3 && isPresent(SHADE, Duration.ofSeconds(1)); attempt++) {
			System.out.println("The notification shade is still covering the screen - closing it");
			pressBack();
		}
	}

	/** True when UMPay is the app in front, rather than the launcher or something else. */
	public boolean isAppInForeground() {

		return appIsInForeground();

	}

	/** Brings UMPay back to the front after a Back press has left it. */
	public void bringAppToFront() {

		relaunchApp();

	}

	/**
	 * Opens the profile panel from the avatar in the top left.
	 *
	 * A gesture tap, not an element click. The class this suite replaced recorded that a
	 * plain click on this avatar does nothing, and the emulator hid that - the click
	 * happened to work there and did not on the Redmi, where the panel simply never opened
	 * and the failure surfaced afterwards as a missing Log Out entry.
	 *
	 * The locator itself carries across devices: the avatar is the first clickable
	 * ImageView on both, at [29,181][161,313] on the emulator and [19,120][108,209] on the
	 * phone. It is the interaction that had to change, not the way it is found.
	 */
	public void openProfile() {

		/*
		 * Checked here and not only at the start of the scenario.
		 *
		 * The shade check used to run once, while getting the app to a known state. That
		 * covers a shade left down by the previous scenario but not one that arrives during
		 * this one - and a chat notification landing between sign-in and this tap is exactly
		 * what happened: the tap hit the shade, and the run failed twenty seconds later
		 * saying the Log Out entry was missing while listing "Reply" and "Mark as read".
		 *
		 * The avatar sits close under the status bar, so this tap is the one most worth
		 * guarding.
		 */
		closeNotificationShade();

		tapAtCentre(PROFILE_AVATAR, "the profile avatar in the top left");

	}
}
