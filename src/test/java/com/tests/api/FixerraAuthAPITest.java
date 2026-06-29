package com.tests.api;

import com.framework.api.FixerraAuthAPI;
import com.framework.config.ConfigManager;
import com.framework.core.BaseTest;
import com.framework.models.VerifyOtpResponse;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FixerraAuthAPITest — Happy Path API Tests for Fixerra Authentication
 *
 * Endpoints:
 *  POST /auth/register — Send OTP
 *  POST /auth/api/v2/register/verify — Verify OTP
 */
@Epic("Authentication")
@Feature("Auth API")
public class FixerraAuthAPITest extends BaseTest {

    private FixerraAuthAPI authAPI;
    private String validMobile;
    private String validOtp;

    @Override
    protected boolean isWebTest() {
        return false;
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        authAPI     = new FixerraAuthAPI();
        validMobile = toApiPhone(ConfigManager.get("users.admin.username"));
        validOtp    = ConfigManager.get("users.admin.password");
    }

    private String toApiPhone(String mobile) {
        return mobile.startsWith("91") ? mobile : "91" + mobile;
    }

    // ════════════════════════════════════════════════════════════════
    //  Happy Path: Register — Send OTP
    // ════════════════════════════════════════════════════════════════

    @Test(groups = {"api", "smoke", "auth"}, priority = 1)
    @Story("Register - Send OTP")
    @Description("Happy path: POST /auth/register returns 201 for valid mobile number")
    @Severity(SeverityLevel.BLOCKER)
    public void testRegisterWithValidMobile() {
        Response response = authAPI.sendOtp(validMobile);

        assertThat(response.getStatusCode())
                .as("Register endpoint should return 201 for valid phone")
                .isIn(200, 201);
    }

    // ════════════════════════════════════════════════════════════════
    //  Happy Path: Verify OTP
    // ════════════════════════════════════════════════════════════════

    @Test(groups = {"api", "smoke", "auth"}, priority = 2)
    @Story("Verify OTP")
    @Description("Happy path: POST /auth/api/v2/register/verify returns 200 with access token for valid OTP")
    @Severity(SeverityLevel.BLOCKER)
    public void testVerifyOtpWithValidOtp() {
        authAPI.register(validMobile);
        VerifyOtpResponse response = authAPI.verifyOtpAndGetResponse(validMobile, validOtp);

        assertThat(response.isSuccess())
                .as("Verify response success flag should be true")
                .isTrue();

        assertThat(response.getData())
                .as("Verify response data should not be null")
                .isNotNull();

        assertThat(response.getData().getAccessToken())
                .as("Access token should be present in response")
                .isNotNull()
                .isNotEmpty();

        assertThat(response.getData().getRefreshToken())
                .as("Refresh token should be present in response")
                .isNotNull()
                .isNotEmpty();
    }
}
