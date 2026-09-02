package com.umpay.mobile.stepdefs;

import com.umpay.mobile.pages.LoginPage;
import com.umpay.mobile.pages.MobileEmails;
import com.umpay.mobile.pages.RegisterPage;
import com.umpay.mobile.utility.DeviceFactory;
import com.umpay.mobile.utility.ExcelDataProvider;
import com.umpay.mobile.utility.MobileBaseClass;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.CaptchaSolver;
import org.example.MailCredentials;
import org.example.VerificationCodeReader;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Registering a new account, unattended.
 *
 * Two things make this possible without a person watching: the captcha is read by OCR from
 * a crop of the device screenshot, and the emailed code is fetched over IMAP. Both live in
 * the existing org.example helpers, which are called from here rather than from the page
 * object - the page object drives the screen, and anything that talks to a service outside
 * the phone belongs at this level.
 *
 * WHERE THE CODE IS SENT
 *
 * The account is registered under a fresh +timestamp address every run, and the code lands
 * in lawma195.infinity@gmail.com, which is where plus addressing delivers it. A registered
 * address cannot be reused, so the tag is what lets this run at all - and it keeps one
 * run's message apart from another's for the reader.
 */
public class RegisterStepDefs {

	/** Fresh images to work through before giving up on the captcha. */
	private static final int CAPTCHA_ATTEMPTS = Integer.getInteger("umpay.ocr.attempts", 10);

	/** How long to wait for the sign-up email. Delivery is usually seconds, rarely a minute. */
	private static final int MAIL_TIMEOUT_SECONDS = Integer.getInteger("umpay.mail.timeout", 120);

	/** The digits a UMPay captcha is known to have; a shorter reading is a misread. */
	private static final int CAPTCHA_LENGTH = 4;

	private static final String ADB = System.getProperty("umpay.adb", "adb");

	private RegisterPage registerPage;
	private LoginPage loginPage;

	private String registeredEmail;

	private RegisterPage register() {
		if (registerPage == null) {
			registerPage = new RegisterPage(MobileBaseClass.driver, ADB, DeviceFactory.udid(),
					new File("screenshots"));
		}
		return registerPage;
	}

	private LoginPage login() {
		if (loginPage == null) {
			loginPage = new LoginPage(MobileBaseClass.driver);
		}
		return loginPage;
	}

	@When("I open the registration form")
	public void iOpenTheRegistrationForm() {

		register().openFromLogin();
		register().chooseEmailTab();

	}

	/*
	 * Where Register_TestData keeps what this step reads. The layout is the web workbook's.
	 *
	 *   0 Scenario | 1 Email | 2 Password | 3 PhoneCountry | 4 PhoneNumber
	 *   5 CaptchaCode | 6 UniqueEmail | 7 ExpectedMessage | 8 Pin
	 *
	 * Email and Password sit at columns 1 and 2 like the login id and password of every
	 * other workbook, so the same reading holds throughout: column 1 is who you are,
	 * column 2 is the password.
	 */
	private static final int EMAIL = 1;
	private static final int PASSWORD = 2;
	private static final int PHONE_NUMBER = 4;
	private static final int UNIQUE_EMAIL = 6;

	@When("I fill in the registration form using {string} of {string} of {string}")
	public void iFillInTheRegistrationForm(String rowNumber, String excelSheetName, String excelFileName) {

		int row = Integer.parseInt(rowNumber);
		ExcelDataProvider excel = new ExcelDataProvider(excelFileName, excelSheetName);

		String address = excel.getStringData(excelSheetName, row, EMAIL);
		String password = excel.getStringData(excelSheetName, row, PASSWORD);
		String phone = excel.getStringData(excelSheetName, row, PHONE_NUMBER);
		boolean freshAddress = "Yes".equalsIgnoreCase(excel.getStringData(excelSheetName, row, UNIQUE_EMAIL));

		/*
		 * A fresh address for the sign-up, delivered to the real mailbox.
		 *
		 * The same arrangement the UMPay web suite uses, and the reason its workbook has a
		 * UniqueEmail column: UMPay will not accept an address that is already registered -
		 * pointing this at lawma195.infinity@gmail.com itself got "User already exists"
		 * every time, which it should, since that is the account the rest of the suite signs
		 * in as. A row that says No registers the address as it stands, which is how the web
		 * suite tests the rejection.
		 *
		 * The plus tag solves both halves at once: the address is new as far as the app is
		 * concerned, while the code it emails is delivered to lawma195.infinity@gmail.com
		 * like any other mail to that mailbox. It also keeps one run's message apart from
		 * another's, which matters because the IMAP reader matches on the recipient.
		 *
		 * -Dumpay.register.email still wins over the workbook, for re-running a registration
		 * against one particular address.
		 */
		registeredEmail = System.getProperty("umpay.register.email",
				freshAddress ? MobileEmails.unique(address) : address);

		password = System.getProperty("umpay.register.password", password);

		register().fillFields(registeredEmail, phone, password, "QA Test User");

		System.out.println("Registering as " + registeredEmail);

	}

	/**
	 * Reads the captcha and submits, taking a fresh image whenever the reading is unusable
	 * or the app turns it down.
	 *
	 * A misread is thrown away without submitting. The length check already knows the
	 * answer is wrong, and sending it would spend a round trip to be told so.
	 */
	@When("I solve the captcha and submit the form")
	public void iSolveTheCaptchaAndSubmit() throws IOException {

		System.out.println("Captcha OCR: " + CaptchaSolver.availability());

		boolean submitted = false;

		for (int attempt = 1; attempt <= CAPTCHA_ATTEMPTS && !submitted; attempt++) {

			File image = register().captureCaptchaImage("attempt_" + attempt);

			String reading = CaptchaSolver.solve(image, CAPTCHA_LENGTH);

			if (reading == null || reading.length() != CAPTCHA_LENGTH) {
				System.out.println("Attempt " + attempt + ": unusable reading \"" + reading
						+ "\", asking for another image");
				register().refreshCaptcha();
				continue;
			}

			System.out.println("Attempt " + attempt + ": read \"" + reading + "\"");

			register().enterCaptcha(reading);
			register().submit("Next");

			if (register().isAskingForVerificationCode(Duration.ofSeconds(15))) {
				submitted = true;
				continue;
			}

			/*
			 * Stop on a refusal no number of retries can fix.
			 *
			 * Without this the loop reads "did not reach the verification screen" as "wrong
			 * captcha" and works through ten fresh images before reporting a captcha
			 * problem - while the app had said "User already exists" on the first attempt
			 * and the OCR had been right every time.
			 */
			String rejection = register().rejectionMessage();

			if (rejection != null) {
				register().dismissDialog();
				throw new AssertionError("The app refused the sign-up: \"" + rejection
						+ "\" for " + registeredEmail
						+ ". This is not a captcha problem and retrying cannot help.");
			}

			/*
			 * Clear the dialog before asking for another image.
			 *
			 * A rejected captcha is the ordinary case, not an exception - the app answers it
			 * with an "OK / The captcha code invalid / Dismiss" box that covers the form. An
			 * earlier version went straight to refreshing, could not find the captcha field
			 * underneath the dialog, and reported that the form had no captcha input at all.
			 */
			System.out.println("Attempt " + attempt + ": the app did not accept the captcha");

			register().dismissDialog();

			register().refreshCaptcha();
		}

		assertTrue(submitted, "The captcha was not solved in " + CAPTCHA_ATTEMPTS
				+ " attempts, so the form was never submitted. The images tried are in screenshots/.");

	}

	@Then("the app should ask for the emailed verification code")
	public void theAppShouldAskForTheCode() {

		assertTrue(register().isOnVerificationScreen(Duration.ofSeconds(30)),
				"Expected the Verification Code screen, with a box to type the code into,"
						+ " after the captcha was accepted");

		System.out.println("Registration submitted - the app is asking for the code emailed to "
				+ registeredEmail);

	}

	/**
	 * Fetches the emailed code and submits it.
	 *
	 * READ THE LOG BEFORE READING A FAILURE HERE. "Mailbox opened with ..." means the IMAP
	 * side did its job and the message simply is not there. On the build this was written
	 * against, that is what happens: the app shows the verification screen but its backend
	 * sends nothing, confirmed by watching the mailbox through both a submit and a Resend
	 * while web registrations against the same mailbox delivered normally.
	 *
	 * If that is still the case, this step failing is the suite reporting an application
	 * defect, not a problem with the test or the credentials.
	 */
	/**
	 * Closes the app now the scenario has what it came for.
	 *
	 * Registration ends on the Verification Code screen, and the session does not reset, so
	 * without this the app is still sitting there when the next run starts - which is the
	 * state that had the setup step backing out to the launcher.
	 */
	@Then("the app is closed")
	public void theAppIsClosed() {

		register().close();

	}

	@When("I enter the verification code from the mailbox")
	public void iEnterTheVerificationCode() {

		String code = VerificationCodeReader.waitForCode(registeredEmail, MAIL_TIMEOUT_SECONDS);

		assertFalse(code == null || code.isEmpty(),
				"No verification code arrived for " + registeredEmail + " within "
						+ MAIL_TIMEOUT_SECONDS + " seconds. If the log above says the mailbox was"
						+ " opened, the message was never sent, and that is an app-side defect"
						+ " rather than a test or credential problem. Credentials were looked for in "
						+ MailCredentials.describe());

		register().enterVerificationCode(code);

	}

	@Then("the registration should be accepted")
	public void theRegistrationShouldBeAccepted() {

		assertFalse(register().isAskingForVerificationCode(Duration.ofSeconds(8)),
				"The verification code was not accepted - still on the Verification Code screen");

		System.out.println("Verification code accepted for " + registeredEmail);

	}
}
