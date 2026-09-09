Feature: Wallets, the user list and the fee listing

  As a UMPay account holder on the phone
  I want the screens behind the profile panel to say what they should
  So that what I hold, who I have brought and what I am charged are all readable

  # WHAT THIS FILE COVERS
  #
  # Three of the screens the profile panel opens, held to the same things the web suite holds its
  # own versions to. Profile.feature proves each screen opens; this one asks what each screen
  # says.
  #
  # WHY THESE THREE FIRST
  #
  # Their contents are readable and steady. The wallets name a currency and a balance, the user
  # list an account number and a status, the fee listing a set of currency codes - none of which
  # moves between runs the way a trade record or a commission listing does. The rest of the
  # panel's screens are worth the same treatment and are not here yet.
  #
  # WHAT IS HELD IS A DEDUCTION
  #
  # The wallets screen writes what is held against the account as "Blocked Amount: HK$-10,684.58",
  # and so does the web. That minus sign is the platform's own convention rather than a fault:
  # money set aside is shown as taken off what the wallet has rather than as a quantity beside it.
  # The case below holds the screen to the convention, so what would fail is a screen that broke
  # it - the same figure shown two ways being the thing nobody could make sense of.
  #
  # NOTHING HERE CHANGES ANYTHING. Every scenario reads a screen and leaves it alone.

  @wallets @Wallets_TC_001
  Scenario: Every wallet names its currency and what it holds
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Wallets" from the profile panel
    Then the "Wallets" screen should open
    And every wallet should name its currency and what it holds

  # The main wallet is what the money forms open on, so anything but one leaves them starting
  # somewhere nobody chose.
  @wallets @Wallets_TC_002
  Scenario: Exactly one wallet is the main wallet
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Wallets" from the profile panel
    Then the "Wallets" screen should open
    And exactly one wallet should be the main wallet

  @wallets @Wallets_TC_003
  Scenario: What is held against the account is written as a deduction
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Wallets" from the profile panel
    Then the "Wallets" screen should open
    And what is held against the account should be written as a deduction

  # A user listed without the number the platform knows them by cannot be asked about, and one
  # without a status cannot be told from an account that has been suspended.
  @userlist @User_List_TC_001
  Scenario: Every user is listed with a number, a name and a status
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "User List" from the profile panel
    Then the "User List" screen should open
    And every user should be listed with a number, a name and a status

  # The account number is what somebody quotes to support, so it has to be the number the platform
  # actually issues rather than a truncation.
  @userlist @User_List_TC_002
  Scenario: Every user is listed under a proper account number
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "User List" from the profile panel
    Then the "User List" screen should open
    And every account number should read as one the platform issues

  # The same person listed twice would have their commission counted twice by anybody reading it.
  @userlist @User_List_TC_003
  Scenario: Nobody is listed twice
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "User List" from the profile panel
    Then the "User List" screen should open
    And nobody should be listed twice

  # A status the screen invents is worse than none: somebody would act on a word nobody defined.
  @userlist @User_List_TC_004
  Scenario: Every status is one the platform uses
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "User List" from the profile panel
    Then the "User List" screen should open
    And every status should be one of "Unlock, Lock, Locked"

  # The same eleven the web fee listing offers. A currency missing from one and not the other
  # would mean the two products disagree about what the platform deals in.
  @feelisting @Fee_Listing_TC_001
  Scenario: Every currency the platform deals in can be asked about
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Fee Listing" from the profile panel
    Then the "Fee Listing" screen should open
    And the fee listing should offer "BDT, BRL, HKD, IDR, MXN, MYR, PHP"

  @feelisting @Fee_Listing_TC_002
  Scenario: Each currency is offered exactly once
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "Fee Listing" from the profile panel
    Then the "Fee Listing" screen should open
    And each currency should be offered exactly once
