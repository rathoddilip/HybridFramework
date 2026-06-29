package com.framework.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Request body for POST /auth/api/v2/register/verify.
 *
 * Example:
 * {"phone":"91XXXXXXXXXX","otp":"000000","partner_url":"app-host.example","consent":false}
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyOtpRequest {

    private String phone;
    private String otp;

    @JsonProperty("partner_url")
    private String partnerUrl;

    private Boolean consent;
}
