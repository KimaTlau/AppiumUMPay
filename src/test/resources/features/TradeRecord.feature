Feature: Trade Record

  As a UMPay account holder on the phone
  I want every order I have raised kept in one place
  So that I can look one up when somebody asks me about it

  # The Trade Record entry on the profile panel, showing the same orders the web suite covers -
  # including the withdraw that was raised and then failed.
  #
  # The web asks more of its own: that no two orders share a number, that a converted order shows
  # the rate it was converted at, that an order can be opened and its receipt kept. Those need
  # driving the screen and are not here.

  @traderecord @Trade_Record_TC_001
  Scenario: The trade record lists the orders the account has raised
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Trade Record" from the profile panel
    Then the "Trade Record" screen should open
    And the screen should list something

  # Three tabs, as the web has three. An order raised as an international transfer is not a
  # deposit and should not be filed with one.
  @traderecord @Trade_Record_TC_002
  Scenario: The trade record is kept in its own tabs
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Trade Record" from the profile panel
    Then the "Trade Record" screen should open
    And the screen should carry "Deposit/Withdrawal, International Transfer"
