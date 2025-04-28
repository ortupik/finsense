package com.finsense.service;

import com.finsense.model.PaymentTransaction;

public interface MobileMoneyService {


    String initiateB2CPayment(PaymentTransaction transaction);


    PaymentTransaction checkPaymentStatus(String providerTransactionId);

    String getProviderType();
}