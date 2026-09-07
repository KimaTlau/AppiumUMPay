package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The sign-up form, its captcha, and the emailed verification step.
 *
 * Almost everything here is carried over from the class this suite replaced rather than
 * rewritten, because it encodes findings that took real work to establish and would be
 * expensive to rediscover. Each is noted where it applies.
 *
 * THE FIELDS ARE FOUND BY THEIR HINT
 *
 * The registration inputs have no labels, but they do carry a hint - "Email", "Password",
 * "Phone" - and that is a genuine locator rather than a position. The captcha box is the
 * exception: its hint is only "Enter", so it is matched on exactly that and deliberately
 * never auto-filled with test data.
 *
 * TYPING GOES OVER ADB
 *
 * sendKeys writes the accessibility node rather than the widget for the captcha and the
 * verification code, so the app never sees the characters. Typing through
 * "adb shell input text" reaches the real input method. sendKeys is kept as a fallback for
 * a machine with no adb on the path.
 */
public class RegisterPage extends BasePage {

	/** Every EditText tag in a page source dump, in document order. */
	private static final Pattern EDIT_TEXT_TAG = Pattern.compile(
			"<[^>]*class=\"android\\.widget\\.EditText\"[^>]*>");

	/** The captcha box is the one field whose hint is exactly "Enter". */
	private static final Pattern CAPTCHA_HINT = Pattern.compile("\\shint=\"Enter\"");

	private static final Pattern HINT_ATTRIBUTE = Pattern.compile("\\shint=\"([^\"]*)\"");

	private static final By REGISTER_NOW = MobileLocators.tappable("Register now");
	private static final By TITLE = MobileLocators.exactly("Register");
	private static final By EMAIL_TAB = MobileLocators.tappable("Email");
	private static final By VERIFICATION_TITLE = MobileLocators.labelled("Verification Code");
	private static final By NEXT = MobileLocators.labelled("Next");

	private final String adb;
	private final String udid;
	private final File screenshotDir;

	public RegisterPage(AndroidDriver driver, String adb, String udid, File screenshotDir) {

		super(driver);
		this.adb = adb;
		this.udid = udid;
		this.screenshotDir = screenshotDir;

		if (!screenshotDir.exists()) {
			screenshotDir.mkdirs();
		}
	}

	// ------------------------------------------------------------------
	// Getting to the form
	// ------------------------------------------------------------------

	/**
	 * Opens the sign-up form and waits until it is actually the screen in front of us.
	 *
	 * The wait is the point. Both screens carry an Email tab, so without it the next step
	 * taps the login screen's tab while the app is still navigating, and the step after
	 * that reads the page source mid-transition and finds no inputs at all - which then
	 * surfaces two steps later as a missing captcha image, three screens from the cause.
	 */
	public void openFromLogin() {

		tap(REGISTER_NOW, "the 'Register now' button on the login screen");

		waitFor(TITLE, "the Register screen");

	}

	/**
	 * Switches the sign-up form to the Email tab.
	 *
	 * The form opens on Mobile - it shows a Phone Number label and an input hinting "Input
	 * phone number" - so this has to happen before the fields mean what the test thinks
	 * they mean. Confirmed by waiting for a field that hints at an email address, because
	 * tapping the tab and hoping is what produced a phone-shaped form last time.
	 */
	public void chooseEmailTab() {

		tapIfPresent(EMAIL_TAB, Duration.ofSeconds(10));

		/*
		 * until() throws on timeout rather than returning false, so the check has to be
		 * wrapped for the message below to ever be printed - an earlier version tested the
		 * return value and the branch was unreachable.
		 */
		try {
			new org.openqa.selenium.support.ui.WebDriverWait(driver, DEFAULT_TIMEOUT)
					.until(d -> hintsOnForm().stream()
							.anyMatch(h -> h.toLowerCase().contains("email")));

		} catch (org.openqa.selenium.TimeoutException e) {
			throw new IllegalStateException("The sign-up form never switched to the Email tab."
					+ " Field hints are " + hintsOnForm()
					+ ". Labels on screen: " + labelsOnScreen(), e);
		}
	}

	/** Every input hint on the form, in document order. */
	public List<String> hintsOnForm() {

		Matcher fields = EDIT_TEXT_TAG.matcher(driver.getPageSource());

		List<String> hints = new ArrayList<>();

		while (fields.find()) {
			Matcher hint = HINT_ATTRIBUTE.matcher(fields.group());
			hints.add(hint.find() ? hint.group(1) : "");
		}

		return hints;

	}

	// ------------------------------------------------------------------
	// The form itself
	// ------------------------------------------------------------------

	/**
	 * Fills every field the form offers, choosing the value from the field's own hint.
	 *
	 * Anything unrecognised is left alone rather than guessed at. The captcha box is the
	 * reason: its hint says only "Enter", and typing test data into it would waste the
	 * attempt and hide what went wrong.
	 */
	public void fillFields(String email, String phone, String password, String name) {

		List<String> hints = hintsOnForm();

		System.out.println("Registration form exposes " + hints.size() + " input field(s): " + hints);

		if (hints.isEmpty()) {
			throw new IllegalStateException("The sign-up form is showing no input fields at all."
					+ " Labels on screen: " + labelsOnScreen());
		}

		for (int index = 0; index < hints.size(); index++) {

			String value = valueForHint(hints.get(index), email, phone, password, name);

			if (value == null) {
				System.out.println("Skipping field " + index + " (hint=\"" + hints.get(index)
						+ "\") - not auto-filled.");
				continue;
			}

			try {
				typeIntoField(index, value, "field " + index + " (hint=\"" + hints.get(index) + "\")");

			} catch (Exception e) {
				System.out.println("Could not fill field " + index + " (hint=\""
						+ hints.get(index) + "\"): " + e.getMessage());
			}
		}
	}

	/**
	 * Test data per field, chosen from the field's hint.
	 *
	 * Returns null for anything that must not be auto-filled, and null is also the default
	 * for an unrecognised field - a guess there would be worse than leaving it empty.
	 */
	private String valueForHint(String hint, String email, String phone, String password, String name) {

		String h = hint.toLowerCase();

		if (h.contains("code") || h.contains("otp") || h.contains("captcha") || h.contains("verif")) {
			return null;
		}
		if (h.contains("email")) {
			return email;
		}
		if (h.contains("phone") || h.contains("mobile")) {
			return phone;
		}
		if (h.contains("password")) {
			return password;
		}
		if (h.contains("name")) {
			return name;
		}
		return null;
	}

	/**
	 * Types into a registration field through the device's input method.
	 *
	 * sendKeys is not enough here. It reaches the accessibility node rather than the
	 * widget, and the app then submits with the field empty - the form came back saying
	 * "Password is required!" and "Captcha Code is required" while the run's log showed
	 * both had been typed. adb goes through the real IME, which is the same reason the
	 * captcha and the verification code use it.
	 *
	 * A filled field in this app drops its hint, so the hint disappearing is the proof the
	 * text landed. That works for the password boxes too, where reading the value back
	 * would give nothing useful.
	 */
	private void typeIntoField(int index, String value, String what) {

		for (int attempt = 1; attempt <= 2; attempt++) {

			WebElement field = driver.findElement(MobileLocators.input(index));
			field.click();

			if (!typeOverAdb(value)) {
				System.out.println("adb typing unavailable, falling back to sendKeys for " + what);
				field.sendKeys(value);
			}

			hideKeyboard();

			List<String> hints = hintsOnForm();

			boolean landed = index >= hints.size() || hints.get(index).isEmpty();

			if (landed) {
				return;
			}

			System.out.println("Typing into " + what + " did not take on attempt " + attempt
					+ " - the field still shows its hint");
		}
	}

	// ------------------------------------------------------------------
	// The captcha
	// ------------------------------------------------------------------

	/**
	 * The captcha input, found by being the field whose hint is "Enter".
	 *
	 * Private: a page object handing a WebElement back to a step would let the step do
	 * anything with it, which is the coupling the page is there to prevent.
	 */
	private WebElement captchaField() {

		Matcher fields = EDIT_TEXT_TAG.matcher(driver.getPageSource());

		for (int index = 0; fields.find(); index++) {
			if (CAPTCHA_HINT.matcher(fields.group()).find()) {
				return driver.findElement(MobileLocators.input(index));
			}
		}

		throw new IllegalStateException("No captcha input on the registration form - no text field hints"
				+ " \"Enter\". Labels currently on screen: " + labelsOnScreen());
	}

	/**
	 * The captcha picture and the button that swaps it for another.
	 *
	 * Both are unlabelled ImageViews, so neither can be found by description. They are told
	 * apart by two things confirmed against a page source dump: they sit on the same row as
	 * the captcha input, and the picture is the one the app marks as not clickable while the
	 * refresh control is clickable.
	 *
	 * @param wantClickable false for the picture, true for the refresh button
	 */
	private WebElement captchaImageView(boolean wantClickable) {

		Rectangle field = captchaField().getRect();
		int fieldBottom = field.getY() + field.getHeight();

		for (WebElement image : driver.findElements(
				io.appium.java_client.AppiumBy.androidUIAutomator(
						"new UiSelector().className(\"android.widget.ImageView\")"))) {

			Rectangle bounds = image.getRect();

			boolean onTheSameRow = bounds.getY() < fieldBottom
					&& (bounds.getY() + bounds.getHeight()) > field.getY();

			if (!onTheSameRow) {
				continue;
			}

			if (Boolean.parseBoolean(image.getAttribute("clickable")) == wantClickable) {
				return image;
			}
		}

		throw new IllegalStateException("Could not find the captcha "
				+ (wantClickable ? "refresh button" : "image") + " beside the captcha input");
	}

	/**
	 * Writes the captcha picture to disk for OCR to read.
	 *
	 * There is no image source to decode as there is on the web: the app draws to a canvas,
	 * so the picture exists only as pixels. The device screenshot is cropped to the image's
	 * own bounds, scaling between the two coordinate spaces - a screenshot comes back in
	 * device pixels while element bounds are reported in the driver's window units, and on a
	 * scaled display those differ.
	 */
	public File captureCaptchaImage(String label) throws IOException {

		Rectangle bounds = captchaImageView(false).getRect();

		BufferedImage screen = ImageIO.read(
				new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)));

		Dimension window = driver.manage().window().getSize();
		double scale = (double) screen.getWidth() / window.getWidth();

		int x = (int) Math.round(bounds.getX() * scale);
		int y = (int) Math.round(bounds.getY() * scale);
		int width = (int) Math.round(bounds.getWidth() * scale);
		int height = (int) Math.round(bounds.getHeight() * scale);

		// Keep the crop inside the screenshot; a rounded edge would throw otherwise.
		x = Math.max(0, Math.min(x, screen.getWidth() - 1));
		y = Math.max(0, Math.min(y, screen.getHeight() - 1));
		width = Math.min(width, screen.getWidth() - x);
		height = Math.min(height, screen.getHeight() - y);

		File target = new File(screenshotDir, label + "_captcha.png");
		ImageIO.write(screen.getSubimage(x, y, width, height), "png", target);

		System.out.println("Captcha image saved to " + target.getPath()
				+ " (" + width + "x" + height + ", screen " + screen.getWidth()
				+ "px / window " + window.getWidth() + ")");

		return target;
	}

	/** Asks the app for a different captcha. */
	public void refreshCaptcha() {

		captchaImageView(true).click();

	}

	/**
	 * Types the captcha reading, and checks it landed.
	 *
	 * Goes through the same verified path as the rest of the form. An earlier version typed
	 * once and hoped, and the app came back with "Captcha Code is required" after six clean
	 * readings had been submitted - the OCR was right every time and the characters simply
	 * never reached the widget, which looks exactly like a captcha the app rejected.
	 */
	/**
	 * Types the captcha reading into a box that has to be focused by hand first.
	 *
	 * THE CAPTCHA PICTURE IS DRAWN ON TOP OF ITS OWN INPUT
	 *
	 * The input reports bounds spanning the whole row - x 77..1003 on this layout - while
	 * the picture sits over x 436..922 of it. An element click goes to the element's
	 * centre, (540,1404), which is inside the picture. So every tap hit the image, focus
	 * stayed on Confirm Password, and nothing typed afterwards reached the widget.
	 *
	 * That single cause produced three different-looking symptoms: sendKeys appearing to
	 * work while the form said the field was empty, adb typing going nowhere, and six clean
	 * OCR readings all coming back rejected as though the answers were wrong.
	 *
	 * Tapping between the left edge of the input and the left edge of the picture lands on
	 * bare field. The gap is computed from the two elements rather than hard-coded, so a
	 * different screen size still finds it.
	 */
	public void enterCaptcha(String reading) {

		if (!focusCaptchaField()) {
			throw new IllegalStateException("Could not put the caret in the captcha box."
					+ " Labels on screen: " + labelsOnScreen());
		}

		clearByBackspace();

		if (!typeOverAdb(reading)) {
			System.out.println("adb typing unavailable, falling back to sendKeys for the captcha.");
			captchaField().sendKeys(reading);
		}

		hideKeyboard();

	}

	/** Whether the caret is actually sitting in the captcha box. */
	private boolean captchaIsFocused() {

		Matcher fields = EDIT_TEXT_TAG.matcher(driver.getPageSource());

		while (fields.find()) {
			if (CAPTCHA_HINT.matcher(fields.group()).find()) {
				return fields.group().contains("focused=\"true\"");
			}
		}

		return false;
	}

	/** Puts the caret in the captcha box by tapping the part of it that is not covered up. */
	private boolean focusCaptchaField() {

		Rectangle field = captchaField().getRect();
		Rectangle picture = captchaImageView(false).getRect();

		int x = (field.getX() + picture.getX()) / 2;
		int y = field.getY() + (field.getHeight() / 2);

		for (int attempt = 1; attempt <= 2; attempt++) {

			tapAt(x, y);

			if (captchaIsFocused()) {
				System.out.println("Captcha field focused by tapping (" + x + "," + y + ")");
				return true;
			}

			System.out.println("Tap at (" + x + "," + y + ") did not focus the captcha field"
					+ " (attempt " + attempt + " of 2)");
		}

		return false;
	}

	/**
	 * Empties the focused input the way a person would, with the keyboard.
	 *
	 * WebElement.clear() is avoided on purpose. Clearing the captcha that way left the app
	 * rejecting it as "Captcha Code is required" while reading the node back showed the
	 * digits sitting in it - the signature of Flutter's text controller losing sync with the
	 * accessibility node that clear() writes through. The automation sees the text; the
	 * widget that validates the form does not.
	 *
	 * The cursor goes to the end first, because a tap lands it wherever it was touched and
	 * backspace only deletes what is behind it.
	 */
	private void clearByBackspace() {

		driver.pressKey(new KeyEvent(AndroidKey.MOVE_END));

		for (int i = 0; i < 12; i++) {
			driver.pressKey(new KeyEvent(AndroidKey.DEL));
		}
	}

	/** Where the captcha box sits among the inputs, or -1 if it is not on screen. */
	private int captchaFieldIndex() {

		Matcher fields = EDIT_TEXT_TAG.matcher(driver.getPageSource());

		for (int index = 0; fields.find(); index++) {
			if (CAPTCHA_HINT.matcher(fields.group()).find()) {
				return index;
			}
		}

		return -1;

	}

	/**
	 * The app's own reason for refusing the sign-up, if it is showing one.
	 *
	 * Some refusals are worth retrying and some are not. A wrong captcha is; "User already
	 * exists" never will be, however many fresh images are read - and the loop that kept
	 * trying reported it as a captcha that could not be solved, which sent the reader
	 * looking at OCR output that had been correct all along.
	 */
	public String rejectionMessage() {

		for (String message : new String[]{"User already exists", "Email already", "already registered"}) {

			if (isPresent(MobileLocators.labelled(message), java.time.Duration.ofSeconds(2))) {
				return message;
			}
		}

		return null;

	}

	/** Dismisses whatever dialog the app put up, so the form is reachable again. */
	public void dismissDialog() {

		if (!tapIfPresent(MobileLocators.tappable("OK"), java.time.Duration.ofSeconds(3))) {
			tapIfPresent(MobileLocators.tappable("Dismiss"), java.time.Duration.ofSeconds(3));
		}
	}

	public void submit(String submitLabel) {

		tap(MobileLocators.tappable(submitLabel), "the " + submitLabel + " button on the sign-up form");

	}

	// ------------------------------------------------------------------
	// The emailed code
	// ------------------------------------------------------------------

	/** Stops the app once the scenario is done with it. */
	public void close() {

		closeApp();

	}

	public boolean isAskingForVerificationCode(Duration timeout) {

		return isPresent(VERIFICATION_TITLE, timeout);

	}

	/**
	 * The verification screen, proven by more than its heading.
	 *
	 * The title alone would also be satisfied by a screen that had rendered its heading and
	 * nothing else, which is the state the sign-up form was caught in earlier - present,
	 * named, and with no inputs on it. Requiring a box to type the code into means the app
	 * really is asking for one.
	 */
	public boolean isOnVerificationScreen(Duration timeout) {

		return isPresent(VERIFICATION_TITLE, timeout)
				&& isPresent(MobileLocators.anyInput(), Duration.ofSeconds(10));

	}

	/**
	 * Types the emailed code and moves on.
	 *
	 * The field is already focused when the screen opens and nothing is drawn over it, so a
	 * plain tap is enough - unlike the captcha. Next is rendered to look disabled until the
	 * box holds a full code, so it is tapped as a gesture rather than as a clickable
	 * element, the same reason the profile avatar is.
	 */
	public void enterVerificationCode(String code) {

		WebElement field = waitFor(MobileLocators.input(0), "the verification code box");
		field.click();

		if (!typeOverAdb(code)) {
			System.out.println("adb typing unavailable, falling back to sendKeys.");
			field.sendKeys(code);
		}

		hideKeyboard();

		tapAtCentre(NEXT, "the Next button on the verification screen");

	}

	// ------------------------------------------------------------------
	// Typing that the app actually receives
	// ------------------------------------------------------------------

	/**
	 * Types through the device's input method rather than the accessibility layer.
	 *
	 * sendKeys writes the accessibility node, which the captcha and verification widgets do
	 * not read from, so the characters never reach the app. adb goes through the real IME.
	 *
	 * @return whether the text was delivered
	 */
	private boolean typeOverAdb(String text) {

		List<String> command = new ArrayList<>();
		command.add(adb);
		command.add("-s");
		command.add(udid);
		command.add("shell");
		command.add("input");
		command.add("text");
		command.add(text);

		try {
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

			if (!process.waitFor(20, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				System.out.println("adb input text did not finish in 20 seconds");
				return false;
			}

			if (process.exitValue() != 0) {
				System.out.println("adb input text failed with exit code " + process.exitValue());
				return false;
			}

			// The IME delivers the characters asynchronously, so give it a moment to land.
			Thread.sleep(600);
			return true;

		} catch (IOException cannotStart) {
			System.out.println("Could not run adb: " + cannotStart.getMessage());
			return false;

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
