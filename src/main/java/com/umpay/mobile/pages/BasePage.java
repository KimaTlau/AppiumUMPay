package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * What every screen in the application needs: waiting, tapping, typing and reading back.
 *
 * The waits are explicit throughout and there are no sleeps. The class this replaced leaned
 * on fixed pauses - sleep(2500) after a tap, sleep(5000) after a login - which is slow when
 * the app is quick and flaky when it is not. Waiting for the thing that is supposed to
 * happen is both faster and honest about what the test is actually expecting.
 */
public abstract class BasePage {

	protected final AndroidDriver driver;
	protected final WebDriverWait wait;

	/** Long enough for a screen transition on a cold emulator, short enough to fail usefully. */
	protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);

	/** The application under test. Kept here so both the pages and the driver factory agree. */
	public static final String APP_PACKAGE = "com.umpay.me";

	protected BasePage(AndroidDriver driver) {

		this.driver = driver;
		this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);

	}

	protected WebElement waitFor(By locator, String what) {

		try {
			return wait.until(ExpectedConditions.presenceOfElementLocated(locator));

		} catch (TimeoutException e) {
			throw new TimeoutException("Could not find " + what + " within " + DEFAULT_TIMEOUT.getSeconds()
					+ "s. Labels on screen: " + labelsOnScreen(), e);
		}
	}

	protected void tap(By locator, String what) {

		try {
			wait.until(ExpectedConditions.elementToBeClickable(locator)).click();

		} catch (TimeoutException e) {
			throw new TimeoutException("Could not tap " + what + " within " + DEFAULT_TIMEOUT.getSeconds()
					+ "s. Labels on screen: " + labelsOnScreen(), e);
		}
	}

	/**
	 * Taps the middle of an element as a gesture, rather than clicking it as a control.
	 *
	 * Used where a plain element click is accepted by the driver and then quietly does
	 * nothing, which the class this suite replaced recorded for the Next button on the
	 * verification screen and for the profile avatar.
	 *
	 * Be careful what this is blamed for. Signing out looked like the same problem - Log
	 * Out found, clicked without error, no confirmation - and it was not: that element is
	 * perfectly clickable, and the real cause was where it sat on screen. See the locator
	 * taking overload below.
	 */
	protected void tapAtCentre(WebElement element, String what) {

		Rectangle bounds = element.getRect();

		int x = bounds.getX() + (bounds.getWidth() / 2);
		int y = bounds.getY() + (bounds.getHeight() / 2);

		// Through tapAt so this gets the adb fallback too. A device that refuses one gesture
		// refuses all of them, and having only some of them fall back would be worse than
		// having none.
		tapAt(x, y);

		System.out.println("Tapped " + what + " at " + x + "," + y);

	}

	/**
	 * Finds something and taps it as a gesture, nudging the list first if it is sitting in
	 * the system's gesture strip along the bottom of the screen.
	 *
	 * scrollIntoView stops as soon as an element is technically visible, which for the last
	 * entry in a list means barely - Log Out came back forty four pixels tall with its
	 * centre at y=2330 on a 2400 tall screen, inside the strip Android reserves for its own
	 * gestures. Every tap there is swallowed by the system: not the element click, not the
	 * pointer gesture, not even adb shell input tap, which leaves the screen byte for byte
	 * identical afterwards.
	 *
	 * One short swipe moves the element clear - the same Log Out then measured a hundred
	 * and forty eight pixels tall with its centre at 2202 - and the tap lands.
	 */
	protected void tapAtCentre(By locator, String what) {

		WebElement element = waitFor(locator, what);

		int screenHeight = driver.manage().window().getSize().getHeight();
		int guard = (int) (screenHeight * 0.94);

		Rectangle bounds = element.getRect();

		if (bounds.getY() + (bounds.getHeight() / 2) > guard) {

			System.out.println(what + " is in the bottom gesture strip; scrolling it clear first");

			nudgeUp();

			element = waitFor(locator, what);
		}

		tapAtCentre(element, what);

	}

	/**
	 * Taps a bare coordinate.
	 *
	 * For the rare case where the element's own centre is the wrong place to touch - the
	 * captcha input is one, with its picture drawn over the right-hand half of it.
	 */
	protected void tapAt(int x, int y) {

		PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
		// The second argument pads the sequence with an opening tick. The class this
		// suite replaced used 1, and that is not decoration: with 0 the Redmi rejects the
		// whole chain as "Unable to perform W3C actions" while the emulator accepts either.
		Sequence tap = new Sequence(finger, 1);

		tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
		tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
		tap.addAction(new Pause(finger, Duration.ofMillis(120)));
		tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

		try {
			driver.perform(Collections.singletonList(tap));

		} catch (InvalidElementStateException gesturesRefused) {
			adbInput("tap", String.valueOf(x), String.valueOf(y));
		}
	}

	/**
	 * Runs an input command through adb, for a device that will not let Appium inject one.
	 *
	 * Some phones refuse W3C pointer actions outright. The Redmi this suite targets is one:
	 * a bare tap in the middle of the screen, with none of this class involved, comes back
	 * as "Unable to perform W3C actions", while the identical tap sent through adb works and
	 * opens the screen it was aimed at. On Xiaomi that is the "USB debugging (Security
	 * settings)" developer option - separate from ordinary USB debugging, which is on - and
	 * it governs whether anything may simulate input.
	 *
	 * Turning that option on is the better fix and restores full gesture support. This
	 * fallback exists so the suite does not stop dead on a phone where it is off, and it
	 * says so on every use rather than quietly hiding the difference.
	 */
	private void adbInput(String... arguments) {

		System.out.println("This device refuses Appium gestures; sending the " + arguments[0]
				+ " through adb instead");

		java.util.List<String> words = new java.util.ArrayList<>(List.of("input"));
		words.addAll(List.of(arguments));

		adbShell(words, "This device refuses Appium gestures and adb is not available to fall"
				+ " back on. Enable 'USB debugging (Security settings)' in the phone's developer"
				+ " options, or put adb on the path.");
	}

	/**
	 * Pulls the notification shade back up, whatever is in it.
	 *
	 * Asked of the system rather than mimed with a Back press. Back happens to close the
	 * shade on this phone, but it is a guess about what has focus: if the shade is not
	 * actually down, the same press walks the app back a screen, which is the opposite of
	 * getting to a known state. {@code cmd statusbar collapse} says only the one thing and
	 * is harmless when there is nothing to collapse.
	 *
	 * Failures here are swallowed on purpose. A phone whose shell refuses the command is
	 * one where the caller's own check will still notice the shade and fall back; a
	 * housekeeping step is not a reason to end a scenario.
	 */
	protected void collapseStatusBar() {

		try {
			adbShell(List.of("cmd", "statusbar", "collapse"), null);

		} catch (RuntimeException theShellRefused) {
			System.out.println("Could not ask the system to close the notification shade: "
					+ theShellRefused.getMessage());
		}
	}

	/**
	 * Runs one adb shell command against the device this run is driving.
	 *
	 * The {@code -s} is not optional in principle even though one device is attached today:
	 * adb picks for itself when more than one is, and a run that silently drove the wrong
	 * phone would be worse than one that stopped.
	 *
	 * @param whenAdbIsMissing what to tell the caller if adb cannot be started at all, or
	 *                         null to let that surface as an ordinary IllegalStateException
	 */
	private void adbShell(java.util.List<String> words, String whenAdbIsMissing) {

		String adb = System.getProperty("umpay.adb", "adb");
		String udid = System.getProperty("umpay.udid", "");

		java.util.List<String> command = new java.util.ArrayList<>(List.of(adb));

		if (!udid.isBlank()) {
			command.add("-s");
			command.add(udid);
		}

		command.add("shell");
		command.addAll(words);

		try {
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

			if (!process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new IllegalStateException("adb " + words.get(0)
						+ " did not finish in 20 seconds");
			}

			// The event is delivered asynchronously, so give the app a moment to react.
			Thread.sleep(600);

		} catch (java.io.IOException cannotStart) {
			throw new IllegalStateException(whenAdbIsMissing != null ? whenAdbIsMissing
					: "adb could not be started to run " + words, cannotStart);

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** A short upward swipe, to move a list along without hunting for a scroll target. */
	protected void nudgeUp() {

		Dimension screen = driver.manage().window().getSize();

		int x = screen.getWidth() / 2;
		int from = (int) (screen.getHeight() * 0.75);
		int to = (int) (screen.getHeight() * 0.55);

		PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
		Sequence swipe = new Sequence(finger, 1);

		swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, from));
		swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
		swipe.addAction(finger.createPointerMove(Duration.ofMillis(400),
				PointerInput.Origin.viewport(), x, to));
		swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

		driver.perform(Collections.singletonList(swipe));

	}

	/** Taps only if the element is already there. For prompts that appear conditionally. */
	protected boolean tapIfPresent(By locator, Duration timeout) {

		try {
			new WebDriverWait(driver, timeout)
					.until(ExpectedConditions.elementToBeClickable(locator)).click();
			return true;

		} catch (TimeoutException e) {
			return false;
		}
	}

	protected boolean isPresent(By locator, Duration timeout) {

		try {
			new WebDriverWait(driver, timeout)
					.until(ExpectedConditions.presenceOfElementLocated(locator));
			return true;

		} catch (TimeoutException e) {
			return false;
		}
	}

	/**
	 * Types into a field and checks the text actually landed.
	 *
	 * sendKeys into a Flutter input does not always take: the field can accept the tap,
	 * take focus and still end up empty, which then surfaces one assertion later as a form
	 * that would not validate rather than as a typing failure. Reading the value back is
	 * possible here - unlike the labels, an input does publish its text - so it is worth
	 * checking rather than assuming.
	 *
	 * A field that will not accept text after two attempts is left as it is; whatever the
	 * scenario asserts next will report it, with a message about the form rather than
	 * about the keyboard.
	 */
	protected void type(By locator, String text, String what) {

		for (int attempt = 1; attempt <= 2; attempt++) {

			WebElement field = waitFor(locator, what);
			field.click();
			field.clear();
			field.sendKeys(text);

			String actual = field.getText();

			if (actual != null && actual.contains(text)) {
				return;
			}

			System.out.println("Typing into " + what + " did not take on attempt " + attempt
					+ " (field reads \"" + actual + "\"), trying again");
		}
	}

	/**
	 * Closes the soft keyboard.
	 *
	 * Worth doing before tapping anything low on the screen: the keyboard covers the
	 * bottom third, and a tap aimed at a button underneath it lands on a key instead.
	 */
	protected void hideKeyboard() {

		try {
			driver.executeScript("mobile: hideKeyboard");

		} catch (Exception ignored) {
			// No keyboard showing, which is the state we wanted anyway.
		}
	}

	/**
	 * Brings the app back to the front, whatever is in front of it.
	 *
	 * Backing out of a screen can leave the phone on its launcher, and no number of further
	 * Back presses will return to an app that is no longer running - a setup step that kept
	 * pressing ended up on the MIUI home screen reporting that the login screen had not
	 * appeared. Relaunching is the only way back from there.
	 */
	protected void relaunchApp() {

		driver.activateApp(APP_PACKAGE);

	}

	/**
	 * Stops the app, leaving nothing on screen for the next run to dig out of.
	 *
	 * Worth doing after a scenario that ends deep inside a flow. Registration finishes on
	 * the Verification Code screen, and with the session set not to reset, the app stays
	 * exactly there - so the next run opens on a screen its setup step has to recognise and
	 * escape. Closing it means the next launch starts from the top.
	 *
	 * This stops the app, not the session; the After hook still closes that.
	 */
	protected void closeApp() {

		try {
			driver.terminateApp(APP_PACKAGE);
			System.out.println("Closed " + APP_PACKAGE);

		} catch (Exception e) {
			System.out.println("Could not close the app: " + e.getMessage());
		}
	}

	/** Whether the app under test is the one currently in front. */
	protected boolean appIsInForeground() {

		try {
			return APP_PACKAGE.equals(driver.getCurrentPackage());

		} catch (Exception e) {
			return false;
		}
	}

	protected void pressBack() {

		driver.pressKey(new KeyEvent(AndroidKey.BACK));

	}

	/**
	 * Every accessibility label currently on screen.
	 *
	 * Included in the message of every failure above. On a Flutter app a bare "element not
	 * found" is close to useless - there are no ids to grep the source for - whereas the
	 * list of labels usually shows immediately whether the app was on the wrong screen,
	 * still loading, or showing an error nobody expected.
	 */
	protected String labelsOnScreen() {

		try {
			List<WebElement> labelled = driver.findElements(MobileLocators.labelled(""));

			return labelled.stream()
					.map(e -> e.getAttribute("content-desc"))
					.filter(d -> d != null && !d.isBlank())
					.map(d -> d.replace('\n', ' '))
					.distinct()
					.limit(25)
					.reduce((a, b) -> a + " | " + b)
					.orElse("(none)");

		} catch (Exception e) {
			return "(could not be read: " + e.getMessage() + ")";
		}
	}
}
