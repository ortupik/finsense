package com.finsense.service.mock;


import com.finsense.service.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockSmsGateway implements SmsGateway {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsGateway.class);

    @Override
    public void sendSms(String recipientPhoneNumber, String message) {
        logger.info("Mock SMS Gateway: Sending SMS to {}: {}", recipientPhoneNumber, message);
        // Simulate successful SMS sending
        try {
            Thread.sleep(300); // Simulate network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("Mock SMS Gateway: SMS sent successfully.");
    }
}
