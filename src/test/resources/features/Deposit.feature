Feature: UMPay mobile deposit

  As a UMPay user on Android
  I want to fill in a deposit
  So that the form behind the dashboard's Deposit tile is known to work

  # NOTHING HERE IS SUBMITTED
  #
  # The scenario stops on a completed form and never presses Confirm. Confirming moves real
  # money on the test environment and no test can undo it.
  #
  # This is enforced rather than trusted: no step and no page object method presses the
  # action, so a future scenario cannot submit one by accident either.
  #
  # WHAT IS ACTUALLY BEING TESTED
  #
  # Confirm exists from the moment the form opens but starts disabled - clickable="false" -
  # and only becomes clickable once the form's own rules are satisfied. Asserting that it
  # starts disabled and stays disabled until the form is complete tests the validation,
  # where asserting that the button merely exists would pass on a completely empty form.
  #
  # The action's label is passed in from here rather than hidden in the page object, because
  # each form names its own: Deposit and Withdrawal say Confirm, Convert says Convert.

  # Deposit validates in stages, which is worth testing as stages.
  #
  # Confirm is on screen from the moment the form opens but sits disabled. Entering an
  # amount is not enough to enable it: doing so reveals a Payment information section that
  # was not there before, and Confirm stays disabled until a payment name is chosen too.
  # An earlier version of this scenario expected the amount alone to enable it, which said
  # more about the person writing the test than about the form.
  @money
  Scenario Outline: A deposit asks for payment details before it can be confirmed
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the "Deposit" form from the dashboard
    Then the "Deposit" form should be shown
    And the "Confirm" action should be disabled
    When I enter the deposit amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the amount should be accepted
    And the form should ask for "Payment information"
    And the "Confirm" action should be disabled
    And the transaction is deliberately not submitted

    Examples:
      | excelFileName | excelSheetName | row |
      | Deposit_TestData.xlsx | Sheet1 | 1 |
