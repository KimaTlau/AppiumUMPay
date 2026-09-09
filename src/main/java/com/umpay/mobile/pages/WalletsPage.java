package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The wallets this account holds, reached from the profile panel.
 *
 * The same screen the web suite covers at /v2/wallet, showing the same figures: a total in Hong
 * Kong dollars, what is held against the account, and a card per wallet naming its currency, the
 * country it belongs to and what is in it.
 *
 * WHAT IS HELD IS WRITTEN AS A DEDUCTION - "Blocked Amount: HK$-10,684.58". That is the
 * platform's own convention rather than a fault, and it is the same on the web. Money set aside
 * is shown as taken off what the wallet has rather than as a quantity beside it.
 *
 * The app labels its cards with content-desc rather than text, so a wallet arrives as one label
 * with its lines run together: "HKD | Hong Kong HKD: 7,943.51 Main Wallet".
 */
public class WalletsPage extends BasePage {

	/** What the screen calls itself. */
	private static final String TITLE = "Wallets";

	/** One wallet card: its currency code, then everything else it says. */
	private static final Pattern WALLET = Pattern.compile("^([A-Z]{3})\\b(.*)$");

	/** What a card says is in it - "HKD: 7,943.51". */
	private static final Pattern HOLDS = Pattern.compile("([A-Z]{3}):\\s*([0-9,.]+)");

	/** What the screen says is held against the account as a whole. */
	private static final Pattern BLOCKED = Pattern.compile("Blocked Amount:\\s*(\\S+)");

	public WalletsPage(AndroidDriver driver) {
		super(driver);
	}

	/** True once the wallets are on the device. */
	public boolean isShowing() {

		return isPresent(MobileLocators.labelled(TITLE), java.time.Duration.ofSeconds(15));
	}

	/** Every label the screen is showing, each on one line. */
	public List<String> labels() {

		List<String> said = new ArrayList<>();

		try {
			for (org.openqa.selenium.WebElement label
					: driver.findElements(MobileLocators.labelled(""))) {

				String description = label.getAttribute("content-desc");

				if (description != null && !description.isBlank()) {

					String cleaned = description.replaceAll("\\s+", " ").trim();

					if (!said.contains(cleaned)) {
						said.add(cleaned);
					}
				}
			}
		} catch (Exception theScreenMoved) {
			// A screen that redrew from under us is not one to report on.
		}

		return said;
	}

	/**
	 * Every wallet card, by the currency it is for.
	 *
	 * Only the cards: the screen's own furniture - its title, the total, the held line - carries
	 * no three letter code at the front, which is what tells a card from the rest.
	 */
	public Map<String, String> wallets() {

		Map<String, String> found = new LinkedHashMap<>();

		for (String label : labels()) {

			if (label.startsWith("Total") || label.startsWith("Blocked")) {
				continue;
			}

			Matcher card = WALLET.matcher(label);

			if (card.find() && HOLDS.matcher(label).find()) {
				found.putIfAbsent(card.group(1), label);
			}
		}

		return found;
	}

	/** What each wallet says is in it, by currency. */
	public Map<String, String> balances() {

		Map<String, String> held = new LinkedHashMap<>();

		for (Map.Entry<String, String> wallet : wallets().entrySet()) {

			Matcher amount = HOLDS.matcher(wallet.getValue());

			if (amount.find()) {
				held.put(wallet.getKey(), amount.group(2));
			}
		}

		return held;
	}

	/** How many wallets claim to be the main one. */
	public int mainWalletCount() {

		int claiming = 0;

		for (String card : wallets().values()) {

			if (card.contains("Main Wallet")) {
				claiming++;
			}
		}

		return claiming;
	}

	/** The currency of whichever wallet is the main one, or "" while none says so. */
	public String mainWallet() {

		for (Map.Entry<String, String> wallet : wallets().entrySet()) {

			if (wallet.getValue().contains("Main Wallet")) {
				return wallet.getKey();
			}
		}

		return "";
	}

	/**
	 * What the screen says is held against the account, or "" while it says nothing.
	 *
	 * Waited for rather than read once. The wallet cards arrive before the header that carries
	 * the total and the held line, so a reading taken the moment the screen appears finds the
	 * wallets and none of the figures above them - which reads as a screen that says nothing
	 * about what is held rather than one that has not finished drawing.
	 */
	public String blockedShown() {

		waitForTheHeldLine();

		for (String label : labels()) {

			Matcher held = BLOCKED.matcher(label);

			if (held.find()) {
				return held.group(1);
			}
		}

		return "";
	}

	/** Waits for the line naming what is held to arrive, giving up rather than hanging. */
	private void waitForTheHeldLine() {

		for (int look = 0; look < 20; look++) {

			for (String label : labels()) {

				if (BLOCKED.matcher(label).find()) {
					return;
				}
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/** Everything the screen says, on one line, for a message that has to name what was seen. */
	public String text() {

		return String.join(" | ", labels());
	}
}
