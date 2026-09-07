package com.umpay.mobile.utility;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opens and closes the session against whichever device is being tested.
 *
 * Every capability is overridable from the command line, which is what lets one suite run
 * against the physical phone and the headless emulator without a second copy of anything:
 *
 *   the Redmi (the defaults)
 *     mvn test
 *
 *   the headless emulator
 *     mvn test -Dumpay.udid=emulator-5554 -Dumpay.deviceName=Android_Emulator \
 *              -Dumpay.platformVersion=16
 *
 * platformVersion has to move with the udid rather than stay at the phone's value:
 * UiAutomator2 refuses to start a session when the version it is told does not match the
 * device it finds, and the emulator image is a different Android release from the phone.
 */
public final class DeviceFactory {

	public static final String APP_PACKAGE = "com.umpay.me";
	public static final String APP_ACTIVITY = "com.umpay.me.MainActivity";

	private static final Set<AndroidDriver> OPEN_SESSIONS = ConcurrentHashMap.newKeySet();

	static {
		/*
		 * A last line of defence, not the usual route.
		 *
		 * Scenarios close their own session in the After hook. This is for the runs that
		 * never reach one - a suite stopped with Ctrl+C, a JVM that exits early - because
		 * an abandoned Appium session holds the device until it times out, and the next
		 * run then fails to start with the device already in use.
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			quitAll();
			releaseScreen();
		}, "appium-cleanup"));
	}

	/** Set once per run, so the screen setting is read and written a single time. */
	private static final AtomicBoolean SCREEN_HELD = new AtomicBoolean(false);

	/** The device's own stay-awake setting, put back when the run ends. */
	private static String previousStayAwake;

	private DeviceFactory() {
	}

	public static String deviceName() {
		return System.getProperty("umpay.deviceName", "Redmi 13C");
	}

	public static String udid() {
		return System.getProperty("umpay.udid", "YPYXSKCQXCXWVW55");
	}

	public static String platformVersion() {
		return System.getProperty("umpay.platformVersion", "15");
	}

	public static String appiumServer() {
		return System.getProperty("appium.server", "http://127.0.0.1:4723");
	}

	/** True when the run is pointed at an emulator rather than the physical phone. */
	public static boolean isEmulator() {
		return udid().startsWith("emulator-");
	}

	public static AndroidDriver startApp() {

		UiAutomator2Options options = new UiAutomator2Options()
				.setDeviceName(deviceName())
				.setUdid(udid())
				.setPlatformVersion(platformVersion())
				.setAppPackage(APP_PACKAGE)
				.setAppActivity(APP_ACTIVITY)
				.setAutomationName("UiAutomator2")
				.setNewCommandTimeout(Duration.ofMinutes(5))
				.amend("autoGrantPermissions", true)
				// The app stays installed and signed in between runs. A reset would mean
				// re-registering an account every time, which the app will not allow.
				.setNoReset(true)
				.setFullReset(false);

		keepScreenAwake();

		try {
			AndroidDriver driver = new AndroidDriver(new URL(appiumServer()), options);

			OPEN_SESSIONS.add(driver);

			/*
			 * No implicit wait is set, deliberately.
			 *
			 * An implicit wait and the explicit waits in the page objects multiply rather
			 * than combine: every lookup that finds nothing blocks for the implicit
			 * timeout first, so a twenty second explicit wait gets a handful of polls
			 * instead of forty, and conditions that ask whether something is absent become
			 * enormously slow. The page objects do all the waiting.
			 */

			System.out.println("Session opened against " + deviceName() + " (" + udid() + ")"
					+ (isEmulator() ? " - emulator" : " - physical device"));

			return driver;

		} catch (MalformedURLException e) {
			throw new IllegalStateException("The Appium server address is not a URL: " + appiumServer(), e);
		}
	}

	/**
	 * Ends a session, and never throws while doing it.
	 *
	 * Called from tear down blocks whose job is to run no matter what already went wrong,
	 * so a session that has already died must not become a second failure on top of the
	 * first.
	 */
	public static void quitApp(AndroidDriver driver) {

		if (driver == null) {
			return;
		}

		try {
			driver.quit();

		} catch (Exception e) {
			System.out.println("The session could not be closed cleanly: " + e.getMessage());

		} finally {
			OPEN_SESSIONS.remove(driver);
		}
	}

	/**
	 * Stops the device's display going to sleep while the suite runs.
	 *
	 * Appium's element lookups are not user input, so they do not reset the display's idle
	 * timer. A step that spends a while polling - the setup step recovering from whatever
	 * the last run left behind is the one that does this - can outlast the timeout, and
	 * once the screen is off every lookup sees nothing. That is not a hypothetical: the
	 * phone's timeout is sixty seconds, and a withdrawal scenario failed after 186 seconds
	 * in its setup loop, reporting that the login screen had not appeared while the
	 * screenshot for it was a completely black frame.
	 *
	 * The device's own value is read first and put back by the shutdown hook, so a phone
	 * that is borrowed for a test run does not keep the setting afterwards.
	 */
	private static void keepScreenAwake() {

		if (!SCREEN_HELD.compareAndSet(false, true)) {
			return;
		}

		previousStayAwake = adb("settings", "get", "global", "stay_on_while_plugged_in");

		// 7 is AC, USB and wireless charging together, which covers however it is plugged in.
		adb("settings", "put", "global", "stay_on_while_plugged_in", "7");

		// Wake it now as well: the run may be starting against a screen that is already off.
		adb("input", "keyevent", "KEYCODE_WAKEUP");

		System.out.println("Holding the screen awake for the run (device setting was "
				+ (previousStayAwake.isEmpty() ? "unset" : previousStayAwake) + ")");
	}

	/** Puts the display timeout back the way the device had it. */
	private static void releaseScreen() {

		if (!SCREEN_HELD.get() || previousStayAwake == null) {
			return;
		}

		adb("settings", "put", "global", "stay_on_while_plugged_in",
				previousStayAwake.isEmpty() ? "0" : previousStayAwake);
	}

	/**
	 * One adb shell command against the device under test.
	 *
	 * Returns what it printed, or an empty string if anything went wrong. Nothing here may
	 * throw: it is called from session start up and from the shutdown hook, and neither is
	 * a place where a missing adb should end the run.
	 */
	private static String adb(String... shellCommand) {

		try {
			String[] command = new String[shellCommand.length + 4];
			command[0] = "adb";
			command[1] = "-s";
			command[2] = udid();
			command[3] = "shell";
			System.arraycopy(shellCommand, 0, command, 4, shellCommand.length);

			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()))) {

				String line;

				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}

			process.waitFor();

			return output.toString().trim();

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return "";

		} catch (Exception e) {
			System.out.println("adb " + String.join(" ", shellCommand) + " failed: " + e.getMessage());
			return "";
		}
	}

	public static void quitAll() {

		if (OPEN_SESSIONS.isEmpty()) {
			return;
		}

		System.out.println("Closing " + OPEN_SESSIONS.size() + " Appium session(s) still open at shutdown");

		for (AndroidDriver driver : Set.copyOf(OPEN_SESSIONS)) {
			quitApp(driver);
		}
	}
}
