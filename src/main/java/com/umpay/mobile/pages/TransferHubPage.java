package com.umpay.mobile.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.List;

/**
 * The list of destinations behind the dashboard's Transfer tile.
 *
 * Transfer is not a form. Tapping the tile opens a menu of six routes, and each one leads
 * somewhere different - a saved template list, a wallet-to-wallet form, two UnionPay forms
 * and two payment apps. There is no amount box on this screen at all.
 *
 * The two payment app routes carry "Maintenance" in their own label and answer a tap with
 * a warning dialog rather than a form. That is a product state worth watching rather than
 * skipping: if it ever changes, this suite should be the thing that notices.
 */
public class TransferHubPage extends BasePage {

	public static final String TEMPLATE = "UMPay to Existing template";
	public static final String WALLET = "UMPay to UMPay Wallet";
	public static final String UNIONPAY_CHINA = "UnionPay China";
	public static final String UNIONPAY_GLOBAL = "UnionPay Global";
	public static final String ALIPAY = "Transfer to AliPay";
	public static final String WECHAT = "Transfer to WeChat";

	/** Every route the hub offers, in the order the screen lists them. */
	public static final List<String> ROUTES = List.of(
			TEMPLATE, WALLET, UNIONPAY_CHINA, UNIONPAY_GLOBAL, ALIPAY, WECHAT);

	/** Exact, because "Transfer to AliPay" contains the word too. */
	private static final By TITLE = MobileLocators.exactly("Transfer");

	private static final By WARNING_TITLE = MobileLocators.labelled("Warning");

	private static final By WARNING_OK = MobileLocators.tappable("OK");

	public TransferHubPage(AndroidDriver driver) {
		super(driver);
	}

	public boolean isShowing() {

		return isPresent(TITLE, Duration.ofSeconds(20));

	}

	public boolean offersRoute(String route) {

		return isPresent(MobileLocators.tappable(route), Duration.ofSeconds(15));

	}

	public void openRoute(String route) {

		tap(MobileLocators.tappable(route), "the " + route + " route");

	}

	/**
	 * The dialog a route under maintenance answers with.
	 *
	 * Read rather than assumed: both AliPay and WeChat say "The service is currently
	 * unavailable. Please try again later." above a single OK.
	 */
	public boolean showsUnavailableWarning() {

		return isPresent(WARNING_TITLE, Duration.ofSeconds(15));

	}

	public boolean warningSays(String message) {

		return isPresent(MobileLocators.labelled(message), Duration.ofSeconds(10));

	}

	/** Closes the warning, so the scenario does not leave a dialog over the hub. */
	public void dismissWarning() {

		tapIfPresent(WARNING_OK, Duration.ofSeconds(10));

	}
}
