package org.example;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UMPay Android app (Flutter) mobile QA suite.
 *
 * LOCATOR STRATEGY — this app is built with Flutter, which renders to a single canvas
 * and exposes its widget semantics as accessibility labels. Confirmed against a live
 * page source dump from the device: every node reports text="" and carries its label in
 * content-desc instead. Locating by visible text therefore matches nothing anywhere in
 * the app; everything below locates by content-desc (descriptionContains) or, for input
 * fields, by EditText class and position.
 *
 * Note that Flutter only builds its semantics tree while an accessibility service is
 * running, which is why `adb shell uiautomator dump` returns a null root but Appium's
 * UiAutomator2 session sees the full tree.
 *
 * SELF-DIAGNOSING FAILURES — when a locator is not found, the assertion message includes
 * every content-desc currently on screen, and the full page source is written to
 * page-source/<step>.xml. A failing run therefore tells you the exact label to swap in
 * rather than requiring a separate Inspector session.
 *
 * SAFETY BOUNDARY: every money-moving flow is driven to its final confirmation screen —
 * amounts entered, currency/account/template selected — but the final Confirm / Convert
 * button is NEVER tapped, so no funds move. Do not remove these stops.
 *
 * Registration is the one exception, and it is deliberate: the captcha is now read by
 * OCR and the form IS submitted, so every run creates a real account on the test
 * environment under its own lawma195.infinity+m<timestamp> address. Run it against a
 * test environment only.
 */
@Listeners(ExtentReportListener.class)
public class MobileFlowTest {

    // ---- Device / app capabilities ----
    private static final String DEVICE_NAME = "Redmi 13C";
    private static final String UDID = "YPYXSKCQXCXWVW55";
    private static final String PLATFORM_VERSION = "15";
    private static final String APP_PACKAGE = "com.umpay.me";
    private static final String APP_ACTIVITY = "com.umpay.me.MainActivity";
    private static final String APPIUM_SERVER = System.getProperty("appium.server", "http://127.0.0.1:4723");

    /** Overridable for a machine where adb is not on PATH: -Dumpay.adb=/path/to/adb */
    private static final String ADB = System.getProperty("umpay.adb", "adb");

    /** How long to wait for the sign-up email. Delivery is usually seconds, rarely a minute. */
    private static final int MAIL_TIMEOUT_SECONDS =
            Integer.getInteger("umpay.mail.timeout", 120);

    // ---- Test account ----
    private static final String EMAIL = System.getProperty("umpay.email", "elkaytlau7@gmail.com");
    private static final String PASSWORD = System.getProperty("umpay.password", "12345678");

    // ---- Register test data ----

    /**
     * A fresh address per run. A registered address cannot be reused, and the
     * +timestamp form means every run has its own while all of them are delivered
     * to the one mailbox - the same trick the web suite uses.
     */
    private static final String REGISTER_TEST_EMAIL = System.getProperty(
            "umpay.register.email",
            "lawma195.infinity+m" + new java.text.SimpleDateFormat("ddMMyyyyHHmmss")
                    .format(new java.util.Date()) + "@gmail.com");

    private static final String REGISTER_TEST_PASSWORD =
            System.getProperty("umpay.register.password", "TestQA@2026");

    private static final String SCREENSHOT_DIR = "screenshots";
    private static final String SOURCE_DIR = "page-source";

    private static AndroidDriver driver;
    private static int shotCounter = 1;

    @BeforeClass
    public void setUp() throws Exception {

        new File(SCREENSHOT_DIR).mkdirs();
        new File(SOURCE_DIR).mkdirs();

        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(DEVICE_NAME)
                .setUdid(UDID)
                .setPlatformVersion(PLATFORM_VERSION)
                .setAppPackage(APP_PACKAGE)
                .setAppActivity(APP_ACTIVITY)
                .setAutomationName("UiAutomator2")
                .setNewCommandTimeout(Duration.ofMinutes(5))
                .amend("autoGrantPermissions", true)
                .setNoReset(true)
                .setFullReset(false);

        driver = new AndroidDriver(new URL(APPIUM_SERVER), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ------------------------------------------------------------------
    // Locators — Flutter exposes labels via content-desc, never text
    // ------------------------------------------------------------------

    /** Any element whose accessibility label contains the given string. */
    private By desc(String label) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + label + "\")");
    }

    /** Tappable element with the given label — disambiguates titles from buttons. */
    private By tappable(String label) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + label + "\").clickable(true)");
    }

    /**
     * Exact-label tile, for the dashboard shortcuts. Needed because several tiles share a
     * word: a "Transfer" substring match hits "Transfer to Mainland China" first and lands
     * on the domestic hub instead of the transfer form.
     */
    private By tile(String label) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().description(\"" + label + "\").clickable(true)");
    }

    /** The nth text input on screen (0-based). */
    private By input(int instance) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\").instance(" + instance + ")");
    }

    /**
     * The masked password input. Selected by position rather than the password flag —
     * UiSelector().password(true) is not honoured by Appium's selector parser even though
     * the node reports password="true". On both login tabs the password field is the
     * second input, after the phone number or email address.
     */
    private By passwordInput() {
        return input(1);
    }

    // ------------------------------------------------------------------
    // Captcha
    // ------------------------------------------------------------------

    /** The captcha this app issues is four digits, and the input caps at four. */
    private static final int CAPTCHA_LENGTH = 4;

    /** Fresh images to work through before handing the captcha to a person. */
    private static final int CAPTCHA_ATTEMPTS =
            Integer.getInteger("umpay.ocr.attempts", 10);

    /** Every EditText tag in a page source dump, in document order. */
    private static final Pattern EDIT_TEXT_TAG = Pattern.compile(
            "<[^>]*class=\"android\\.widget\\.EditText\"[^>]*>");

    /**
     * The captcha input's hint, spelled exactly. The password fields hint "Enter Password"
     * and the address field "Enter Email", so this has to match the whole value, not a prefix.
     */
    private static final Pattern CAPTCHA_HINT = Pattern.compile("\\shint=\"Enter\"");

    /**
     * The captcha input, found by the only thing that stays true about it.
     *
     * Every field on this form is a bare EditText with no resource-id, so it has to be
     * picked out by an attribute. max-text-length="4" looks like the obvious choice and
     * was the original one, but it is a trap: the moment a code is typed the app re-renders
     * the field with max-text-length="8". Matching on it therefore worked before typing and
     * silently stopped matching after — which is how a run came back green while the app was
     * still on the form showing "Captcha Code is required". hint="Enter" is present in all
     * four form states, empty or filled, and no other input carries it.
     *
     * The value is read from the page source rather than from the element because
     * UiAutomator2 lists these attributes as supported but still answers getAttribute on
     * these Flutter-backed nodes with UnsupportedCommandException. The fields are counted in
     * the XML and the match is addressed by index — UiSelector's instance() walks the
     * hierarchy in the same document order.
     */
    private WebElement captchaField() {

        Matcher fields = EDIT_TEXT_TAG.matcher(driver.getPageSource());

        for (int index = 0; fields.find(); index++) {
            if (CAPTCHA_HINT.matcher(fields.group()).find()) {
                return driver.findElement(input(index));
            }
        }

        throw new AssertionError("No captcha input on the registration form — no text field"
                + " hints \"Enter\". Labels currently on screen: " + onScreenLabels());
    }

    /**
     * The captcha picture and the button that swaps it for another.
     *
     * Both are unlabelled ImageViews, so neither can be located by description.
     * They are told apart by two things confirmed against a page source dump: they
     * sit on the same row as the captcha input, and the picture is the one the app
     * marks as not clickable while the refresh control is clickable.
     *
     * @param wantClickable false for the picture, true for the refresh button
     */
    private WebElement captchaImageView(boolean wantClickable) {

        Rectangle field = captchaField().getRect();
        int fieldBottom = field.getY() + field.getHeight();

        for (WebElement image : driver.findElements(
                AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.ImageView\")"))) {

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

        throw new AssertionError("Could not find the captcha "
                + (wantClickable ? "refresh button" : "image") + " beside the captcha input");
    }

    /**
     * Writes the captcha picture to disk for OCR to read.
     *
     * There is no image source to decode as there is on the web: the app draws to a
     * canvas, so the picture only exists as pixels. The device screenshot is cropped
     * to the image's own bounds, scaling between the two coordinate spaces because a
     * screenshot comes back in device pixels while element bounds are reported in the
     * driver's window units, and on a scaled display those differ.
     */
    private File captureCaptchaImage(String label) throws IOException {

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

        File target = new File(SCREENSHOT_DIR, label + "_captcha.png");
        ImageIO.write(screen.getSubimage(x, y, width, height), "png", target);

        System.out.println("Captcha image saved to " + target.getPath()
                + " (" + width + "x" + height + ", screen " + screen.getWidth()
                + "px / window " + window.getWidth() + ")");

        return target;
    }

    /** Asks the app for a different captcha and gives it a moment to arrive. */
    private void refreshCaptcha() {

        captchaImageView(true).click();
        sleep(2500);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean isPresent(By locator, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement waitFor(By locator, int seconds, String what) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            dumpSource(what.replaceAll("[^A-Za-z0-9]+", "_"));
            throw new AssertionError("Could not find " + what
                    + ". Labels currently on screen: " + onScreenLabels());
        }
    }

    private void tap(By locator, int seconds, String what) {
        waitFor(locator, seconds, what).click();
    }

    /** Taps only if present — for optional steps that vary by account state. */
    private boolean tapIfPresent(By locator, int seconds) {
        if (isPresent(locator, seconds)) {
            driver.findElement(locator).click();
            return true;
        }
        return false;
    }

    /**
     * Taps a raw screen coordinate. The profile avatar does not respond to a normal
     * element click, so anything reached through it is driven by a real touch gesture.
     */
    private void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(new Pause(finger, Duration.ofMillis(120)));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    private void tapCenter(WebElement element) {
        Rectangle r = element.getRect();
        tapAt(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
    }

    /** Scrolls the current list down by roughly half a screen. */
    private void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        int from = (int) (size.getHeight() * 0.75);
        int to = (int) (size.getHeight() * 0.25);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, from));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(new Pause(finger, Duration.ofMillis(100)));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), x, to));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    /**
     * Taps the first row of a picker list — used for saved templates and payout accounts,
     * whose labels are account-specific and so cannot be hard-coded into the test.
     */
    private boolean tapFirstListItem(int belowY) {

        List<WebElement> rows = driver.findElements(
                AppiumBy.androidUIAutomator("new UiSelector().clickable(true)"));

        for (WebElement row : rows) {
            Rectangle r = row.getRect();
            if (r.getY() > belowY && r.getWidth() > 200 && r.getHeight() > 40 && r.getHeight() < 400) {
                tapCenter(row);
                return true;
            }
        }
        return false;
    }

    /** Every accessibility label visible right now, for diagnosing a missed locator. */
    private String onScreenLabels() {
        Set<String> labels = new LinkedHashSet<>();
        try {
            Matcher m = Pattern.compile("content-desc=\"([^\"]+)\"").matcher(driver.getPageSource());
            while (m.find()) {
                String label = m.group(1).replace("&#10;", " / ").trim();
                if (!label.isEmpty()) {
                    labels.add(label);
                }
            }
        } catch (Exception e) {
            return "<page source unavailable: " + e.getMessage() + ">";
        }
        return labels.isEmpty() ? "<none>" : String.join(" | ", labels);
    }

    private void dumpSource(String label) {
        try {
            Files.write(new File(SOURCE_DIR, label + ".xml").toPath(),
                    driver.getPageSource().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private void shot(String label) {
        try {
            File src = driver.getScreenshotAs(OutputType.FILE);
            String name = String.format("%02d_%s.png", shotCounter++, label);
            File dest = new File(SCREENSHOT_DIR, name);
            // REPLACE_EXISTING because the counter restarts at 01 every run: without it the
            // second run onwards throws FileAlreadyExistsException on any name it reuses, and
            // the catch below turns that into a quiet "Screenshot failed" line. The evidence
            // this suite exists to produce was going missing for exactly that reason.
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved screenshot: " + name);
            ExtentReportListener.attachScreenshot(dest, label);
        } catch (IOException e) {
            System.out.println("Screenshot failed for " + label + ": " + e.getMessage());
        }
    }

    /**
     * Closes the soft keyboard. Pressing Back to dismiss it is unsafe: when the keyboard
     * is not actually open, the same press navigates a screen backwards instead, which is
     * how a flow can end up outside the app entirely.
     */
    private void hideKeyboard() {
        try {
            if (driver.isKeyboardShown()) {
                driver.hideKeyboard();
                sleep(700);
            }
        } catch (Exception ignored) {
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Walks back to the dashboard so each flow starts from a known screen, however the
     * previous one left the app.
     */
    private void returnToHome() {

        for (int i = 0; i < 8; i++) {

            if (isPresent(tappable("Deposit"), 2)) {
                return;
            }

            // Pressing Back past the app's first screen drops out to the Android
            // launcher, which would strand every remaining flow. Bring the app forward
            // again instead of pressing Back into the void.
            String pkg = driver.getCurrentPackage();
            if (!APP_PACKAGE.equals(pkg)) {
                System.out.println("Left the app (now in " + pkg + ") - reactivating.");
                driver.activateApp(APP_PACKAGE);
                sleep(3000);
                continue;
            }

            driver.navigate().back();
            sleep(900);
        }
    }

    /**
     * Brings UMPay to the front before the first test touches it.
     *
     * Creating the session does not reliably leave the app on screen: with noReset
     * on, an app already running in the background can be attached to without being
     * foregrounded, and the suite then reads the Android launcher instead. That is
     * not a subtle failure - every locator misses and the first assertion reports a
     * screen full of app icons - but it costs a whole run to find out.
     */
    private void ensureAppInForeground() {

        for (int attempt = 1; attempt <= 3; attempt++) {

            String current = driver.getCurrentPackage();

            if (APP_PACKAGE.equals(current)) {
                return;
            }

            System.out.println("UMPay is not in the foreground (currently " + current
                    + ") - activating it, attempt " + attempt + " of 3.");

            driver.activateApp(APP_PACKAGE);
            sleep(4000);
        }

        Assert.assertEquals(driver.getCurrentPackage(), APP_PACKAGE,
                "UMPay would not come to the foreground. Labels on screen: " + onScreenLabels());
    }

    /**
     * Clears the "Select Language" dialog that a freshly installed build shows on its
     * very first launch, choosing English.
     *
     * The choice is not cosmetic. Every locator in this suite matches on the English
     * accessibility label — "Deposit", "Login", "Register now", "Log Out" — so a run that
     * lands on the Thai or Chinese build finds nothing and fails on the first assertion
     * with a screen full of labels it cannot read.
     *
     * The dialog only appears once per install, so its absence is the normal case and
     * costs a two second look. Nothing here throws: if the dialog is not on screen there
     * is nothing to do, and if it is on screen but behaves unexpectedly the caller's own
     * assertions report it against a screenshot.
     */
    private void selectLanguageIfAsked() {

        if (!isPresent(desc("Select Language"), 2)) {
            return;
        }

        System.out.println("First launch: the Select Language dialog is up. Choosing English.");
        dumpSource("select_language");
        shot("select_language");

        // The three options and Confirm are all clickable Views carrying only a
        // content-desc; the title above them is the one element that is not clickable.
        if (!tapIfPresent(tappable("English"), 5)) {
            System.out.println("No English option in the language dialog. Labels on screen: "
                    + onScreenLabels());
            return;
        }
        sleep(800);

        // Picking a language only highlights it — the dialog closes on Confirm.
        tapIfPresent(tappable("Confirm"), 5);
        sleep(2500);

        if (isPresent(desc("Select Language"), 2)) {
            System.out.println("The language dialog is still up after confirming; carrying on"
                    + " anyway so the caller's assertion reports what is actually on screen.");
        }

        shot("language_selected");
    }

    /**
     * Backs out of whatever screen the last run left behind, until the login form is up.
     *
     * noReset=true keeps the app's state between runs, which is what makes a fast re-run
     * possible — but it also means a run that died mid-flow hands the next one the screen
     * it died on. A failed registration attempt is the case that actually bit: the suite
     * came back to a half-filled sign-up form and testLogin failed looking for a Login
     * button that was never going to be there.
     *
     * Android back is used rather than the on-screen arrow because the arrow is an
     * unlabelled ImageView whose position differs per screen, while back unwinds any of
     * them. Nothing is asserted here; if the login form never appears, the caller's own
     * wait reports it with the labels that were on screen.
     */
    private void ensureOnLoginScreen() {

        if (isPresent(tappable("Login"), 3)) {
            return;
        }

        for (int attempt = 1; attempt <= 4 && !isPresent(tappable("Login"), 2); attempt++) {

            System.out.println("Not on the login screen (currently: " + onScreenLabels()
                    + ") - pressing back, attempt " + attempt + " of 4.");

            driver.navigate().back();
            sleep(1500);
        }
    }

    /** Names of the home-screen actions, used to confirm we are signed in. */
    private boolean onDashboard(int seconds) {
        return isPresent(desc("Deposit"), seconds) || isPresent(desc("Wallet"), 2);
    }

    // ------------------------------------------------------------------
    // TC-M-01 — Login
    // ------------------------------------------------------------------
    @Test(priority = 1)
    public void testLogin() {

        sleep(2000);
        ensureAppInForeground();
        selectLanguageIfAsked();
        shot("app_launch");

        // Already signed in from a previous run — sign out so this case tests a real login.
        if (onDashboard(3)) {
            System.out.println("App was already signed in; signing out first.");
            signOut();
            sleep(1500);
        }

        ensureOnLoginScreen();

        // The form opens on the Mobile tab, so the email field does not exist until this tap.
        tap(tappable("Email"), 15, "the Email tab on the login screen");
        sleep(1200);
        shot("login_email_tab");

        WebElement email = waitFor(input(0), 10, "the email input");
        email.click();
        email.sendKeys(EMAIL);

        WebElement password = waitFor(passwordInput(), 10, "the password input");
        password.click();
        password.sendKeys(PASSWORD);

        hideKeyboard(); // so the Login button is reachable
        shot("login_filled");

        tap(tappable("Login"), 10, "the Login button");
        sleep(5000);
        shot("home_after_login");

        Assert.assertTrue(onDashboard(20),
                "Expected the dashboard after login. Labels on screen: " + onScreenLabels());
    }

    // ------------------------------------------------------------------
    // TC-M-02 — Deposit (stops before final Confirm)
    // ------------------------------------------------------------------
    @Test(priority = 2, dependsOnMethods = "testLogin")
    public void testDeposit() {

        returnToHome();
        tap(tappable("Deposit"), 15, "the Deposit action on the dashboard");
        sleep(2500);
        dumpSource("deposit_screen");
        shot("deposit_page");

        // The form already defaults to the HKD wallet. Tapping "Select Currency" only
        // opens a picker sheet that then covers the amount field, so leave it alone.
        if (isPresent(input(0), 5)) {
            WebElement amount = driver.findElement(input(0));
            amount.click();
            amount.sendKeys("500");
            hideKeyboard();
            shot("deposit_amount_entered");
        }

        shot("deposit_final_form");
        // SAFETY: do NOT tap Confirm — that would submit a real deposit request.
    }

    // ------------------------------------------------------------------
    // TC-M-03 — Withdraw (stops before final Confirm)
    // ------------------------------------------------------------------
    @Test(priority = 3, dependsOnMethods = "testLogin")
    public void testWithdraw() {

        returnToHome();
        tap(tappable("Withdraw"), 15, "the Withdraw action on the dashboard");
        sleep(2500);
        dumpSource("withdraw_screen");
        shot("withdraw_page");

        if (isPresent(input(0), 5)) {
            WebElement amount = driver.findElement(input(0));
            amount.click();
            amount.sendKeys("500");
            hideKeyboard();
            shot("withdraw_amount_entered");
        }

        // Confirm stays disabled until a payout account is chosen, so pick a saved one.
        if (tapIfPresent(tappable("Select Saved Account"), 4)) {
            sleep(2000);
            shot("withdraw_select_account");
            if (tapFirstListItem(250)) {
                sleep(2000);
                dumpSource("withdraw_form_completed");
            }
        }

        hideKeyboard();
        shot("withdraw_final_form");
        // SAFETY: do NOT tap Confirm — that would submit a real withdrawal request.
    }

    // ------------------------------------------------------------------
    // TC-M-04 — Transfer (stops before final Confirm)
    // ------------------------------------------------------------------
    @Test(priority = 4, dependsOnMethods = "testLogin")
    public void testTransfer() {

        returnToHome();
        tap(tile("Transfer"), 15, "the Transfer tile on the dashboard");
        sleep(2500);
        dumpSource("transfer_screen");
        shot("transfer_hub");

        // The hub lists destinations rather than a form: step into the saved-template
        // route, then pick a template, before an amount field exists.
        if (tapIfPresent(tappable("UMPay to Existing template"), 5)) {
            sleep(2500);
            dumpSource("transfer_template_picker");
            shot("transfer_select_template");

            if (tapFirstListItem(250)) {
                sleep(2500);
                dumpSource("transfer_form");
                shot("transfer_template_loaded");
            }
        }

        if (isPresent(input(0), 5)) {
            WebElement amount = driver.findElement(input(0));
            amount.click();
            amount.sendKeys("150");
            hideKeyboard();
            shot("transfer_amount_entered");
        }

        shot("transfer_final_form");
        // SAFETY: do NOT tap Next/Confirm — that would submit a real transfer.
    }

    // ------------------------------------------------------------------
    // TC-M-05 — Convert (stops before final Convert)
    // ------------------------------------------------------------------
    @Test(priority = 5, dependsOnMethods = "testLogin")
    public void testConvert() {

        returnToHome();
        tap(tappable("Convert"), 15, "the Convert action on the dashboard");
        sleep(2500);
        dumpSource("convert_screen");
        shot("convert_page");

        if (isPresent(input(0), 5)) {
            WebElement amount = driver.findElement(input(0));
            amount.click();
            amount.sendKeys("1000");
            hideKeyboard();
            sleep(1200); // let the app re-price the conversion
            shot("convert_amount_entered");
        }

        shot("convert_final_calculation");
        // SAFETY: do NOT tap Convert — that would submit a real conversion.
    }

    // ------------------------------------------------------------------
    // TC-M-06 — Logout
    // ------------------------------------------------------------------
    @Test(priority = 6, dependsOnMethods = "testLogin")
    public void testLogout() {

        returnToHome();
        signOut();
        shot("logout_success");

        Assert.assertTrue(isPresent(tappable("Login"), 15),
                "Expected the login screen after logout. Labels on screen: " + onScreenLabels());
    }

    // ------------------------------------------------------------------
    // TC-M-07 — Register (captcha read by OCR, form submitted)
    // ------------------------------------------------------------------
    @Test(priority = 7, dependsOnMethods = "testLogout")
    public void testRegister() throws IOException {

        // testLogout leaves the app on the login screen, where Register now lives.
        tap(tappable("Register now"), 15, "the 'Register now' button on the login screen");
        sleep(3000);
        dumpSource("register_screen");
        shot("register_page");

        // The sign-up form is tabbed like the login form, so select Email if offered.
        if (tapIfPresent(tappable("Email"), 3)) {
            sleep(1500);
            dumpSource("register_email_tab");
            shot("register_email_tab");
        }

        fillRegistrationFields();

        hideKeyboard();
        dumpSource("register_filled");
        shot("register_final_form");

        Assert.assertTrue(solveCaptchaAndSubmit(),
                "The captcha was not solved in " + CAPTCHA_ATTEMPTS + " attempts, so the form was"
                        + " never submitted. The images tried are in " + SCREENSHOT_DIR
                        + ". Labels on screen: " + onScreenLabels());

        shot("register_submitted");
        dumpSource("register_submitted");

        // Assert the destination independently of the loop that claimed to have got here.
        Assert.assertTrue(isPresent(desc("Verification Code"), 15),
                "Expected the emailed Verification Code step after the captcha was accepted."
                        + " Labels on screen: " + onScreenLabels());

        System.out.println("Registration submitted — the app is asking for the code emailed to "
                + REGISTER_TEST_EMAIL);

        completeEmailVerification();
    }

    /**
     * Finishes sign-up by reading the emailed code out of the mailbox and submitting it.
     *
     * This is the step that makes the flow unattended. The code goes to the +alias this run
     * registered, which is delivered to the one real mailbox, so the message can be picked
     * out with no chance of catching a code from an earlier run.
     *
     * The field is already focused when the screen opens, and unlike the captcha box nothing
     * is drawn over it, so it takes a plain tap. Typing goes over adb for the same reason it
     * does on the captcha: sendKeys writes the accessibility node rather than the widget.
     */
    private void completeEmailVerification() throws IOException {

        String code = VerificationCodeReader.waitForCode(REGISTER_TEST_EMAIL, MAIL_TIMEOUT_SECONDS);

        // Read the log above before reading this message. "Mailbox opened with ..." means the
        // IMAP side did its job and the message simply is not there, which on this build is
        // what happens: the app shows the verification screen but its backend sends nothing,
        // confirmed by watching the mailbox through a submit and a Resend while the web
        // registrations against the same mailbox delivered normally.
        Assert.assertFalse(code.isEmpty(),
                "No verification code arrived for " + REGISTER_TEST_EMAIL + " within "
                        + MAIL_TIMEOUT_SECONDS + " seconds. If the log above says the mailbox was"
                        + " opened, the message was never sent and this is an app-side defect,"
                        + " not a test or credential problem. Credentials were looked for in "
                        + MailCredentials.describe());

        WebElement field = waitFor(input(0), 15, "the verification code box");
        tapCenter(field);
        sleep(700);

        if (!typeOverAdb(code)) {
            System.out.println("adb typing unavailable, falling back to sendKeys.");
            field.sendKeys(code);
        }

        hideKeyboard();
        sleep(1200);
        shot("verification_code_entered");

        // Next is rendered disabled until the box holds a full code, so it is tapped as a
        // gesture rather than as a clickable element — the same reason the profile avatar is.
        tapCenter(waitFor(desc("Next"), 10, "the Next button on the verification screen"));
        sleep(5000);

        shot("verification_submitted");
        dumpSource("verification_submitted");

        Assert.assertFalse(isPresent(desc("Verification Code"), 5),
                "The verification code was not accepted - still on the Verification Code screen."
                        + " Labels on screen: " + onScreenLabels());

        System.out.println("Verification code accepted. Screen now shows: " + onScreenLabels());
    }

    /**
     * Reads the captcha and submits the form, taking a fresh image whenever the
     * reading is unusable or the app turns it down.
     *
     * A misread is thrown away without submitting: the length check already knows
     * the answer is wrong, and submitting it would spend a round trip to be told so.
     * Only a plausible four digit reading is worth the app's time.
     *
     * @return whether the form was accepted
     */
    private boolean solveCaptchaAndSubmit() throws IOException {

        System.out.println("Captcha OCR: " + CaptchaSolver.availability());

        for (int attempt = 1; attempt <= CAPTCHA_ATTEMPTS; attempt++) {

            System.out.println("Captcha attempt " + attempt + " of " + CAPTCHA_ATTEMPTS);

            String reading = CaptchaSolver.solve(
                    captureCaptchaImage("attempt" + attempt), CAPTCHA_LENGTH);

            if (reading.isEmpty()) {
                System.out.println("Nothing usable from this image. Taking a fresh captcha.");
                refreshCaptcha();
                continue;
            }

            // Entering the code is the hard part of this form, not reading it. Measured so far:
            //
            //   click + sendKeys                  field reads the digits back, form rejected
            //   click + backspace + sendKeys      field reads the digits back, form rejected
            //   click/real tap + DIGIT key events field reads back EMPTY, form rejected
            //
            // sendKeys writes onto the accessibility node, which is why the digits can be read
            // straight back while whatever Flutter validates against stays empty. Typing over
            // adb goes through the input method instead, the same route a thumb takes, so the
            // app sees ordinary IME input rather than a node someone edited underneath it.
            WebElement field = captchaField();

            if (!focusCaptchaField()) {
                System.out.println("Could not put the cursor in the captcha field; skipping this"
                        + " attempt rather than typing into whatever else has focus.");
                refreshCaptcha();
                continue;
            }

            clearByBackspace();

            // Only ever typed once the captcha field is confirmed focused. adb delivers
            // characters to whatever holds focus, and before the tap was fixed that was the
            // Confirm Password box — typing there would quietly corrupt the form.
            if (!typeOverAdb(reading)) {
                System.out.println("adb typing unavailable, falling back to sendKeys.");
                field.sendKeys(reading);
            }

            hideKeyboard();

            // Read the value back, but give it a moment to appear first. adb hands the text to
            // the input method and returns, so the digits land a little after the call — an
            // immediate check reported an empty field on an attempt that actually succeeded.
            String entered = awaitCaptchaText(reading);

            System.out.println("Captcha entered from OCR: " + reading
                    + " (field now reads \"" + entered + "\")");

            if (!reading.equals(entered)) {
                // Not a failure. Typing over adb goes through the input method into the Flutter
                // widget, and the accessibility node does not mirror it — the exact inverse of
                // sendKeys, which used to mirror the node while the widget stayed empty. That
                // is why the node is not trusted as a verdict: the form decides, below.
                System.out.println("Note: the node still reads \"" + entered + "\"; it does not"
                        + " mirror input-method typing. Acceptance is judged by the form.");
            }

            // The button that submits this step is labelled "Next", not "Register" — the only
            // "Register" on the screen is the page title, which is not clickable. Sign-up is
            // staged, and clearing the captcha is what moves the form off step one.
            tap(tappable("Next"), 10, "the Next button on the registration form");

            if (leftTheRegistrationForm()) {
                System.out.println("Captcha accepted on attempt " + attempt);
                return true;
            }

            System.out.println("The form did not move on, so the captcha was rejected."
                    + " Taking a fresh captcha.");
            refreshCaptcha();
        }

        return false;
    }

    /**
     * Types through the device's input method by shelling out to adb.
     *
     * `adb shell input text` delivers characters the way the on-screen keyboard does, to
     * whatever currently has focus. That matters here because the alternative, sendKeys,
     * sets the accessibility node's text directly — the digits can then be read back while
     * the widget that actually validates the form never received them.
     *
     * The device is addressed explicitly when a serial is known, so this keeps working with
     * more than one device attached.
     *
     * @return whether adb reported success; false leaves the caller to fall back
     */
    private boolean typeOverAdb(String text) {

        List<String> command = new ArrayList<>();
        command.add(ADB);
        command.add("-s");
        command.add(UDID);
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

            sleep(600); // the IME delivers the characters asynchronously
            return true;

        } catch (IOException cannotStart) {
            System.out.println("Could not run adb: " + cannotStart.getMessage());
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
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

    /**
     * Puts the caret in the captcha box by tapping the part of it that is not covered up.
     *
     * This is the whole reason the code was never accepted. The captcha input reports bounds
     * spanning the entire row, [52,940][668,1080], but the picture is drawn on top of its
     * right-hand side at [290,943][614,1030] — and an element click goes to the element's
     * centre, (360,1010), which is inside the picture. Every attempt was tapping the image.
     * Focus stayed on Confirm Password, so key presses went nowhere, and sendKeys only ever
     * wrote text onto the accessibility node while the widget that validates the form never
     * saw a keystroke. Same cause behind all three symptoms.
     *
     * Tapping between the left edge of the field and the left edge of the picture lands on
     * bare input. The gap is worked out from the two elements rather than hard-coded so a
     * different screen size still finds it.
     *
     * @return whether the caret ended up in the captcha box
     */
    private boolean focusCaptchaField() {

        Rectangle field = captchaField().getRect();
        Rectangle picture = captchaImageView(false).getRect();

        int x = (field.getX() + picture.getX()) / 2;
        int y = field.getY() + field.getHeight() / 2;

        for (int attempt = 1; attempt <= 2; attempt++) {

            tapAt(x, y);
            sleep(900);

            if (captchaIsFocused()) {
                System.out.println("Captcha field focused by tapping (" + x + "," + y + ").");
                return true;
            }

            System.out.println("Tap at (" + x + "," + y + ") did not focus the captcha field"
                    + " (attempt " + attempt + " of 2).");
        }

        return false;
    }

    /**
     * Empties the focused input the way a person would, with the keyboard.
     *
     * WebElement.clear() is avoided here on purpose. The other inputs on this form are filled
     * with a plain click-then-sendKeys and the app accepts them, while the captcha — the one
     * field that was being cleared first — came back rejected as "Captcha Code is required"
     * on every attempt even though reading the node back showed the digits sitting in it.
     * That split is the signature of Flutter's own text controller losing sync with the
     * accessibility node that clear() writes through: the automation sees the text, the
     * widget that validates the form does not.
     *
     * The cursor is sent to the end first because a tap lands it wherever it was touched, and
     * backspace only deletes what is behind it. The count is comfortably above the field's
     * cap; extra presses on an empty field do nothing.
     */
    private void clearByBackspace() {

        driver.pressKey(new KeyEvent(AndroidKey.MOVE_END));

        for (int i = 0; i < 12; i++) {
            driver.pressKey(new KeyEvent(AndroidKey.DEL));
        }
    }

    /**
     * Waits briefly for the field to show the code, and returns whatever it settled on.
     *
     * Typing over adb is asynchronous: the call returns once the input method has been handed
     * the characters, not once the widget has them. Checking immediately made a successful
     * attempt look like a failed one in the log.
     */
    private String awaitCaptchaText(String expected) {

        long deadline = System.currentTimeMillis() + 3000;
        String text = captchaFieldText();

        while (!expected.equals(text) && System.currentTimeMillis() < deadline) {
            sleep(300);
            text = captchaFieldText();
        }

        return text;
    }

    /** The captcha input's current contents, or "" when the field cannot be found. */
    private String captchaFieldText() {

        try {
            String text = captchaField().getAttribute("text");
            return text == null ? "" : text;
        } catch (AssertionError | RuntimeException cannotRead) {
            return "";
        }
    }

    /**
     * Whether submitting moved the app off the form.
     *
     * This deliberately does not test for the captcha input disappearing, which is what it
     * used to do. That signal was wrong in a way that produced a green run for a submit that
     * never happened: the app re-renders the field after typing, the locator stopped matching
     * it, and "I can no longer find the field" got read as "the form moved on" while the app
     * was still sitting there with "Captcha Code is required" on screen.
     *
     * Two honest signals replace it. A message mentioning the captcha is the app telling us
     * the code was missing or wrong, and it means rejected right now rather than after the
     * full timeout. The page title going away is what actually shows the step was accepted —
     * sign-up is staged, so the next step is a different screen.
     *
     * A verdict is returned rather than thrown because a rejection is an ordinary outcome
     * here: the caller's whole job is to fetch another image and try again.
     */
    private boolean leftTheRegistrationForm() {

        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {

            // Accepting the code takes sign-up to the emailed verification step, so that
            // screen appearing is positive proof the form went through. An earlier version
            // inferred success from the form's title no longer being findable, which is how a
            // run once reported a pass for a submit that never happened.
            if (isPresent(desc("Verification Code"), 1)) {
                return true;
            }

            if (isPresent(desc("Captcha"), 1)) {
                System.out.println("The app is reporting a captcha problem on the form: "
                        + onScreenLabels());
                return false;
            }

            sleep(500);
        }

        return false;
    }

    /**
     * Types test data into the sign-up form. The field set is discovered at runtime from
     * each input's hint rather than hard-coded, and anything unrecognised is left blank.
     */
    private void fillRegistrationFields() {

        List<WebElement> fields = driver.findElements(
                AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.EditText\")"));

        System.out.println("Registration form exposes " + fields.size() + " input field(s).");

        for (int i = 0; i < fields.size(); i++) {

            WebElement field = fields.get(i);
            String hint = field.getAttribute("hint");
            String value = registrationValueFor(hint == null ? "" : hint);

            if (value == null) {
                System.out.println("Skipping field " + i + " (hint=\"" + hint + "\") - not auto-filled.");
                continue;
            }

            try {
                field.click();
                field.sendKeys(value);
                hideKeyboard();
            } catch (Exception e) {
                System.out.println("Could not fill field " + i + " (hint=\"" + hint + "\"): " + e.getMessage());
            }
        }
    }

    /**
     * Test data per field, chosen from the field's hint. Returns null for anything that
     * must not be auto-filled. The captcha input's hint is only "Enter", so the default
     * for an unrecognised field is deliberately null rather than a guess.
     */
    private String registrationValueFor(String hint) {

        String h = hint.toLowerCase();

        if (h.contains("code") || h.contains("otp") || h.contains("captcha") || h.contains("verif")) {
            return null;
        }
        if (h.contains("email")) {
            return REGISTER_TEST_EMAIL;
        }
        if (h.contains("phone") || h.contains("mobile")) {
            return "0123456789";
        }
        if (h.contains("password")) {
            return REGISTER_TEST_PASSWORD;
        }
        if (h.contains("name")) {
            return "QA Test User";
        }
        return null;
    }

    /**
     * Opens Profile Setting from the dashboard. The entry point is the avatar in the
     * top-left corner, which carries no accessibility label at all — it is identified by
     * position among the clickable icons, and tapped as a gesture because a plain element
     * click on it does nothing.
     */
    private void openProfilePanel() {

        List<WebElement> icons = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.ImageView\").clickable(true)"));

        for (WebElement icon : icons) {
            Rectangle r = icon.getRect();
            if (r.getY() < 300 && r.getX() < 200) {
                tapCenter(icon);
                sleep(2500);
                return;
            }
        }

        dumpSource("dashboard_no_avatar");
        throw new AssertionError("Could not find the profile avatar on the dashboard. "
                + "Labels on screen: " + onScreenLabels());
    }

    /** Shared by testLogin (to reach a clean state) and testLogout (as the case itself). */
    private void signOut() {

        openProfilePanel();
        dumpSource("profile_screen");
        shot("profile_panel");

        // "Log Out" sits below the fold at the bottom of the Profile Setting list, and is
        // capitalised exactly this way — descriptionContains is case-sensitive.
        for (int i = 0; i < 4 && !isPresent(desc("Log Out"), 1); i++) {
            swipeUp();
            sleep(900);
        }
        shot("profile_scrolled");

        tapCenter(waitFor(desc("Log Out"), 10, "the Log Out entry in Profile Setting"));
        sleep(1800);
        shot("logout_confirm_dialog");

        tapCenter(waitFor(desc("Yes, Logout"), 10, "the 'Yes, Logout' confirmation button"));
        sleep(4000);
    }
}
