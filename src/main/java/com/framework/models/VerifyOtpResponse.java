package com.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * VerifyOtpResponse — Response from:
 * POST /auth/api/v2/register/verify
 *
 * On success → returns auth token used for subsequent API calls.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyOtpResponse {

    private boolean success;
    private String  message;
    private Integer statusCode;
    @JsonAlias("payload")
    private Data    data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String  accessToken;    // JWT for authenticated requests
        private String  refreshToken;
        private String  tokenType;      // "Bearer"
        private Integer expiresIn;
        private String  userId;
        private String  mobile;
        private String  phone;
        private boolean isNewUser;      // true = first time registration
        private UserProfile profile;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserProfile {
        private String name;
        private String email;
        private String mobile;
        private String phone;
        private String kycStatus;
    }
}
