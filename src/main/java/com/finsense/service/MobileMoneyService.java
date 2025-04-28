package com.finsense.service;

import com.finsense.model.PaymentTransaction;

public interface MobileMoneyService {

    /**
     * Initiates a B2C payment with a specific mobile money provider.
     *
     * @param transaction The payment transaction details.
     * @return The provider's transaction ID if successful, or throws an exception.
     * @throws com.finsense.exception.ExternalApiException if the external API call fails.
     */
    String initiateB2CPayment(PaymentTransaction transaction);

    /**
     * Checks the status of a B2C payment with a specific mobile money provider.
     *
     * @param providerTransactionId The transaction ID from the mobile money provider.
     * @return The updated payment transaction status.
     * @throws com.finsense.exception.ExternalApiException if the external API call fails.
     */
    PaymentTransaction checkPaymentStatus(String providerTransactionId);

    /**
     * Returns the provider type that this service implementation handles.
     *
     * @return The mobile money provider type (e.g., "MPESA", "AIRTEL_MONEY").
     */
    String getProviderType();
}