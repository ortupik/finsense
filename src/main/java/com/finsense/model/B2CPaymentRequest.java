package com.finsense.model;

import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Data
public class B2CPaymentRequest {

    @NotBlank(message = "Recipient phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String recipientPhoneNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency; // e.g., KES, USD

    @NotBlank(message = "Mobile money provider is required")
    private String provider; // e.g., MPESA, AIRTEL_MONEY

    private String description;
}

