package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The people this account has brought to UMPay, reached from the profile panel.
 *
 * The same list the web suite covers at /customer, showing the same people: each is listed by the
 * account number the platform knows them by, the name they go under - or N/A where they have not
 * set one - and whether their account is locked.
 *
 * The app writes a whole entry into one label, lines and all, so a person arrives as
 * "Kima_Test123 User uuid 000000770850 Status Unlock". The web draws the same three things in
 * separate boxes, which is why the two suites read them differently and assert the same things.
 */
public class UserListPage extends BasePage {

	private static final String TITLE = "User List";

	/** One person, as the app writes them: a name, then the number, then the status. */
	private static final Pattern PERSON = Pattern.compile(
			"^(.*?)\\s*User uuid\\s*(\\S+)\\s*Status\\s*(\\S+)");

	/** What an account number looks like: twelve figures. */
	private static final String ACCOUNT_NUMBER = "\\d{12}";

	public UserListPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(MobileLocators.labelled(TITLE), Duration.ofSeconds(15));
	}

	/**
	 * Waits for at least one person to be listed.
	 *
	 * The screen draws its title before the people arrive, so a reading taken as it opens finds
	 * an empty list - which reads as an account that has brought nobody rather than one whose
	 * list has not loaded.
	 */
	public void waitUntilSomebodyIsListed() {

		for (int look = 0; look < 20; look++) {

			if (!accountNumbersListed().isEmpty()) {
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

	/** Everyone listed, by the account number they are known by and the name they go under. */
	public Map<String, String> namesListed() {

		Map<String, String> people = new LinkedHashMap<>();

		for (String label : labels()) {

			Matcher person = PERSON.matcher(label);

			if (person.find()) {
				people.put(person.group(2).trim(), person.group(1).trim());
			}
		}

		return people;
	}

	/** Everyone listed, by the account number they are known by and whether they are locked. */
	public Map<String, String> statesListed() {

		Map<String, String> states = new LinkedHashMap<>();

		for (String label : labels()) {

			Matcher person = PERSON.matcher(label);

			if (person.find()) {
				states.put(person.group(2).trim(), person.group(3).trim());
			}
		}

		return states;
	}

	/** Everyone listed, by the account number they are known by, in the order listed. */
	public List<String> accountNumbersListed() {

		List<String> numbers = new ArrayList<>();

		for (String label : labels()) {

			Matcher person = PERSON.matcher(label);

			if (person.find()) {
				numbers.add(person.group(2).trim());
			}
		}

		return numbers;
	}

	/** True while {@code number} reads as an account number the platform would issue. */
	public boolean readsAsAnAccountNumber(String number) {

		return number != null && number.matches(ACCOUNT_NUMBER);
	}

	/** Everything the screen says, on one line. */
	public String text() {

		return String.join(" | ", labels());
	}
}
