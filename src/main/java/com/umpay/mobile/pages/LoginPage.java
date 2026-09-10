package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The sign in screen.
 *
 * Every locator below was taken from a dump of the running screen rather than guessed:
 *
 *   View       clickable  desc='Login'                 the title
 *   View       clickable  desc='Mobile\nTab 1 of 2'    the phone tab
 *   View       clickable  desc='Email\nTab 2 of 2'     the email tab
 *   EditText   clickable  desc=''  password=false      the address or number
 *   EditText   clickable  desc=''  password=true       the password
 *   View       clickable  desc='Login'                 the button
 *   Button     clickable  desc='Register now'
 *
 * Note the two elements labelled "Login": the title and the button differ only by being
 * clickable, which is why the button is found with tappable rather than labelled.
 *
 * The two inputs carry no label of any kind, so they are addressed by their position among
 * the EditText nodes. That is the application's limitation rather than a shortcut here -
 * see {@link MobileLocators} for what would fix it.
 */
public class LoginPage extends BasePage {

	/**
	 * The title, matched exactly rather than as a substring.
	 *
	 * descriptionContains("Login") is true on the Security screen as well, which offers
	 * "Login Password" and "Biometric Login". That made {@link #isShowing} answer yes while
	 * the app was somewhere else entirely: the step that gets the app to a known state
	 * believed it was already on the login screen and returned without signing out, and the
	 * five scenarios after it all died tapping an Email tab that was never on screen.
	 *
	 * The same trap as the Register link two doors down, and the same fix.
	 */
	private static final By TITLE = MobileLocators.exactly("Login");
	private static final By MOBILE_TAB = MobileLocators.tappable("Mobile");
	private static final By EMAIL_TAB = MobileLocators.tappable("Email");
	private static final By LOGIN_BUTTON = MobileLocators.tappable("Login");
	private static final By FORGOT_PASSWORD = MobileLocators.tappable("Forgot Password");
	private static final By REGISTER_NOW = MobileLocators.tappable("Register now");

	/** First input: the email address on the Email tab, the number on the Mobile tab. */
	private static final By IDENTIFIER_FIELD = MobileLocators.input(0);

	/** Second input: the password on both tabs. */
	private static final By PASSWORD_FIELD = MobileLocators.input(1);

	public LoginPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(TITLE, Duration.ofSeconds(10));

	}

	public boolean offersRegistration() {

		return isPresent(REGISTER_NOW, Duration.ofSeconds(5));

	}

	public boolean offersPasswordRecovery() {

		return isPresent(FORGOT_PASSWORD, Duration.ofSeconds(5));

	}

	/**
	 * Switches to the Email tab.
	 *
	 * The form opens on Mobile, and the email field does not exist until this happens -
	 * the tab swap rebuilds the inputs rather than hiding one and showing another.
	 */
	public void openEmailTab() {

		tap(EMAIL_TAB, "the Email tab");

	}

	public void openMobileTab() {

		tap(MOBILE_TAB, "the Mobile tab");

	}

	public void enterCredentials(String identifier, String password) {

		type(IDENTIFIER_FIELD, identifier, "the email or phone field");
		type(PASSWORD_FIELD, password, "the password field");

		// The Login button sits under the keyboard until this happens.
		hideKeyboard();

	}

	public void submit() {

		tap(LOGIN_BUTTON, "the Login button");

	}

	public void loginWithEmail(String email, String password) {

		openEmailTab();
		enterCredentials(email, password);
		submit();

	}

	public void openRegistration() {

		tap(REGISTER_NOW, "the Register now link");

	}
}
