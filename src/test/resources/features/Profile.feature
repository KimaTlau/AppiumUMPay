Feature: Profile panel

  As a UMPay account holder on the phone
  I want everything about my own account reachable from one place
  So that my records, my wallets and my settings are where I left them

  # WHAT THIS FILE COVERS
  #
  # The profile panel behind the avatar in the top left, and the eleven destinations it offers.
  #
  # WHY IT LOOKS LIKE THE WEB SUITE
  #
  # It is the same product. The web project's profile drawer offers Trade Record, User List,
  # Commission Listing, Fee Listing, Wallets, Payment, Templates, Transfer Fee Setting, Security,
  # Document verification and Language, and so does this panel - with the same account showing the
  # same referral code, the same saved payout accounts and the same eleven currencies on the fee
  # listing. So the web's entry case for each of those modules replicates here almost word for
  # word, and this file is that replication.
  #
  # WHAT IS NOT HERE YET, AND WHY
  #
  # The web suite asserts on what is inside each of those screens - that every wallet names its
  # currency and what is blocked, that no two orders share a number, that every commission carries
  # a reference. None of that is here. Each of those needs a page object of its own and a run on
  # the device to prove it, and a case that claimed to check the contents while only checking the
  # title would be worse than no case at all. This file establishes that every destination is
  # reachable; the contents are the next tranche.
  #
  # NOTHING HERE CHANGES ANYTHING. Every scenario reads a screen and goes back. Signing out is
  # covered in UMPayLogin.feature and deliberately not repeated here, because after it the session
  # is gone and the scenarios below would have nothing to open.

  @profile @Profile_TC_001
  Scenario: The profile panel offers everything the account holder can reach
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    Then the profile panel should offer "Trade Record, User List, Commission Listing, Fee Listing, Wallets, Payment, Templates, Transfer Fee Setting, Security, Document verification, Language"

  # The same code the web drawer shows for this account. A code that differed between the two
  # would mean one of them is showing somebody else's.
  @profile @Profile_TC_002
  Scenario: The profile panel shows the account's referral code
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    Then the profile panel should show a referral code

  # One row per destination, as the web suite does it. Each is its own example rather than one
  # scenario walking all eleven: when a destination stops opening, the run should name which.
  @profile @Profile_TC_003
  Scenario Outline: Each destination in the profile panel opens its own screen
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    When I open the profile panel
    And I open "<destination>" from the profile panel
    Then the "<destination>" screen should open
    When I go back from the destination
    Then the profile panel should offer "<destination>"

    Examples: everything the panel leads to
      | destination           |
      | Trade Record          |
      | User List             |
      | Commission Listing    |
      | Fee Listing           |
      | Wallets               |
      | Payment               |
      | Templates             |
      | Transfer Fee Setting  |
      | Security              |
      | Document verification |
