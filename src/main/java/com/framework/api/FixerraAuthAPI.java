package com.framework.api;

import com.framework.config.ConfigManager;
import com.framework.core.APIClient;
import com.framework.models.RegisterRequest;
import com.framework.models.RegisterResponse;
import com.framework.models.VerifyOtpRequest;
import com.framework.models.VerifyOtpResponse;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service class for authentication APIs.
 */
public class FixerraAuthAPI extends APIClient {

    private static final Logger log = LogManager.getLogger(FixerraAuthAPI.class);

    private static final String REGISTER_ENDPOINT = "/auth/register";
    private static final String VERIFY_OTP_ENDPOINT = "/auth/api/v2/register/verify";

    @Override
    protected void initSpecs() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept("application/json, text/plain, */*")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Origin", ConfigManager.get("auth.origin"))
                .addHeader("Referer", ConfigManager.get("auth.referer"))
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-site")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Mobile Safari/537.36")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"147\", \"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"147\"")
                .addHeader("sec-ch-ua-mobile", "?1")
                .addHeader("sec-ch-ua-platform", "\"Android\"")
                .addFilter(new AllureRestAssured()
                        .setRequestTemplate("http-request.ftl")
                        .setResponseTemplate("http-response.ftl"))
                .log(LogDetail.ALL)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .log(LogDetail.ALL)
                .build();
    }

    @Step("API: Register phone: {phone}")
    public Response register(String phone) {
        log.info("Registering phone: {}", phone);

        RegisterRequest request = RegisterRequest.builder()
                .phone(phone)
                .build();

        return post(REGISTER_ENDPOINT, request);
    }

    @Step("API: Register phone and get parsed response: {phone}")
    public RegisterResponse registerAndGetResponse(String phone) {
        Response response = register(phone);
        assertCreatedOrOk(response);
        return response.as(RegisterResponse.class);
    }

    @Step("API: Send OTP to phone: {phone}")
    public Response sendOtp(String phone) {
        return register(phone);
    }

    @Step("API: Send OTP and get parsed response: {phone}")
    public RegisterResponse sendOtpAndGetResponse(String phone) {
        return registerAndGetResponse(phone);
    }

    @Step("API: Verify OTP for phone: {phone}")
    public Response verifyOtp(String phone, String otp) {
        String partnerUrl = ConfigManager.get("auth.partnerUrl", "app-host.example");
        boolean consent = Boolean.parseBoolean(ConfigManager.get("auth.consent", "false"));
        return verifyOtp(phone, otp, partnerUrl, consent);
    }

    @Step("API: Verify OTP for phone: {phone}")
    public Response verifyOtp(String phone, String otp, String partnerUrl, boolean consent) {
        log.info("Verifying OTP for phone: {} | partnerUrl: {} | consent: {}", phone, partnerUrl, consent);

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .phone(phone)
                .otp(otp)
                .partnerUrl(partnerUrl)
                .consent(consent)
                .build();

        return post(VERIFY_OTP_ENDPOINT, request);
    }

    /**
     * Backward-compatible overload. The current API curl does not require requestId.
     */
    @Step("API: Verify OTP for phone: {phone}")
    public Response verifyOtp(String phone, String otp, String requestId) {
        return verifyOtp(phone, otp);
    }

    @Step("API: Verify OTP and get parsed response")
    public VerifyOtpResponse verifyOtpAndGetResponse(String phone, String otp) {
        Response response = verifyOtp(phone, otp);
        assertCreatedOrOk(response);
        return response.as(VerifyOtpResponse.class);
    }

    /**
     * Backward-compatible overload. The current API curl does not require requestId.
     */
    @Step("API: Verify OTP and get parsed response")
    public VerifyOtpResponse verifyOtpAndGetResponse(String phone, String otp, String requestId) {
        return verifyOtpAndGetResponse(phone, otp);
    }

    @Step("API: Verify OTP and extract access token")
    public String verifyOtpAndGetToken(String phone, String otp) {
        VerifyOtpResponse response = verifyOtpAndGetResponse(phone, otp);

        String token = response.getData() != null
                ? response.getData().getAccessToken()
                : null;

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Access token not found in verify OTP response");
        }

        log.info("Access token extracted successfully for phone: {}", phone);
        return token;
    }

    /**
     * Backward-compatible helper. The current API curl does not return or require requestId.
     */
    @Step("API: Register phone without requestId requirement: {phone}")
    public String sendOtpAndGetRequestId(String phone) {
        registerAndGetResponse(phone);
        return null;
    }

    @Step("API: Register invalid phone: {invalidPhone}")
    public Response sendOtpToInvalidMobile(String invalidPhone) {
        log.info("Testing invalid phone: {}", invalidPhone);

        RegisterRequest request = RegisterRequest.builder()
                .phone(invalidPhone)
                .build();

        return post(REGISTER_ENDPOINT, request);
    }

    @Step("API: Verify with wrong OTP for phone: {phone}")
    public Response verifyWrongOtp(String phone, String wrongOtp) {
        return verifyOtp(phone, wrongOtp);
    }

    /**
     * Backward-compatible overload. The current API curl does not require requestId.
     */
    @Step("API: Verify with wrong OTP for phone: {phone}")
    public Response verifyWrongOtp(String phone, String wrongOtp, String requestId) {
        return verifyWrongOtp(phone, wrongOtp);
    }

    @Step("API: Verify OTP without partner_url for phone: {phone}")
    public Response verifyOtpWithoutPartnerUrl(String phone, String otp) {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .phone(phone)
                .otp(otp)
                .consent(false)
                .build();

        return post(VERIFY_OTP_ENDPOINT, request);
    }

    /**
     * Backward-compatible method name. The current API validation case is missing partner_url.
     */
    @Step("API: Verify OTP with missing required field for phone: {phone}")
    public Response verifyOtpWithoutRequestId(String phone, String otp) {
        return verifyOtpWithoutPartnerUrl(phone, otp);
    }

    private void assertCreatedOrOk(Response response) {
        int statusCode = response.getStatusCode();
        if (statusCode != 200 && statusCode != 201) {
            throw new AssertionError(
                    String.format("Expected status [200 or 201] but got [%d]. Body: %s",
                            statusCode, response.getBody().asPrettyString())
            );
        }
    }
}
