package com.finsense.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class PaymentTransaction {

    @Id
    private String id;

    private String recipientPhoneNumber;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String description;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String providerTransactionId; // ID from the mobile money provider
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
