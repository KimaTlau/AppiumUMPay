Feature: UMPay mobile transfers

  As a UMPay user on Android
  I want to reach every way the app can move money out of my wallet
  So that each transfer route is known to open, validate and price correctly

  # WHAT THIS FILE COVERS, AND WHAT GlobalTransfer.feature COVERS
  #
  # The split follows the web suite's. GlobalTransfer.feature drives the UnionPay route in
  # depth - the limits it states, the pricing it works out, and the amount it refuses. This
  # file covers the breadth instead: every route the hub offers, and the state each form is
  # in before anything is sent. Nothing here repeats what that file already does.
  #
  # TRANSFER IS A MODULE, NOT A FORM
  #
  # The dashboard's Transfer tile opens a hub of six destinations, and each leads somewhere
  # different. There is no amount box on the hub itself:
  #
  #   UMPay to Existing template  a list of saved payees
  #   UMPay to UMPay Wallet       wallet to wallet by UUID, with a fee payer choice
  #   UnionPay China              amount in USD, converted and quoted into CNY
  #   UnionPay Global             the same form without a receive currency
  #
  # The two UnionPay routes are listed because the hub offers them and this file asserts
  # that it does; the forms behind them are covered by GlobalTransfer.feature.
  #   Transfer to AliPay          under maintenance
  #   Transfer to WeChat          under maintenance
  #
  # NOTHING HERE IS SENT
  #
  # Every scenario stops on a form and never presses Transfer. Sending moves real money on
  # the test environment and no test can undo it. As with the other money flows this is
  # enforced rather than trusted: no step and no page object method taps the action, so a
  # later scenario cannot send one by accident either.
  #
  # THE TWO ROUTES UNDER MAINTENANCE ARE TESTED, NOT SKIPPED
  #
  # AliPay and WeChat answer a tap with "The service is currently unavailable. Please try
  # again later." That is a real product state, and asserting it means this suite is what
  # notices when the routes come back rather than somebody finding out by accident.
  #
  # THE WALLET FORM'S AMOUNT BOX CANNOT BE AUTOMATED
  #
  # Every part of the wallet form is reachable except the amount. The box under "Transfer
  # Amount" is a View with no description and no text - there is no accessibility node to
  # type into, and tapping where it sits produces no input field. The UnionPay forms expose
  # theirs as an ordinary EditText, so this is specific to that one screen.
  #
  # So the wallet scenarios cover the parts that can be driven, and the validation-becomes-
  # enabled behaviour is covered on UnionPay in GlobalTransfer.feature, where the amount can
  # actually be entered.
  # This is a testability defect worth raising: one Semantics wrapper on that field would
  # make the whole route automatable.

  # The credentials come out of Login_TestData like the web suite's Transfer.feature, which
  # signs in from the same workbook. Nothing else here is data driven: the routes, labels and
  # counters these scenarios assert on are the application's own wording, and belong in the
  # scenario where a reader can see them rather than in a spreadsheet.
  Background:
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the transfer hub

  @transfer @Transfer_TC_001
  Scenario: The transfer hub offers every destination
    Then the transfer hub should offer the route "UMPay to Existing template"
    And the transfer hub should offer the route "UMPay to UMPay Wallet"
    And the transfer hub should offer the route "UnionPay China"
    And the transfer hub should offer the route "UnionPay Global"
    And the transfer hub should offer the route "Transfer to AliPay"
    And the transfer hub should offer the route "Transfer to WeChat"

  # Both payment app routes are listed with "Maintenance" in their own label and refuse to
  # open. The scenario asserts the refusal rather than avoiding the route, so the day the
  # service returns this fails and says so.
  @transfer @Transfer_TC_002
  Scenario Outline: A payment app route under maintenance refuses to open
    When I take the "<route>" route
    Then the app should say the service is unavailable

    Examples:
      | route              |
      | Transfer to AliPay |
      | Transfer to WeChat |

  # The saved payees belong to whichever account is signed in, so nothing here asserts a
  # particular one - only that the route opens the list and the list has something in it,
  # which is what a user needs before a template transfer is possible at all.
  @transfer @Transfer_TC_003
  Scenario: The saved template route opens the list of payees
    When I take the "UMPay to Existing template" route
    Then the saved template list should be shown
    And the template list should hold at least one saved payee

  @transfer @Transfer_TC_004
  Scenario: The wallet transfer form asks for everything a wallet transfer needs
    When I take the "UMPay to UMPay Wallet" route
    Then the wallet transfer form should be shown
    And the wallet form should show the sending wallet and its balance
    And the wallet form should offer a destination wallet field
    And the wallet form should ask for "Transfer Amount"
    And the wallet form should ask for "Minimum: 100.0"
    And the wallet form should ask for "Select who will pay the fee"
    And the wallet form should ask for "Create template"
    And the wallet form should ask for "Remark"

  # The button is on screen from the moment the form opens but starts unclickable. Asserting
  # that is a real test of the form's state, where asserting the button merely exists would
  # pass on a completely empty one.
  @transfer @Transfer_TC_005
  Scenario: A wallet transfer cannot be sent from an empty form
    When I take the "UMPay to UMPay Wallet" route
    Then the wallet transfer form should be shown
    And the wallet transfer should not be sendable yet
    And the transfer is deliberately not sent

  @transfer @Transfer_TC_006
  Scenario: Either party can be chosen to pay the wallet transfer fee
    When I take the "UMPay to UMPay Wallet" route
    Then the wallet transfer form should be shown
    And the wallet form should offer both parties as the fee payer
    When I choose "Fee will be paid by me" to pay the fee
    Then the fee payer choice "Fee will be paid by other party" should still be offered
    When I choose "Fee will be paid by other party" to pay the fee
    Then the fee payer choice "Fee will be paid by me" should still be offered

  # The remark box is the one field on this form that can be typed into, and its counter is
  # a real piece of behaviour: 150 characters, counting down as they are used.
  @transfer @Transfer_TC_007
  Scenario: The wallet remark box counts the characters left
    When I take the "UMPay to UMPay Wallet" route
    Then the wallet transfer form should be shown
    And the remark box should report "150 characters remaining"
    When I write "Rent" in the wallet remark box
    Then the remark box should report "146 characters remaining"
