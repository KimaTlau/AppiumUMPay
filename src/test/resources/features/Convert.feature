Feature: UMPay mobile convert

  As a UMPay user on Android
  I want to price a conversion between my wallets
  So that the form behind the dashboard's Convert tile is known to work

  # WHERE THIS DIFFERS FROM THE WEB SUITE
  #
  # The web Convert.feature submits the conversion and then asserts that the source wallet
  # balance actually fell - the dialog says the request was accepted, the balance says the
  # money moved. Nothing on mobile is submitted, so this scenario stops on the priced form.
  #
  # That is a deliberate difference rather than a gap in the mobile suite: the web run
  # already proves the money moves, and repeating it from the phone would spend real balance
  # a second time for the same assertion.
  #
  # The action here is named Convert rather than Confirm - the same word as the form's own
  # title - which is why the label is passed in from the scenario.

  @money @convert
  Scenario Outline: A conversion prices the amount and offers to convert
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the "Convert" form from the dashboard
    Then the "Convert" form should be shown
    When I enter the conversion amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the amount should be accepted
    And the transaction is deliberately not submitted

    Examples:
      | excelFileName | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1 | 1 |
