package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Whatever screen the profile panel opened.
 *
 * The web suite has a page object for each of these - the wallets, the trade record, the fee
 * listing and the rest - because each one is asserted on in detail. Here the question is
 * narrower and the same for all of them: did the entry open a screen of its own, and does that
 * screen say what it is? So one page object answers it for every destination rather than eleven
 * that would each hold a single method.
 *
 * The app labels its controls with content-desc rather than text, which is why nothing here
 * reads getText: a sweep of text on these screens finds nothing at all.
 */
public class ProfileDestinationPage extends BasePage {

	/** How long a screen gets to draw itself before it counts as not having opened. */
	private static final Duration SETTLE = Duration.ofSeconds(15);

	public ProfileDestinationPage(AndroidDriver driver) {
		super(driver);
	}

	/**
	 * True once a screen calling itself {@code named} is on the device.
	 *
	 * Matched on the start of the label rather than the whole of it: several of these screens
	 * write their title together with what is underneath it, so the wallets arrive as
	 * "Wallets" but the document verification as "Document verification Verified".
	 */
	public boolean isShowing(String named) {

		return isPresent(MobileLocators.labelled(named), SETTLE);
	}

	/** Every label the screen is showing, each on one line. */
	public List<String> labels() {

		List<String> said = new ArrayList<>();

		try {
			for (WebElement label : driver.findElements(MobileLocators.labelled(""))) {

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

	/** Everything the screen says, on one line, for a message that has to name what was seen. */
	public String text() {

		return String.join(" | ", labels());
	}

	/** True while the screen carries a label starting with {@code said}. */
	public boolean carries(String said) {

		for (String label : labels()) {

			if (label.startsWith(said)) {
				return true;
			}
		}

		return false;
	}

	/** Back to whatever opened this screen. */
	public void goBack() {

		pressBack();
	}
}
