Feature: Home

  As a UMPay account holder on the phone
  I want the screen I land on to show what I hold and where I can go
  So that I can see the state of my account and start whatever I came to do

  # The screen every account lands on. The web suite covers the same page and asks the same two
  # things of it: that it offers every flow a transaction can be started from, and that the
  # amounts arrive hidden.
  #
  # The dashboard offers more than the four money tiles the rest of this suite drives. Transfer
  # to Mainland China, Global Transfer, International School Fee and Bills are all here too, and
  # each is a module of its own on the web.

  @home @Home_TC_001
  Scenario: The dashboard offers every flow the account can start
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    Then the dashboard should offer "Deposit, Withdrawal, Transfer, Convert, Bills, International School Fee"

  # Somebody opening their account in a public place should not have what they hold on the screen
  # behind them. The web home page hides its amounts the same way.
  @home @Home_TC_002
  Scenario: The amounts on the dashboard arrive hidden
    Given I log into the UMPay application with valid credentials using "1" of "Sheet1" of "Login_TestData.xlsx"
    Then the amounts on the dashboard should be hidden
