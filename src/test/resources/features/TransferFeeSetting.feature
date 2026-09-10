Feature: Transfer fee setting

  As a UMPay account holder on the phone
  I want to say who pays the fee on a transfer
  So that I am not deciding it every time

  # The Transfer Fee Setting entry on the profile panel, offering the same three ways the web
  # suite covers.
  #
  # NOTHING HERE PRESSES UPDATE. On the web, saving this setting raises no request at all and the
  # choice is gone on reload - a defect recorded there. Pressing it here would either hit the same
  # wall or quietly change a setting the transfer scenarios rely on, so this reads the screen and
  # leaves it. Whether the phone saves it is worth asking, and worth asking deliberately.

  @feesetting @Transfer_Fee_Setting_TC_001
  Scenario: The page offers every way the fee can be settled
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Transfer Fee Setting" from the profile panel
    Then the "Transfer Fee Setting" screen should open
    And the screen should carry "Fee will be selected by user, Fee will always be paid by other party, Fee will always be paid by me"

  @feesetting @Transfer_Fee_Setting_TC_002
  Scenario: The page says what it is asking and offers to save it
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Transfer Fee Setting" from the profile panel
    Then the "Transfer Fee Setting" screen should open
    And the screen should carry "Select who will pay the fee, Update"
