Feature: UMPay mobile end to end journey

  As a UMPay user on Android
  I want to sign in, move through every money flow, and sign out
  So that one run proves the app hangs together rather than each screen alone

  # WHY THIS EXISTS ALONGSIDE THE PER-FEATURE FILES
  #
  # UMPayLogin.feature, Deposit.feature and the rest each prove one flow, and each signs in
  # for itself, so they say nothing about whether the flows work in succession against one
  # session. This is deliberately one long scenario for that reason: everything after the
  # first step runs on the state the previous step left behind.
  #
  # That is also its weakness. A single scenario stops at the first failure, so a broken
  # Deposit hides Withdrawal, Convert and the transfer routes. Keep the per-feature files for
  # pinpointing a flow; use this one to prove the journey.
  #
  # WHY IT DOES NOT START WITH REGISTRATION, THE WAY THE WEB ONE DOES
  #
  # The web EndToEndTest.feature registers an account and carries it through every flow.
  # That cannot be mirrored here, and the reason is in Register.feature: this build sends no
  # verification email to the device's address, so mobile registration stops on the
  # Verification Code screen and closes the app. There is no signed-in session to carry
  # forward. Registration is proved on its own by Register.feature instead.
  #
  # When the email is fixed, the journey to add in front of the sign in below is:
  #
  #     When I open the registration form
  #     And I fill in the registration form with a fresh email address
  #     And I solve the captcha and submit the form
  #     And I enter the verification code from the mailbox
  #     Then the registration should be accepted
  #
  # HOW THE FLOWS FOLLOW ONE ANOTHER
  #
  # Only the first flow signs in. Each navigation step returns to the dashboard before
  # opening what it was asked for, so the flows can be written one after another without a
  # step in between putting the app back where the next one expects it.
  #
  # WHERE THE DATA COMES FROM
  #
  # Each flow reads its own module's workbook, named inline rather than through an Examples
  # table. The web EndToEndTest.feature does the same and for the same reason: one Examples
  # row cannot name six different workbooks, and a journey through six flows needs all six.
  #
  # WHAT IT COSTS TO RUN
  #
  # Nothing is submitted and nothing is sent - the same rule the per-feature files keep. The
  # journey fills each form, asserts what the app does with it, and moves on. No money moves
  # and no account is created.
  #
  # Run it on its own:  mvn test -Dcucumber.filter.tags="@e2e"

  @e2e
  Scenario: A user signs in, fills every money form, prices a transfer and signs out

    # 1 - Login
    Given the UMPay app is open on the login screen
    When I sign in with the credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should reach the dashboard
    And the dashboard should offer the money actions

    # 2 - Deposit: the amount alone does not enable Confirm, the payment details do
    When I open the "Deposit" form from the dashboard
    Then the "Deposit" form should be shown
    And the "Confirm" action should be disabled
    When I enter the deposit amount in "1" of "Sheet1" of "Deposit_TestData.xlsx"
    Then the amount should be accepted
    And the form should ask for "Payment information"
    And the transaction is deliberately not submitted

    # 3 - Withdrawal
    When I open the "Withdrawal" form from the dashboard
    Then the "Withdrawal" form should be shown
    When I enter the withdrawal amount in "1" of "Sheet1" of "Withdraw_TestData.xlsx"
    Then the amount should be accepted
    And the transaction is deliberately not submitted

    # 4 - Convert
    When I open the "Convert" form from the dashboard
    Then the "Convert" form should be shown
    When I enter the conversion amount in "1" of "Sheet1" of "Convert_TestData.xlsx"
    Then the amount should be accepted
    And the transaction is deliberately not submitted

    # 5 - Transfer: the hub is reachable and the wallet route is offered
    When I open the transfer hub
    Then the transfer hub should offer the route "UMPay to Existing template"
    And the transfer hub should offer the route "UMPay to UMPay Wallet"

    # 6 - Global transfer: UnionPay China prices the amount and offers to send it
    When I take the "UnionPay China" route
    Then the UnionPay transfer form should be shown
    When I enter the UnionPay amount in "1" of "Sheet1" of "GlobalTransfer_TestData.xlsx"
    Then the transfer should be priced
    And the UnionPay transfer should become sendable
    And the transfer is deliberately not sent

    # 7 - Logout
    When I sign out
    Then I should be back on the login screen
