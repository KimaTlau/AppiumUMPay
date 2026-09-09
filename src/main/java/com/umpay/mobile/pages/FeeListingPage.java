package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What UMPay charges, reached from the profile panel.
 *
 * The same screen the web suite covers at /customer/fee, offering the same currencies the
 * platform deals in. Each arrives as its code and the country it belongs to run together -
 * "BDT Bangladesh", "BRL Brazilian Real", "HKD Hong Kong".
 *
 * Nothing here changes anything. A fee schedule is the platform's to set; this screen shows it.
 */
public class FeeListingPage extends BasePage {

	private static final String TITLE = "Fee Listing";

	/** One currency on the list: its three letter code, then whose it is. */
	private static final Pattern CURRENCY = Pattern.compile("^([A-Z]{3,4})\\s+\\S");

	public FeeListingPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(MobileLocators.labelled(TITLE), Duration.ofSeconds(15));
	}

	/**
	 * Waits for the currencies to arrive.
	 *
	 * The screen draws its title before the list, so a reading taken as it opens finds no
	 * currencies at all - which reads as a fee listing that offers nothing rather than one that
	 * has not finished loading.
	 */
	public void waitUntilTheCurrenciesArrive() {

		for (int look = 0; look < 20; look++) {

			if (currenciesOffered().size() >= 2) {
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
	 * Every currency the fees can be asked about, in the order the screen lists them.
	 *
	 * Repeats are kept rather than dropped: whether a currency is offered twice is one of the
	 * things the web suite asks, and a list that quietly removed the duplicate could not answer
	 * it.
	 */
	public List<String> currenciesOffered() {

		List<String> currencies = new ArrayList<>();

		for (String label : labels()) {

			Matcher currency = CURRENCY.matcher(label);

			if (currency.find()) {
				currencies.add(currency.group(1));
			}
		}

		return currencies;
	}

	/** Everything the screen says, on one line. */
	public String text() {

		return String.join(" | ", labels());
	}
}
