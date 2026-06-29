package com.framework.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Request body for POST /auth/register.
 *
 * Example:
 * {"phone":"91XXXXXXXXXX"}
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequest {

    private String phone;
}
