package com.umpay.mobile.pages;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * How elements are addressed in this application, and why.
 *
 * UMPay's Android app is built in Flutter, and Flutter publishes almost nothing that a
 * normal Android locator can use. A dump of the live login screen shows the whole of it:
 *
 *   View       clickable=true   content-desc='Email\nTab 2 of 2'
 *   View       clickable=true   content-desc='Login'
 *   EditText   clickable=true   content-desc=''   password=false
 *   EditText   clickable=true   content-desc=''   password=true
 *
 * Three consequences follow, and they shape every locator in this package.
 *
 * THERE ARE NO RESOURCE IDS. Not one element in the application carries a resource-id -
 * the only id anywhere in the tree is android:id/content, which belongs to the platform.
 * By.id is therefore never an option here, however much one would prefer it.
 *
 * TEXT IS ALWAYS EMPTY; THE LABEL LIVES IN content-desc. Flutter renders its own text, so
 * the accessibility label is the only place a caption appears. Everything visible and
 * labelled is found through {@link #labelled} or {@link #tappable}.
 *
 * THE TEXT INPUTS CARRY NO LABEL AT ALL. They have no id, no content-desc and no text, and
 * the one attribute that distinguishes them - password="true" - cannot be selected on:
 * Appium's UiSelector parser rejects both password(true) and password(false), which was
 * confirmed against the running app rather than assumed. Their position among the EditText
 * nodes is genuinely the only handle the application offers, which is why {@link #input}
 * exists and why it is the one positional locator here.
 *
 * That last one is a limitation of the application, not of the tests. Adding
 * Semantics(identifier: 'login_email') around the fields in the Flutter source would make
 * them addressable by name and let {@link #input} be deleted.
 */
public final class MobileLocators {

	private MobileLocators() {
	}

	/** Any element whose accessibility label contains the given text. */
	public static By labelled(String label) {

		return AppiumBy.androidUIAutomator(
				"new UiSelector().descriptionContains(\"" + escape(label) + "\")");

	}

	/**
	 * A tappable element carrying the label.
	 *
	 * The clickable filter is what separates a heading from the control beneath it: the
	 * login screen has both a "Login" title and a "Login" button, identical but for this.
	 */
	public static By tappable(String label) {

		return AppiumBy.androidUIAutomator(
				"new UiSelector().descriptionContains(\"" + escape(label) + "\").clickable(true)");

	}

	/**
	 * An element whose label is exactly this, tappable or not.
	 *
	 * For headings, which are not clickable and so cannot use {@link #tile}, and where a
	 * substring match would be wrong. The sign-up screen is the example: its title is
	 * "Register", and descriptionContains("Register") also matches the "Register now"
	 * button back on the login screen - so a wait for the title was satisfied without ever
	 * leaving login, and every step after it worked on the wrong screen.
	 */
	public static By exactly(String label) {

		return AppiumBy.androidUIAutomator(
				"new UiSelector().description(\"" + escape(label) + "\")");

	}

	/**
	 * A tappable element whose label is exactly this, for the dashboard tiles.
	 *
	 * Several tiles share a word, and a "Transfer" substring match finds "Transfer to
	 * Mainland China" first - landing on the domestic hub instead of the transfer form.
	 */
	public static By tile(String label) {

		return exactTappable(label);

	}

	/**
	 * A tappable element whose label is exactly this.
	 *
	 * Needed wherever a label is a substring of another one on the same screen. The
	 * UnionPay transfer form is the clearest case: its action button is described as
	 * "Transfer" and its amount heading as "Please Input Transfer Amount", so a contains
	 * match finds the heading - which is not clickable, and reports the button as disabled
	 * on a form that is perfectly ready to send.
	 */
	public static By exactTappable(String label) {

		return AppiumBy.androidUIAutomator(
				"new UiSelector().description(\"" + escape(label) + "\").clickable(true)");

	}

	/**
	 * The nth text input on screen, counting from zero.
	 *
	 * The only positional locator in this package, and it is here under protest - see the
	 * class comment. Every screen that uses it names what it expects at that position, so
	 * a layout change fails somewhere legible rather than silently typing into the wrong
	 * box.
	 */
	public static By input(int instance) {

		return AppiumBy.androidUIAutomator(
				"new UiSelector().className(\"android.widget.EditText\").instance(" + instance + ")");

	}

	/**
	 * An element further down a scrolling list, scrolled to as part of finding it.
	 *
	 * UiScrollable does the scrolling inside the locator, so the caller neither swipes nor
	 * knows how far down the thing is. That matters for the profile panel, where Log Out
	 * sits below the fold: the alternative is a loop of fixed swipes, which is a guess
	 * about list length dressed up as a step.
	 *
	 * Only for lists. On a screen with nothing scrollable this finds nothing rather than
	 * falling back to a plain search, so it is not a drop-in for {@link #labelled}.
	 */
	public static By scrollTo(String label) {

		return AppiumBy.androidUIAutomator(
				"new UiScrollable(new UiSelector().scrollable(true))"
						+ ".scrollIntoView(new UiSelector().descriptionContains(\""
						+ escape(label) + "\"))");

	}

	/**
	 * The first tappable image on screen.
	 *
	 * For the profile avatar, which carries no label at all - searching the whole tree for
	 * anything described as "Profile" finds nothing. The second positional locator in this
	 * package, and here for the same reason as {@link #input}: the application offers no
	 * alternative.
	 */
	public static By firstClickableImage() {

		return AppiumBy.androidUIAutomator(
				"new UiSelector().className(\"android.widget.ImageView\").clickable(true).instance(0)");

	}

	/** Every text input on screen, for the screens that count them. */
	public static By anyInput() {

		return AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")");

	}

	/** A quotation mark inside a UiSelector string would end the selector early. */
	private static String escape(String label) {

		return label.replace("\\", "\\\\").replace("\"", "\\\"");

	}
}
