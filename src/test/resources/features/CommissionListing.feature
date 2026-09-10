Feature: Commission Listing

  As a UMPay account holder who has brought other people to the platform
  I want to see what I have earned and whether it has been settled
  So that I know what is owed to me

  # The Commission Listing entry on the profile panel. The web suite covers the same screen and
  # the same commissions, each carrying a reference number and whether it has been settled.

  @commission @Commission_Listing_TC_001
  Scenario: The commission listing shows what the account has earned
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Commission Listing" from the profile panel
    Then the "Commission Listing" screen should open
    And the screen should list something

  # A listing this long has to be narrowable, or a commission from months ago cannot be found.
  @commission @Commission_Listing_TC_002
  Scenario: The commissions can be narrowed
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Commission Listing" from the profile panel
    Then the "Commission Listing" screen should open
    And the screen should carry "Filter"
