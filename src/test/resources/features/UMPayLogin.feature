Feature: UMPay mobile login

  As a UMPay user on Android
  I want to sign in, check the contents of my dashboard, and sign out again
  So that the rest of the app is reachable at all

  # This is the mobile counterpart of the web suite's UMPayLogin.feature, and the file
  # set here follows that project's: one feature file per flow, named the same way, so
  # a flow can be found in either suite by the same name.

  # HOW ELEMENTS ARE FOUND IN THIS APP
  #
  # UMPay's Android app is Flutter, and it publishes no resource-ids whatsoever - the only
  # id anywhere in the tree is android:id/content, which belongs to the platform. Flutter
  # also renders its own text, so the "text" attribute is always empty and every caption
  # lives in the accessibility label instead. Buttons, tabs and tiles are therefore found by
  # their content-desc, which is a real locator and stable across builds.
  #
  # The text inputs are the exception, and it is worth knowing why rather than being
  # surprised by it. They carry no id, no content-desc and no text, and the one attribute
  # that would tell them apart - password="true" - cannot be selected on, because Appium's
  # UiSelector parser rejects both password(true) and password(false). That was checked
  # against the running app, not assumed. Their position among the EditText nodes is the
  # only handle the application offers.
  #
  # Adding Semantics(identifier: 'login_email') around those fields in the Flutter source
  # would fix it properly and let the positional locator be deleted.
  #
  # WHERE THE RUN IS POINTED
  #
  # The defaults target the physical Redmi. For the headless emulator:
  #   mvn test -Dumpay.udid=emulator-5554 -Dumpay.deviceName=Android_Emulator \
  #            -Dumpay.platformVersion=16

  @login @smoke @Login_TC_001
  Scenario: The app opens on a login screen offering both sign in methods
    Given the UMPay app is open on the login screen
    Then the login screen should offer both sign in methods

  @login @smoke @Login_TC_002
  Scenario Outline: A valid account signs in and reaches the dashboard
    Given the UMPay app is open on the login screen
    When I sign in with the credentials in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then I should reach the dashboard
    And the dashboard should offer the money actions

    Examples:
      | excelFileName | excelSheetName | row |
      | Login_TestData.xlsx | Sheet1 | 1 |

  @login @Login_TC_003
  Scenario Outline: A signed in user can sign out again
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I sign out
    Then I should be back on the login screen

    Examples:
      | excelFileName | excelSheetName | row |
      | Login_TestData.xlsx | Sheet1 | 1 |
