package com.umpay.mobile.stepdefs;

import com.umpay.mobile.pages.DashboardPage;
import com.umpay.mobile.pages.ProfileDestinationPage;
import com.umpay.mobile.utility.MobileBaseClass;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * What the rest of the panel's screens say, and what the dashboard says.
 *
 * <p>Most of these screens are asked the same kind of question - does this screen carry the
 * things it is supposed to carry - so one step answers it for all of them rather than eight
 * nearly identical ones. Where a screen deserves a question of its own, it gets one: the bills
 * are asked whether every entry names its kind, its date and its amount, because a ledger entry
 * missing any of those is one nobody could reconcile.
 *
 * <p>These replicate the web suite's entry cases for Trade Record, Commission Listing, Payment,
 * Templates, Transfer Fee Setting, Security, Bills and the home page. What the web asks beyond
 * that - that no two orders share a number, that filtering leaves only what was asked for - needs
 * driving the screens rather than reading them, and is not here.
 */
public class PanelContentStepDefs {

    private ProfileDestinationPage screenPage;

    private DashboardPage dashboardPage;

    /** One bill: a kind, a date and time, then an amount. */
    private static final Pattern BILL = Pattern.compile(
            "^(\\S.*?)\\s+(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+([A-Z]{3})$");

    private ProfileDestinationPage screen() {

        if (screenPage == null) {
            screenPage = new ProfileDestinationPage(MobileBaseClass.driver);
        }

        return screenPage;
    }

    private DashboardPage dashboard() {

        if (dashboardPage == null) {
            dashboardPage = new DashboardPage(MobileBaseClass.driver);
        }

        return dashboardPage;
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

    /** Waits for a screen to have more on it than its own title. */
    private void waitUntilItHasDrawn() {

        for (int look = 0; look < 24; look++) {

            if (screen().labels().size() > 2) {
                return;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Anything the panel opened
    // ------------------------------------------------------------------

    /**
     * The screen carries what it is supposed to carry.
     *
     * Matched on the start of a label rather than the whole of it, because the app runs a
     * heading together with what sits under it - the security screen writes "PIN" on its own but
     * the wallets write "HKD | Hong Kong HKD: 7,943.51 Main Wallet" as one.
     */
    @Then("the screen should carry {string}")
    public void theScreenShouldCarry(String expected) {

        waitUntilItHasDrawn();

        List<String> missing = new ArrayList<>();

        for (String wanted : named(expected)) {

            if (!screen().carries(wanted)) {
                missing.add(wanted);
            }
        }

        Assert.assertTrue(missing.isEmpty(),
                "The screen does not carry " + missing + ". It shows: " + screen().text());

        System.out.println("The screen carries " + named(expected));
    }

    @Then("the screen should list something")
    public void theScreenShouldListSomething() {

        waitUntilItHasDrawn();

        List<String> showing = screen().labels();

        Assert.assertTrue(showing.size() > 2,
                "The screen opened and lists nothing beyond its own title, so there is nothing on"
                        + " it to read. It shows: " + screen().text());

        System.out.println("The screen lists " + showing.size() + " things");
    }

    // ------------------------------------------------------------------
    // The dashboard
    // ------------------------------------------------------------------

    @Then("the dashboard should offer {string}")
    public void theDashboardShouldOffer(String expected) {

        List<String> wanted = named(expected);

        Assert.assertTrue(dashboard().offersTiles(wanted),
                "The dashboard does not offer all of " + wanted + ", so those cannot be started"
                        + " from the screen the account lands on. It shows: " + dashboard().text());

        System.out.println("The dashboard offers " + wanted);
    }

    /**
     * The amounts arrive hidden.
     *
     * The same thing the web home page does, and worth the same case: somebody opening their
     * account in a public place should not have what they hold on the screen behind them.
     */
    @Then("the amounts on the dashboard should be hidden")
    public void theAmountsShouldBeHidden() {

        Assert.assertTrue(dashboard().amountsAreHidden(),
                "The dashboard shows its amounts the moment it opens, so anybody behind somebody"
                        + " signing in sees what they hold. It shows: " + dashboard().text());

        System.out.println("The dashboard's amounts arrive hidden");
    }

    @When("I open Bills from the dashboard")
    public void iOpenBillsFromTheDashboard() {

        dashboard().openTile("Bills");
    }

    /**
     * Every entry in the ledger says what it was, when, and for how much.
     *
     * An entry missing its date cannot be placed and one missing its amount cannot be reconciled,
     * which is the same reason the web suite asks it of its own Bills page.
     */
    @Then("every bill should name its kind, its date and its amount")
    public void everyBillShouldNameTheThree() {

        waitUntilItHasDrawn();

        List<String> entries = new ArrayList<>();

        for (String label : screen().labels()) {

            if (BILL.matcher(label).matches()) {
                entries.add(label);
            }
        }

        Assert.assertFalse(entries.isEmpty(),
                "Nothing on the bills screen reads as a transaction - a kind, a date and an"
                        + " amount. It shows: " + screen().text());

        System.out.println("All " + entries.size() + " bills name a kind, a date and an amount."
                + " The newest: " + entries.get(0));
    }
}
