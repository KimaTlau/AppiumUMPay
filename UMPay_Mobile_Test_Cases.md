# UMPay Mobile (Android) — Test Case Specification

App under test: `UMPay_3.2.3_test_2.9.apk` (package `com.umpay.me`, version 3.2.3, build 114)
Device: Xiaomi Redmi 13C, Android 15 (UDID `YPYXSKCQXCXWVW55`)
Account: elkaytlau7@gmail.com (same test account used for the web QA pass)
Automation: Appium + UiAutomator2 (`MobileFlowTest.java`)

Status: **not yet executed** — these are the planned steps/expected results.
Actual Result and Pass/Fail are left blank below and will be filled in from your run's
console output and screenshots once you send them back.

| ID | Module | Title | Steps | Expected Result | Actual Result | Result |
|----|--------|-------|-------|------------------|----------------|--------|
| TC-M-00 | Register | Open Register and fill the Email method form | 1. From the Login screen, tap Register<br>2. Enter a test email + password<br>3. Observe the Captcha requirement<br>4. Tap the Phone Number tab and back | Register form accepts email/password input; a Captcha code is required before submission; Phone Number method shows Country + Phone number fields | _pending run_ | _pending_ |
| TC-M-01 | Login | Log in with valid credentials | 1. Launch app<br>2. Enter email + password<br>3. Tap Login | User authenticated, lands on Home showing wallet balances | _pending run_ | _pending_ |
| TC-M-02 | Deposit | Open Deposit and fill the form | 1. Tap Deposit<br>2. Select currency<br>3. Enter amount 500<br>4. Select payment type (Bank) | Form reaches a fully-configured state (payment type + amount) without submitting | _pending run_ | _pending_ |
| TC-M-03 | Withdraw | Open Withdraw and select a saved payout account | 1. Tap Withdraw<br>2. Enter amount 500<br>3. Choose "From Template"<br>4. Select a saved account | Order summary (fee/total) displays for the selected account without submitting | _pending run_ | _pending_ |
| TC-M-04 | Transfer | Transfer to an existing template | 1. Tap Transfer<br>2. Choose "UMPay to Existing template"<br>3. Select a saved template<br>4. Enter amount 150 | Receiver auto-populates from template; fee/receiver-gets amount shown without submitting | _pending run_ | _pending_ |
| TC-M-05 | Convert | Convert HKD to USD | 1. Tap Convert<br>2. Select USD as target<br>3. Enter amount 1000 | Live exchange rate and converted amount shown without submitting | _pending run_ | _pending_ |
| TC-M-06 | Logout | Log out of the session | 1. Open profile<br>2. Tap Logout<br>3. Confirm Yes | Session ends, app returns to the Login screen | _pending run_ | _pending_ |

## Scope note

Matches the safety boundary used for the web QA report: every flow is driven to its
final confirmation/order-summary screen but the final "Confirm"/"Convert" action is never
tapped, so no real funds move during this pass. Register follows the same principle: the
form is filled with test data, but the Captcha code is never read/entered and the
Register button is never tapped, so no new account is actually created.

## Known risk areas to watch when you run it

- **Navigation pattern**: the web app uses a left sidebar (Home/Deposit/Withdraw/...). The
  mobile app likely uses a bottom nav bar or drawer instead — `MobileFlowTest.java` taps
  by label text (`"Deposit"`, `"Withdraw"`, etc.), which should work either way, but the
  exact tab layout hasn't been visually confirmed.
- **Biometric prompt**: the app declares `USE_BIOMETRIC`/`USE_FINGERPRINT` permissions. If
  a biometric/PIN prompt appears on launch or after backgrounding, the test attempts to
  dismiss common "Cancel"/"Not now" dialogs automatically, but this hasn't been verified
  against the real prompt UI.
- **Amount field selection**: several screens locate the amount input by "first EditText
  on screen" — if a screen has more than one text field before the amount box, this may
  need to target a specific field instead.
