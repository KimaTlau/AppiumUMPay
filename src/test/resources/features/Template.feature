Feature: Templates

  As a UMPay account holder on the phone
  I want the destinations I send to saved
  So that I do not have to type them in again

  # The Templates entry on the profile panel, holding the same saved destinations the web suite
  # covers. Nothing here saves or removes one: the transfer scenarios send to these, and one
  # removed would fail a scenario in another file.

  @templates @Template_TC_001
  Scenario: The saved templates are listed
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Templates" from the profile panel
    Then the "Templates" screen should open
    And the screen should list something

  @templates @Template_TC_002
  Scenario: The page offers to add a template
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Templates" from the profile panel
    Then the "Templates" screen should open
    And the screen should carry "Add Template"
