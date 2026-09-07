package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The language chooser the app opens with on a fresh install.
 *
 * It is not shown once a language has been chosen, so every method here treats its absence
 * as normal rather than as a failure.
 */
public class LanguagePage extends BasePage {

	private static final By TITLE = MobileLocators.labelled("Select Language");
	private static final By CONFIRM = MobileLocators.tappable("Confirm");

	public LanguagePage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(TITLE, Duration.ofSeconds(5));

	}

	private By language(String name) {

		return MobileLocators.tappable(name);

	}

	/**
	 * Chooses a language and confirms, if the chooser is up. Returns whether it did
	 * anything, so a scenario can report "already past this" rather than fail.
	 */
	public boolean chooseIfAsked(String languageName) {

		if (!isShowing()) {
			return false;
		}

		tap(language(languageName), "the " + languageName + " option");
		tap(CONFIRM, "the Confirm button on the language chooser");

		return true;

	}
}
