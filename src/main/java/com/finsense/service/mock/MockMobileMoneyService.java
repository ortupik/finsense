package com.finsense.service.mock;


import com.finsense.model.PaymentStatus;
import com.finsense.model.PaymentTransaction;
import com.finsense.service.MobileMoneyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MockMobileMoneyService implements MobileMoneyService {

    private static final Logger logger = LoggerFactory.getLogger(MockMobileMoneyService.class);

    @Override
    public String initiateB2CPayment(PaymentTransaction transaction) {
        logger.info("Mock Mobile Money Service: Initiating B2C payment for transaction ID: {}", transaction.getId());
        // Simulate a successful transaction after a short delay
        try {
            Thread.sleep(1000); // Simulate network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String providerTransactionId = "MOCK_" + UUID.randomUUID().toString();
        logger.info("Mock Mobile Money Service: Payment initiated successfully with provider transaction ID: {}", providerTransactionId);
        return providerTransactionId;
    }

    @Override
    public PaymentTransaction checkPaymentStatus(String providerTransactionId) {
        logger.info("Mock Mobile Money Service: Checking payment status for provider transaction ID: {}", providerTransactionId);
        // Simulate a successful status after a short delay
        try {
            Thread.sleep(500); // Simulate network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setProviderTransactionId(providerTransactionId);
        transaction.setStatus(PaymentStatus.SUCCESS); // Always return success for the mock
        logger.info("Mock Mobile Money Service: Payment status check successful, status: {}", PaymentStatus.SUCCESS);
        return transaction;
    }

    @Override
    public String getProviderType() {
        return "MOCK"; // You can use a specific provider type here if needed for testing different mocks
    }
}
