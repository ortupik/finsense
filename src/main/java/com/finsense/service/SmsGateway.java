package com.finsense.service;

public interface SmsGateway {

    void sendSms(String recipientPhoneNumber, String message);
}
