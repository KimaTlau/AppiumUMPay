Feature: Payment accounts

  As a UMPay account holder on the phone
  I want the accounts I can be paid out to kept somewhere I can see them
  So that a withdrawal has somewhere to go

  # The Payment entry on the profile panel, holding the same saved accounts the web suite covers -
  # AFIRME, Bangkok Bank and the rest, each with the name and number it is identified by.
  #
  # NOTHING HERE ADDS OR REMOVES AN ACCOUNT. These are what the withdraw and payout flows draw on,
  # and one removed here would fail a scenario somewhere else entirely - the same rule the web
  # suite keeps.

  @payment @Payment_TC_001
  Scenario: The saved payment accounts are listed
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Payment" from the profile panel
    Then the "Payment" screen should open
    And the screen should list something

  # An account saved without the details a payout is addressed to is money sent into the dark.
  @payment @Payment_TC_002
  Scenario: Every saved account names what identifies it
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Payment" from the profile panel
    Then the "Payment" screen should open
    And the screen should carry "Account Name, Account Number"
