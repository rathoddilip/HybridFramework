package com.tests.web;

import com.framework.config.ConfigManager;
import com.framework.core.BaseTest;
import com.framework.pages.HomePage;
import com.framework.pages.LoginPage;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * LoginTest — Mobile + OTP Login UI Tests
 *
 * URL: baseUrL/login
 */
@Epic("Authentication")
@Feature("Mobile OTP Login")
public class LoginTest extends BaseTest {

    private LoginPage loginPage;
    private String    appUrl;
    private String    validMobile;
    private String    validOtp;

    @BeforeMethod(alwaysRun = true)
    public void openLoginPage() {
        try {
            appUrl      = ConfigManager.get("app.baseUrl");
            validMobile = ConfigManager.get("users.admin.username"); // store mobile in username field
            validOtp    = ConfigManager.get("users.admin.password"); // store test OTP in password field

            loginPage = new LoginPage();
            loginPage.navigateTo(appUrl + "/login");
        } catch (Exception e) {
            throw new RuntimeException("Failed to open login page: " + e.getMessage(), e);
        }
    }

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test(groups = {"web"}, priority = 1)
    @Story("Valid Login")
    @Description("Verify user can login with valid mobile number and OTP")
    @Severity(SeverityLevel.BLOCKER)
    public void testSuccessfulLoginWithValidMobileAndOtp() {
        HomePage homePage = loginPage.loginWithMobileAndOtp(validMobile, validOtp);
        assertThat(homePage.getCurrentUrl())
                .as("URL should redirect to /landingPage after login")
                .contains("/landingPage");
    }

}
