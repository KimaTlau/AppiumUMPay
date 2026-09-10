Feature: Security

  As a UMPay account holder on the phone
  I want to see what guards my account
  So that my password, my PIN and my authenticator are mine to control

  # The Security entry on the profile panel. The web suite covers the same screen and finds three
  # things guarding the account; the phone has a fourth the web does not - Biometric Login - which
  # is worth holding it to precisely because there is nothing on the web to compare it against.
  #
  # NOTHING HERE IS PRESSED. Changing the password would lock every other scenario out of the
  # account, and removing the authenticator would take the account with it. The web suite keeps
  # the same rule for the same reason.

  @security @Security_TC_001
  Scenario: The page offers everything the account is guarded by
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Security" from the profile panel
    Then the "Security" screen should open
    And the screen should carry "Login Password, PIN, 2FA Authenticator"

  # The phone guards the account with something the web does not offer at all.
  @security @Security_TC_002
  Scenario: The phone offers a biometric sign in the web has no equivalent for
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Security" from the profile panel
    Then the "Security" screen should open
    And the screen should carry "Biometric Login"
