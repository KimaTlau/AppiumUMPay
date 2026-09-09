package com.umpay.mobile.stepdefs;

import com.umpay.mobile.pages.FeeListingPage;
import com.umpay.mobile.pages.UserListPage;
import com.umpay.mobile.pages.WalletsPage;
import com.umpay.mobile.utility.MobileBaseClass;

import io.cucumber.java.en.Then;

import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the wallets, the user list and the fee listing actually say.
 *
 * <p>The profile scenarios prove each of these screens opens. These ask the same questions of
 * them the web suite asks of its own versions: that every wallet names what it holds, that
 * exactly one is the main one, that everyone listed carries a number the platform would issue,
 * that no currency is offered twice.
 *
 * <p>The pages are built lazily, as the other mobile step classes do it: Cucumber creates the
 * step class before the device session exists, so a page built in a constructor would capture a
 * driver that is not there yet.
 */
public class ProfileContentStepDefs {

    private WalletsPage walletsPage;

    private UserListPage userListPage;

    private FeeListingPage feeListingPage;

    private WalletsPage wallets() {

        if (walletsPage == null) {
            walletsPage = new WalletsPage(MobileBaseClass.driver);
        }

        return walletsPage;
    }

    private UserListPage users() {

        if (userListPage == null) {
            userListPage = new UserListPage(MobileBaseClass.driver);
        }

        return userListPage;
    }

    private FeeListingPage fees() {

        if (feeListingPage == null) {
            feeListingPage = new FeeListingPage(MobileBaseClass.driver);
        }

        return feeListingPage;
    }

    /** "a, b, c" as the feature writes it, into the things it names. */
    private List<String> named(String commaSeparated) {

        List<String> each = new ArrayList<>();

        for (String one : commaSeparated.split(",")) {

            if (!one.trim().isEmpty()) {
                each.add(one.trim());
            }
        }

        return each;
    }

    // ------------------------------------------------------------------
    // The wallets
    // ------------------------------------------------------------------

    @Then("every wallet should name its currency and what it holds")
    public void everyWalletShouldNameWhatItHolds() {

        Map<String, String> balances = wallets().balances();

        Assert.assertFalse(balances.isEmpty(),
                "The wallets screen lists nothing at all, so an account that holds money cannot"
                        + " see any of it. It shows: " + wallets().text());

        List<String> incomplete = new ArrayList<>();

        for (Map.Entry<String, String> wallet : balances.entrySet()) {

            if (wallet.getValue() == null || wallet.getValue().isBlank()) {
                incomplete.add(wallet.getKey() + " states no balance");
            }
        }

        Assert.assertTrue(incomplete.isEmpty(),
                "These wallets are missing part of what they should say: " + incomplete);

        System.out.println("All " + balances.size() + " wallets name what they hold: " + balances);
    }

    /**
     * Exactly one wallet is the main one.
     *
     * The main wallet is what the money forms open on, so anything but one leaves them starting
     * somewhere nobody chose - the same reason the web suite asks it.
     */
    @Then("exactly one wallet should be the main wallet")
    public void exactlyOneWalletShouldBeMain() {

        int claiming = wallets().mainWalletCount();

        Assert.assertEquals(claiming, 1,
                "The wallets screen shows " + claiming + " wallets claiming to be the main one."
                        + " It shows: " + wallets().text());

        System.out.println("The main wallet is " + wallets().mainWallet());
    }

    /**
     * What is held is written as a deduction.
     *
     * The minus sign is the platform's own convention, on the phone as on the web: money set
     * aside is shown as taken off what the wallet has rather than as a quantity beside it. What
     * would be worth failing for is the screen breaking that convention, not keeping it.
     */
    @Then("what is held against the account should be written as a deduction")
    public void whatIsHeldShouldBeADeduction() {

        String held = wallets().blockedShown();

        Assert.assertFalse(held.isEmpty(),
                "The wallets screen says nothing about what is held against the account. It shows: "
                        + wallets().text());

        Assert.assertTrue(held.contains("-") || held.replaceAll("[^0-9.]", "").matches("0*\\.?0*"),
                "What is held reads \"" + held + "\", which is neither a deduction nor nothing at"
                        + " all - and every other screen in both suites writes it as a deduction");

        System.out.println("What is held against the account reads " + held);
    }

    // ------------------------------------------------------------------
    // The user list
    // ------------------------------------------------------------------

    @Then("every user should be listed with a number, a name and a status")
    public void everyUserShouldCarryTheThree() {

        users().waitUntilSomebodyIsListed();

        Map<String, String> names = users().namesListed();
        Map<String, String> states = users().statesListed();

        Assert.assertFalse(names.isEmpty(),
                "The user list shows nobody at all. It shows: " + users().text());

        List<String> incomplete = new ArrayList<>();

        for (Map.Entry<String, String> user : names.entrySet()) {

            if (user.getValue().isBlank()) {
                incomplete.add(user.getKey() + " is listed with no name");
            }

            if (!states.containsKey(user.getKey()) || states.get(user.getKey()).isBlank()) {
                incomplete.add(user.getKey() + " is listed with no status");
            }
        }

        Assert.assertTrue(incomplete.isEmpty(),
                "These are missing part of what they should say: " + incomplete);

        System.out.println("All " + names.size() + " carry a number, a name and a status: " + names);
    }

    @Then("every account number should read as one the platform issues")
    public void everyNumberShouldReadRight() {

        users().waitUntilSomebodyIsListed();

        List<String> numbers = users().accountNumbersListed();

        Assert.assertFalse(numbers.isEmpty(), "There is nobody listed to read");

        List<String> wrong = new ArrayList<>();

        for (String number : numbers) {

            if (!users().readsAsAnAccountNumber(number)) {
                wrong.add(number);
            }
        }

        Assert.assertTrue(wrong.isEmpty(),
                "These do not read as account numbers the platform issues, so quoting one to"
                        + " support would get nowhere: " + wrong);

        System.out.println("All " + numbers.size() + " account numbers read as the platform issues"
                + " them: " + numbers);
    }

    @Then("nobody should be listed twice")
    public void nobodyShouldBeListedTwice() {

        users().waitUntilSomebodyIsListed();

        List<String> numbers = users().accountNumbersListed();

        Assert.assertFalse(numbers.isEmpty(), "There is nobody listed to read");

        List<String> twice = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (String number : numbers) {

            if (seen.contains(number) && !twice.contains(number)) {
                twice.add(number);
            }

            seen.add(number);
        }

        Assert.assertTrue(twice.isEmpty(),
                "These are listed more than once, and anybody reading this screen would count"
                        + " their commission twice: " + twice);

        System.out.println("All " + numbers.size() + " are listed once each");
    }

    @Then("every status should be one of {string}")
    public void everyStatusShouldBeKnown(String expected) {

        users().waitUntilSomebodyIsListed();

        Map<String, String> states = users().statesListed();

        Assert.assertFalse(states.isEmpty(), "There is nobody listed to read");

        List<String> known = named(expected);
        List<String> strange = new ArrayList<>();

        for (Map.Entry<String, String> user : states.entrySet()) {

            boolean recognised = false;

            for (String state : known) {

                if (state.equalsIgnoreCase(user.getValue())) {
                    recognised = true;
                }
            }

            if (!recognised) {
                strange.add(user.getKey() + " is \"" + user.getValue() + "\"");
            }
        }

        Assert.assertTrue(strange.isEmpty(),
                "These are in a state this screen has never been said to use, so nobody reading it"
                        + " would know what it means: " + strange);

        System.out.println("Everybody listed is in a state the platform uses: " + states.values());
    }

    // ------------------------------------------------------------------
    // The fee listing
    // ------------------------------------------------------------------

    @Then("the fee listing should offer {string}")
    public void theFeeListingShouldOffer(String expected) {

        fees().waitUntilTheCurrenciesArrive();

        List<String> offered = fees().currenciesOffered();

        List<String> missing = new ArrayList<>();

        for (String wanted : named(expected)) {

            if (!offered.contains(wanted)) {
                missing.add(wanted);
            }
        }

        Assert.assertTrue(missing.isEmpty(),
                "The fee listing does not offer " + missing + ", so what it costs to move money in"
                        + " those cannot be asked about. It offers: " + offered);

        System.out.println("The fee listing offers " + offered);
    }

    @Then("each currency should be offered exactly once")
    public void eachCurrencyOnce() {

        fees().waitUntilTheCurrenciesArrive();

        List<String> offered = fees().currenciesOffered();

        Assert.assertFalse(offered.isEmpty(), "The fee listing offers no currencies at all");

        List<String> twice = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (String currency : offered) {

            if (seen.contains(currency) && !twice.contains(currency)) {
                twice.add(currency);
            }

            seen.add(currency);
        }

        Assert.assertTrue(twice.isEmpty(),
                "These currencies are listed more than once, and two entries for one currency"
                        + " could answer differently: " + twice);

        System.out.println("All " + offered.size() + " currencies are offered once each");
    }
}
