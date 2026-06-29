package com.framework.pages;

import com.framework.core.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * LoginPage — Investment App
 * <p>
 * URL: baseUrl/login
 * <p>
 * Flow:
 *  Step 1 → Enter Aadhaar-linked mobile number → Click "Get Started"
 *  Step 2 → Enter OTP received on mobile
 *  Step 3 → Redirect to /pendingPage
 */
public class LoginPage extends BasePage {

    // ── Step 1: Mobile Number Screen ──────────────────────────────────────────

    @FindBy(id = "noCopyPaste")
    private WebElement mobileNumberField;

    @FindBy(xpath = "//button[normalize-space()='Get Started']")
    private WebElement getStartedButton;

    // ── Step 2: OTP Screen ────────────────────────────────────────────────────

    // Single OTP field strategy
    @FindBy(css = "//input[contains(@aria-label,'Please enter OTP character')]")
    private WebElement otpSingleField;

    // Split OTP boxes strategy (one box per digit)
    private final By otpBoxes       = By.xpath("//input[contains(@aria-label,'Please enter OTP character')]");
    private final By otpScreen      = By.xpath("//div[@class='signup_login_middle_section__aiigo']");
    private final By welcomeText    = By.xpath("//h1[contains(.,'Welcome to') and contains(.,'Fixerra')]");

    // ── Full Login Flow ───────────────────────────────────────────────────────

    /**
     * Complete login:
     * Enter mobile → Get Started → Enter OTP → Submit → HomePage
     */
    @Step("Login with mobile: {mobileNumber} and OTP: {otp}")
    public HomePage loginWithMobileAndOtp(String mobileNumber, String otp) {
        enterMobileNumber(mobileNumber);
        clickGetStarted();
        waitForOtpScreen();
        enterOtp(otp);
        waitForPendingPage();
        return new HomePage();
    }

    /**
     * Submit mobile only — used in hybrid tests where
     * OTP is retrieved from the API response.
     */
    @Step("Submit mobile number only: {mobileNumber}")
    public LoginPage submitMobileNumber(String mobileNumber) {
        enterMobileNumber(mobileNumber);
        clickGetStarted();
        waitForOtpScreen();
        return this;
    }

    // ── Step 1 Actions ────────────────────────────────────────────────────────

    @Step("Enter Aadhaar-linked mobile number: {mobileNumber}")
    public void enterMobileNumber(String mobileNumber) {
        waitForVisible(mobileNumberField);
        type(mobileNumberField, mobileNumber);
    }

    @Step("Click Get Started")
    public void clickGetStarted() {
        click(getStartedButton);
    }

    // ── Step 2 Actions ────────────────────────────────────────────────────────

    @Step("Enter OTP: {otp}")
    public void enterOtp(String otp) {
        // Strategy 1: Single field OTP
        if (isDisplayed(By.xpath(
                "input[aria-label='Please enter OTP character 1']"))) {
            type(otpSingleField, otp);
            return;
        }

        // Strategy 2: Split digit boxes
        List<WebElement> boxes = findAll(otpBoxes);
        if (!boxes.isEmpty()) {
            for (int i = 0; i < otp.length() && i < boxes.size(); i++) {
                boxes.get(i).sendKeys(String.valueOf(otp.charAt(i)));
            }
        }
    }

    // ── Validations ───────────────────────────────────────────────────────────

    public boolean isPendingScreenDisplayed() {
        return isDisplayed(welcomeText);
    }

    // ── Private Waits ─────────────────────────────────────────────────────────

    private void waitForOtpScreen() {
        waitForVisible(otpScreen);
    }

    private void waitForPendingPage() {
        wait.until(driver -> driver.getCurrentUrl().contains("/landingPage"));
    }

}