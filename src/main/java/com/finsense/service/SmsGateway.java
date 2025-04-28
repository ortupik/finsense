package com.finsense.service;

public interface SmsGateway {

    /**
     * Sends an SMS message to a recipient.
     *
     * @param recipientPhoneNumber The phone number of the recipient.
     * @param message The content of the SMS message.
     * @throws com.finsense.payment.exception.ExternalApiException if the external API call fails.
     */
    void sendSms(String recipientPhoneNumber, String message);
}
