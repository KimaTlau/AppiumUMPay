Feature: UMPay mobile registration

  As a new UMPay user on Android
  I want to sign up with an email address and verify it
  So that the whole account creation path is known to work without a person driving it

  # WHAT THIS COSTS TO RUN
  #
  # It creates a real account on the test environment. A registered address cannot be
  # reused, so each run takes a fresh one through a +timestamp alias - every run gets its
  # own address while all of them deliver to the same real mailbox, which is what lets the
  # emailed code be read back with no chance of catching one from an earlier run.
  #
  # It is tagged @register and excluded from every unattended run for that reason. Launch
  # it deliberately:
  #
  #   mvn test -Dcucumber.filter.tags="@register" \
  #            -Dumpay.udid=emulator-5554 -Dumpay.deviceName=Android_Emulator \
  #            -Dumpay.platformVersion=16
  #
  # HOW IT RUNS WITHOUT A PERSON
  #
  # The captcha is read by OCR from a crop of the device screenshot - the app draws it to a
  # canvas, so there is no image source to decode as there would be on the web. A reading
  # that is not four digits is discarded and a fresh image requested without submitting,
  # because sending a known-wrong answer only spends a round trip to be told so.
  #
  # TYPING GOES OVER ADB
  #
  # sendKeys writes the accessibility node rather than the widget for the captcha and the
  # verification box, so the app never sees those characters. Both are typed through
  # "adb shell input text", which reaches the real input method.
  #
  # WHERE THIS SCENARIO STOPS, AND WHY
  #
  # It ends when the app asks for the emailed code. It does not fetch that code or type it
  # in, and that is a deliberate boundary rather than an oversight.
  #
  # Everything up to this point is the app's own behaviour and is worth guarding: the
  # navigation, the Email tab, the fields filled from their hints, the captcha read off the
  # screen by OCR, and the account actually being created. Reaching the Verification Code
  # screen proves all of it - the backend accepted the registration and issued the
  # challenge.
  #
  # What comes after depends on an email that this build does not send. Two registrations
  # were driven to completion during this work, both left sitting on this screen with
  # nothing delivered, while the same mailbox holds 249 verification codes from web
  # registrations to the identical address format. So the code entry cannot be exercised,
  # and a test that waited for it would report an application defect every night in the
  # middle of a scenario about registration.
  #
  # The steps for fetching and entering the code still exist and still work; nothing here
  # uses them. When the email is fixed, add these two lines back before the close:
  #
  #     When I enter the verification code from the mailbox
  #     Then the registration should be accepted
  #
  # WHERE THE ADDRESS AND PASSWORD COME FROM
  #
  # Register_TestData.xlsx, laid out like the web suite's workbook of the same name. The
  # address in column 1 is the real mailbox; UniqueEmail in column 6 says whether to
  # register it as it stands or take a fresh +timestamp alias of it, and this row says Yes
  # because a registered address cannot be reused. -Dumpay.register.email still overrides
  # the workbook for re-running against one particular address.
  #
  # THE APP IS CLOSED AT THE END
  #
  # Stopping deep inside a flow leaves the app on the Verification Code screen, and the
  # session does not reset it, so the next run would open there and have to find its own way
  # out. That is not hypothetical: it is how a setup step ended up backing out to the phone's
  # home screen. Closing the app means the next launch starts from the top.

  @register @Register_TC_001
  Scenario Outline: A new account is registered and the app asks to verify the email address
    Given the UMPay app is open on the login screen
    When I open the registration form
    And I fill in the registration form using "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I solve the captcha and submit the form
    Then the app should ask for the emailed verification code
    And the app is closed

    Examples:
      | excelFileName | excelSheetName | row |
      | Register_TestData.xlsx | Sheet1 | 1 |
