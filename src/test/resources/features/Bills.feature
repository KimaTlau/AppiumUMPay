Feature: Bills

  As a UMPay account holder on the phone
  I want a ledger of everything that has moved
  So that I can find a payment I made and see what it cost

  # The Bills tile on the dashboard, showing the same ledger the web suite covers - the same
  # conversions, on the same dates, for the same amounts.
  #
  # Nothing here filters or opens an entry. The web asks a good deal more of its own Bills page:
  # that filtering by kind leaves only that kind, that a stretch of days with nothing in it says
  # so, that a receipt can be downloaded. All of that needs driving the screen rather than reading
  # it, and is the next tranche rather than this one.

  @bills @Bills_TC_001
  Scenario: The bills list what the account has done
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open Bills from the dashboard
    Then the "Bills" screen should open
    And the screen should list something

  # An entry missing its date cannot be placed and one missing its amount cannot be reconciled.
  @bills @Bills_TC_002
  Scenario: Every transaction names its kind, its date and its amount
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open Bills from the dashboard
    Then the "Bills" screen should open
    And every bill should name its kind, its date and its amount

  # The ledger is long, so it offers to be narrowed. That the filter is there is worth holding
  # the screen to; what it does with it is the next tranche.
  @bills @Bills_TC_003
  Scenario: The ledger offers to be narrowed
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open Bills from the dashboard
    Then the "Bills" screen should open
    And the screen should carry "Filter"
