package com.umpay.mobile.stepdefs;

import com.umpay.mobile.pages.DashboardPage;
import com.umpay.mobile.pages.ProfileDestinationPage;
import com.umpay.mobile.pages.ProfilePage;
import com.umpay.mobile.utility.MobileBaseClass;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * The profile panel, and everything it opens.
 *
 * <p>The web suite gives each of these destinations a module of its own - the wallets, the trade
 * record, the fee listing and the rest. The app offers the same set, so these scenarios replicate
 * the web's entry cases for each: that the panel offers them, and that each one opens a screen of
 * its own. Asserting on what is inside each screen is the next tranche rather than this one, and
 * a case that claimed to check the contents while only checking the title would be worse than
 * none.
 */
public class ProfileStepDefs {

    private ProfilePage profilePage;

    private ProfileDestinationPage destinationPage;

    private DashboardPage dashboardPage;

    /** What the panel offered, read before the run moved off it. */
    private List<String> offered = new ArrayList<>();

    private ProfilePage profile() {

        if (profilePage == null) {
            profilePage = new ProfilePage(MobileBaseClass.driver);
        }

        return profilePage;
    }

    private ProfileDestinationPage destination() {

        if (destinationPage == null) {
            destinationPage = new ProfileDestinationPage(MobileBaseClass.driver);
        }

        return destinationPage;
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

    /** How many times opening the panel is worth trying, as the web suite does it. */
    private static final int ATTEMPTS = 3;

    /**
     * Opens the profile panel.
     *
     * Tried more than once on purpose. The avatar has no label of its own, so the app is opened
     * by tapping where it sits, and a tap at a coordinate is the least reliable thing this suite
     * does - two of eleven runs landed on nothing and reported a panel that had simply never been
     * asked to open. Retrying tells that apart from a panel that will not open at all.
     */
    @When("I open the profile panel")
    public void iOpenTheProfilePanel() throws InterruptedException {

        for (int attempt = 1; attempt <= ATTEMPTS && !profile().isShowing(); attempt++) {

            dashboard().openProfile();

            if (profile().isShowing()) {
                return;
            }

            System.out.println("The profile panel did not open on attempt " + attempt + " of "
                    + ATTEMPTS + "; going back to the dashboard and trying again");

            dashboard().pressBackFromAnywhere();

            Thread.sleep(2000);
        }

        Assert.assertTrue(profile().isShowing(),
                "The profile panel did not open in " + ATTEMPTS + " attempts. The screen shows: "
                        + destination().text());
    }

    @Then("the profile panel should offer {string}")
    public void theProfilePanelShouldOffer(String expected) {

        offered = profile().destinationsOffered();

        List<String> missing = new ArrayList<>();

        for (String wanted : named(expected)) {

            if (!offered.contains(wanted)) {
                missing.add(wanted);
            }
        }

        Assert.assertTrue(missing.isEmpty(),
                "The profile panel does not offer " + missing + ", so those cannot be reached from"
                        + " here at all. It offers: " + offered);

        System.out.println("The profile panel offers " + offered);
    }

    @When("I open {string} from the profile panel")
    public void iOpenFromTheProfilePanel(String destinationName) {

        profile().open(destinationName);
    }

    @Then("the {string} screen should open")
    public void theScreenShouldOpen(String named) {

        Assert.assertTrue(destination().isShowing(named),
                "Opening it did not reach a screen calling itself \"" + named + "\". The screen"
                        + " shows: " + destination().text());

        System.out.println("The " + named + " screen opened, showing: "
                + destination().text().substring(0,
                        Math.min(160, destination().text().length())));
    }

    @When("I go back from the destination")
    public void iGoBackFromTheDestination() {

        destination().goBack();
    }

    /**
     * The referral code the panel shows.
     *
     * The same code the web drawer shows for this account, which is worth checking on both: a
     * code that differed between the two would mean one of them is showing somebody else's.
     */
    @Then("the profile panel should show a referral code")
    public void theProfilePanelShouldShowAReferralCode() {

        String code = profile().referralCode();

        Assert.assertFalse(code.isEmpty(),
                "The profile panel shows no referral code, so there is nothing this account could"
                        + " pass on. It shows: " + String.join(" | ", profile().everythingOnThePanel()));

        Assert.assertTrue(code.matches("[A-Z0-9]{4,10}"),
                "The referral code reads \"" + code + "\", which is not the shape of a code the"
                        + " platform issues");

        System.out.println("The profile panel shows the referral code " + code);
    }
}
