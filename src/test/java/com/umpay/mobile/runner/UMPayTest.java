package com.umpay.mobile.runner;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;;
import io.appium.java_client.android.options.UiAutomator2Options;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.openqa.selenium.WebDriver;

import java.net.URL;

/**
 * Unit test for simple App.
 */
public class UMPayTest
    extends TestCase
{
    WebDriver driver;
    static AppiumDriver apDriver;
    static AndroidDriver anDriver;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UMPayTest(String testName )
    {
        super( testName );
    }

    public static void main(String[] args){

        //openUmpay();
        try {
            UiAutomator2Options options = new UiAutomator2Options()
                    .setDeviceName("Redmi 13C")
                    .setUdid("YPYXSKCQXCXWVW55")
                    .setPlatformVersion("15")
                    .setAppPackage("com.umpay.me")
                    .setAppActivity("com.umpay.me.MainActivity")
                    .setAutomationName("UiAutomator2")
                    .setSkipServerInstallation(false)
                    // Additional options for Xiaomi devices
                    .amend("autoGrantPermissions", true)
                    .amend("allowTestPackages", true)
                    .amend("enforceAppInstall", true)
                    // Clear app data and cache
                    .amend("appWaitActivity", "*")
                    .amend("fullReset", true)
                    .amend("noReset", false);


            URL appiumServer = new URL("http://127.0.0.1:4723");
            AndroidDriver driver = new AndroidDriver(appiumServer, options);

            System.out.println("Application started....");

            // Your test code here

            driver.quit();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /*public static void openUmpay(){
        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setCapability("deviceName","Redmi 13C");
        cap.setCapability("udid","YPYXSKCQXCXWVW55");
        cap.setCapability("platformName","Android");
        cap.setCapability("platformVersion","15 AP3A.240905.015.A2");

        cap.setCapability("appPackage","com.umpay.me");
        cap.setCapability("appActivity","com.umpay.me.MainActivity");

        try {
            URL url = new URL("http://0.0.0.0:4723/wd/hub");
            anDriver = new AndroidDriver(url, cap);
            System.out.println("Application Started....");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        System.out.println("Application Started....");
    }
*/
    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( UMPayTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}