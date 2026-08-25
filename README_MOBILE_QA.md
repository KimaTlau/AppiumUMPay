# UMPay Mobile QA — How to run

This adds `MobileFlowTest.java` to your existing `AppiumUMPay` project. It drives the
UMPay Android app (`UMPay_3.2.3_test_2.9.apk`, package `com.umpay.me`) through Register,
Login, Deposit, Withdraw, Transfer, Convert, and Logout on your connected Redmi 13C, and
saves a numbered screenshot at every meaningful step to a `screenshots/` folder in the
project root — the same pattern used for the web QA report.

Register (`testRegister`, priority 1) runs first, since it needs the logged-out Login
screen to reach the "Register" link — if the app launches into an already-authenticated
session (likely, since `noReset=true` preserves state between runs), the test logs out
first so Register is reachable, then leaves the app back at the Login screen for
`testLogin` (priority 2) to pick up from. It fills the Email method's Email/Password
fields with throwaway test data, confirms a Captcha Code field is present, and checks the
Phone Number method renders — but it never reads/enters the Captcha code and never taps
the Register button, so no new account is actually created (same safety principle as the
financial flows below).

## Why I couldn't run this myself

This app is built with **Flutter**, confirmed by decompiling the APK
(`libflutter.so`, `flutter_assets/`, Flutter's `LaunchTheme`). Flutter apps don't expose
native Android resource-ids the way the Selenium web tests could rely on stable
`id=` locators — elements are located by visible **text** or **accessibility
label** instead. I extracted the real on-screen strings from the compiled app
(`Login`, `Email`, `Password`, `Deposit`, `Withdraw`, `Select Currency`, `From Template`,
`Payment Type`, `Convert To`, etc. all matched what we saw on web), so the locators in
`MobileFlowTest.java` are built from those, not guesses — but I have no way to actually
run this against your physical device from here, so some may still need a tweak once you
run it against the live app.

The build is also **release-mode** (no debug/VM-service flag found), so the more precise
`appium-flutter-driver` approach isn't available for this APK — `UiAutomator2` with
text-based selectors is the correct fallback for a production Flutter build like this one.

## Prerequisites (same as your existing Appium setup)

1. Redmi 13C connected via USB with USB debugging enabled, `adb devices` shows it.
2. UMPay app already installed on the device (the test does **not** install/reset it —
   `noReset=true` — so your existing app data/session state is preserved unless you log
   out).
3. Appium server running locally:
   ```
   appium
   ```
   (defaults to `http://127.0.0.1:4723`, matching `MobileFlowTest.java`).

## Running it

**Option A — IntelliJ (recommended, matches how `UMPayTest.java` is already set up):**
Right-click `MobileFlowTest.java` → Run. TestNG will execute the 6 methods in order
(Login → Deposit → Withdraw → Transfer → Convert → Logout) via the `priority` values.

**Option B — command line:**
```
mvn test
```
This uses the `testng.xml` suite file and the surefire plugin binding added to `pom.xml`.

## Safety boundary (same as the web QA pass)

Every flow fills in the form fully — amount, currency, saved payment account/template —
and reaches the final confirmation/order-summary screen, but **never taps the final
"Confirm"/"Convert" button**. No real funds move. Register follows the same principle:
the form is filled with test data but the test **never reads/enters the Captcha code and
never taps the Register button**, so no new account is created. Please don't remove those stopping
points if you tweak the test.

## If a step fails

Flutter locators are the one part I couldn't verify live. If a step throws a
`NoSuchElementException`:
1. Run **Appium Inspector** (or `adb shell uiautomator dump` + `cat /sdcard/window_dump.xml`)
   against the app at that exact screen.
2. Find the real `text` or `content-desc` for the element.
3. Swap it into the corresponding `byText(...)` / `byDesc(...)` call — each screen's logic
   is self-contained so this is usually a one-line fix.

## What to send back

Once it runs (fully or partially), please send me:
- The `screenshots/` folder (or zip it) — even partial runs are useful.
- The console/test output (pass/fail per method, and the stack trace for any failure).

I'll turn that into the same style of polished HTML report used for the web QA pass, with
the real pass/fail results and screenshot gallery.
