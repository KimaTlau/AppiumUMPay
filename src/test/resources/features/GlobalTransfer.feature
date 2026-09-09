Feature: UMPay mobile global transfer

  As a UMPay user on Android
  I want to send money abroad through UnionPay
  So that the route that converts and prices a transfer is known to work

  # WHAT THIS FILE COVERS, AND WHAT Transfer.feature COVERS
  #
  # The split follows the web suite's. There, Transfer.feature covers the breadth - every
  # area, every route, and the state each form is in - while GlobalTransfer.feature drives
  # the UnionPay route in depth. The same division is kept here so a reader who knows one
  # suite can find their way around the other.
  #
  # On mobile both UnionPay routes are reached from the transfer hub rather than from a
  # separate Global Transfer area, so each scenario opens the hub and names the route it
  # takes from there. There is no Background: the scenarios that enter an amount sign in
  # from the transfer workbook and the ones that do not sign in from Login_TestData, so the
  # sign in belongs to the scenario rather than above it.
  #
  # WHERE THIS DIFFERS FROM THE WEB SUITE
  #
  # The web GlobalTransfer.feature drives a transfer out of a spreadsheet all the way to a
  # submitted order and then asserts the order completed. Nothing on mobile is sent: every
  # scenario stops on a priced form and never presses the action. Sending moves real money on
  # the test environment and no test can undo it, and as with the other money flows this is
  # enforced rather than trusted - no step and no page object method taps the action, so a
  # later scenario cannot send one by accident either.
  #
  # CHINA CONVERTS, GLOBAL DOES NOT
  #
  # The two routes are the same screen with one difference that matters. China quotes an
  # amount in USD into CNY and therefore has a receive currency; Global sends without
  # converting and has none. Asserting the absence is the whole point of the last scenario.

  # WHERE THE AMOUNTS COME FROM
  #
  # GlobalTransfer_TestData.xlsx, the same workbook name the web suite uses for this flow.
  # Row 1 carries an amount above the form's stated 10.00 USD minimum and row 2 one below
  # it, so the scenario that expects pricing and the scenario that expects refusal are the
  # same steps against different data rather than two figures typed into the feature.
  #
  # The two scenarios that assert only what the empty form states name no row: they enter
  # nothing, so they sign in from Login_TestData and stop there.

  @globaltransfer @Global_Transfer_TC_001
  Scenario: UnionPay China states its limits before anything is entered
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the transfer hub
    And I take the "UnionPay China" route
    Then the UnionPay transfer form should be shown
    And the UnionPay form should state "Pay Currency"
    And the UnionPay form should state "Receive Currency"
    And the UnionPay form should state "Card Available Balance"
    And the UnionPay form should state "Limit Min 10.00 USD"
    And the UnionPay form should state "Limit Max 10,000.00 USD"
    And the UnionPay transfer should not be sendable yet
    And the transfer should not be priced

  # The heart of this feature. An amount above the minimum makes the form quote the whole
  # transfer - rate, fee, total and what actually arrives - and only then does it offer to
  # send it. Both halves matter: a form that priced without enabling, or enabled without
  # pricing, would be broken in a way that "the button exists" would never catch.
  @globaltransfer @Global_Transfer_TC_002
  Scenario Outline: UnionPay China prices the transfer once a valid amount is entered
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the transfer hub
    And I take the "UnionPay China" route
    Then the UnionPay transfer form should be shown
    When I enter the UnionPay amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the transfer should be priced
    And the priced transfer should show "Original Amount"
    And the priced transfer should show "Estimate Rate"
    And the priced transfer should show "Fee"
    And the priced transfer should show "Total Amount"
    And the priced transfer should show "Approximate Received"
    And the UnionPay transfer should become sendable
    And the transfer is deliberately not sent

    Examples:
      | excelFileName | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | Sheet1 | 1 |

  # The form states a minimum of 10.00 USD. An amount under it should leave the transfer
  # unpriced and unsendable - the same validation as above, tested from the failing side.
  @globaltransfer @Global_Transfer_TC_003
  Scenario Outline: UnionPay China refuses an amount below its minimum
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the transfer hub
    And I take the "UnionPay China" route
    Then the UnionPay transfer form should be shown
    When I enter the UnionPay amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the transfer should not be priced
    And the UnionPay transfer should not be sendable yet

    Examples:
      | excelFileName | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | Sheet1 | 2 |

  # Global is the same screen as China with one difference that matters: it does not convert,
  # so it has no receive currency. Asserting the absence is the point of the scenario.
  @globaltransfer @Global_Transfer_TC_004
  Scenario: UnionPay Global offers the same form without a receive currency
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the transfer hub
    And I take the "UnionPay Global" route
    Then the UnionPay transfer form should be shown
    And the UnionPay form should state "Pay Currency"
    And the UnionPay form should state "Limit Min 10.00 USD"
    And the UnionPay form should state "Limit Max 10,000.00 USD"
    And the UnionPay form should state "Remark"
    And the UnionPay form should not offer "Receive Currency"
    And the UnionPay transfer should not be sendable yet
