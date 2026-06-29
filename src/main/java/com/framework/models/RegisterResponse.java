package com.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * RegisterResponse — Response from:
 * POST /auth/register
 *
 * Update field names after inspecting the actual API response
 * in browser DevTools → Network → register → Response tab.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterResponse {

    private boolean success;
    private String  message;
    private String  requestId;   // Used in OTP verify call
    private String  sessionId;   // Alternative session identifier
    private Integer statusCode;
    @JsonAlias("payload")
    private Data    data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String  mobile;
        private String  phone;
        private String  requestId;
        private String  token;      // Some APIs return temp token here
        private Integer otpLength;  // 4 or 6
        private Integer expiresIn;  // OTP expiry in seconds
    }
}
