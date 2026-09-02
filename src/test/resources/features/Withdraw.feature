Feature: UMPay mobile withdraw

  As a UMPay user on Android
  I want to fill in a withdrawal
  So that the form behind the dashboard's Withdrawal tile is known to work

  # The dashboard tile is labelled "Withdrawal" while the web suite calls the flow Withdraw.
  # The file is named for the flow so it matches its web counterpart; the tile is named for
  # what the app actually says, because that is what has to be found on screen.
  #
  # NOTHING HERE IS SUBMITTED
  #
  # The scenario stops on a completed form and never presses Confirm. Confirming moves real
  # money on the test environment and no test can undo it. As with the other money flows
  # this is enforced rather than trusted: no step and no page object method presses the
  # action.

  @money
  Scenario Outline: A withdrawal is validated before it can be confirmed
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the "Withdrawal" form from the dashboard
    Then the "Withdrawal" form should be shown
    When I enter the withdrawal amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the amount should be accepted
    And the transaction is deliberately not submitted

    Examples:
      | excelFileName | excelSheetName | row |
      | Withdraw_TestData.xlsx | Sheet1 | 1 |
