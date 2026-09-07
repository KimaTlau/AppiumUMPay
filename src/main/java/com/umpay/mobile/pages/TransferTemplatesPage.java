package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The saved payees behind the "UMPay to Existing template" route.
 *
 * The entries are nicknames and masked account numbers that belong to whichever account is
 * signed in - "Within China 3 / ****  ****  **01  1978" on the test account - so nothing
 * here asserts a particular one. What can be asserted without inventing test data is that
 * the route opens the list and that the list has something in it, which is what a user
 * needs before a transfer is possible at all.
 */
public class TransferTemplatesPage extends BasePage {

	private static final By TITLE = MobileLocators.exactly("Select Template");

	/**
	 * A saved payee row.
	 *
	 * Every row is clickable and carries a description; the heading is not clickable, so
	 * counting clickable descriptions counts the templates and nothing else.
	 */
	private static final By TEMPLATE_ROWS =
			By.xpath("//*[@clickable='true'][@content-desc!='']");

	public TransferTemplatesPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(TITLE, Duration.ofSeconds(20));

	}

	public int templateCount() {

		// Waits for the first row rather than reading straight away: the list is fetched
		// when the screen opens, and an immediate count would be counting an empty list.
		isPresent(TEMPLATE_ROWS, Duration.ofSeconds(15));

		return driver.findElements(TEMPLATE_ROWS).size();

	}
}
