package com.finsense.service;

import com.finsense.exception.ExternalApiException;
import com.finsense.exception.InvalidRequestException;
import com.finsense.exception.PaymentException;
import com.finsense.model.B2CPaymentRequest;
import com.finsense.model.PaymentStatus;
import com.finsense.model.PaymentTransaction;
import com.finsense.repository.PaymentTransactionRepository;
import com.finsense.util.PaymentIdGenerator;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final List<MobileMoneyService> mobileMoneyServices;
    private final SmsGateway smsGateway;
    private final ExecutorService notificationExecutor;

    @Autowired
    public PaymentService(PaymentTransactionRepository paymentTransactionRepository,
                          List<MobileMoneyService> mobileMoneyServices,
                          SmsGateway smsGateway) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.mobileMoneyServices = mobileMoneyServices;
        this.smsGateway = smsGateway;
        this.notificationExecutor = Executors.newFixedThreadPool(5); // Thread pool for sending notifications asynchronously
    }

    @Transactional
    public PaymentTransaction initiatePayment(B2CPaymentRequest request) {
        logger.info("Initiating payment for recipient: {}", request.getRecipientPhoneNumber());

        // Basic validation
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be positive.");
        }

        // Find the appropriate mobile money service based on the provider
        MobileMoneyService mobileMoneyService = findMobileMoneyService(request.getProvider());

        // Create a new payment transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(PaymentIdGenerator.generate());
        transaction.setRecipientPhoneNumber(request.getRecipientPhoneNumber());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setProvider(request.getProvider());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Save the initial transaction state
        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
        logger.info("Payment transaction saved with ID: {}", savedTransaction.getId());

        try {
            // Initiate the payment with the mobile money provider
            String providerTransactionId = mobileMoneyService.initiateB2CPayment(savedTransaction);
            savedTransaction.setProviderTransactionId(providerTransactionId);
            savedTransaction.setStatus(PaymentStatus.IN_PROGRESS);
            savedTransaction.setUpdatedAt(LocalDateTime.now());
            paymentTransactionRepository.save(savedTransaction);
            logger.info("Payment initiation successful with provider transaction ID: {}", providerTransactionId);

            notifyRecipient(savedTransaction.getId(), PaymentStatus.SUCCESS);

        } catch (ExternalApiException e) {
            logger.error("Error initiating payment with provider: {}", request.getProvider(), e);
            savedTransaction.setStatus(PaymentStatus.FAILED);
            savedTransaction.setFailureReason("External API error: " + e.getMessage());
            savedTransaction.setUpdatedAt(LocalDateTime.now());
            paymentTransactionRepository.save(savedTransaction);
            notifyRecipient(savedTransaction.getId(), PaymentStatus.FAILED);
            throw new PaymentException("Failed to initiate payment with mobile money provider.", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during payment initiation", e);
            savedTransaction.setStatus(PaymentStatus.FAILED);
            savedTransaction.setFailureReason("An unexpected error occurred: " + e.getMessage());
            savedTransaction.setUpdatedAt(LocalDateTime.now());
            paymentTransactionRepository.save(savedTransaction);
            notifyRecipient(savedTransaction.getId(), PaymentStatus.FAILED);
            throw new PaymentException("An unexpected error occurred during payment initiation.", e);
        }

        return savedTransaction;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> getPaymentStatus(String transactionId) {
        logger.info("Fetching payment status for transaction ID: {}", transactionId);
        Optional<PaymentTransaction> transaction = paymentTransactionRepository.findById(transactionId);

        if (transaction.isPresent()) {
            logger.info("Found transaction with status: {}", transaction.get().getStatus());
        } else {
            logger.warn("Payment transaction not found with ID: {}", transactionId);
        }
        return transaction;
    }

    public void processProviderStatusUpdate(String providerTransactionId, PaymentStatus newStatus, String failureReason) {
        logger.info("Processing provider status update for provider transaction ID: {} with new status: {}", providerTransactionId, newStatus);
        Optional<PaymentTransaction> optionalTransaction = paymentTransactionRepository.findByProviderTransactionId(providerTransactionId);

        if (optionalTransaction.isPresent()) {
            PaymentTransaction transaction = optionalTransaction.get();
            transaction.setStatus(newStatus);
            transaction.setFailureReason(failureReason);
            transaction.setUpdatedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);
            logger.info("Updated transaction {} status to {}", transaction.getId(), newStatus);

            notifyRecipient(transaction.getId(), newStatus);
        } else {
            logger.warn("No local transaction found for provider transaction ID: {}", providerTransactionId);
        }
    }

    private MobileMoneyService findMobileMoneyService(String provider) {
        return mobileMoneyServices.stream()
                .filter(service -> service.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Unsupported mobile money provider: " + provider));
    }

    private void notifyRecipient(String transactionId, PaymentStatus status) {
        notificationExecutor.submit(() -> {
            try {
                Optional<PaymentTransaction> optionalTransaction = paymentTransactionRepository.findById(transactionId);
                if (optionalTransaction.isPresent()) {
                    PaymentTransaction transaction = optionalTransaction.get();
                    String message;
                    switch (status) {
                        case SUCCESS:
                            message = String.format("Your payment of %s %s has been successfully processed. Transaction ID: %s",
                                    transaction.getAmount(), transaction.getCurrency(), transaction.getId());
                            break;
                        case FAILED:
                            message = String.format("Your payment of %s %s failed. Transaction ID: %s. Reason: %s",
                                    transaction.getAmount(), transaction.getCurrency(), transaction.getId(),
                                    transaction.getFailureReason() != null ? transaction.getFailureReason() : "Unknown");
                            break;
                        case PENDING:
                            message = String.format("Your payment of %s %s is pending. Transaction ID: %s",
                                    transaction.getAmount(), transaction.getCurrency(), transaction.getId());
                            break;
                        case IN_PROGRESS:
                            message = String.format("Your payment of %s %s is being processed. Transaction ID: %s",
                                    transaction.getAmount(), transaction.getCurrency(), transaction.getId());
                            break;
                        case CANCELLED:
                            message = String.format("Your payment of %s %s has been cancelled. Transaction ID: %s",
                                    transaction.getAmount(), transaction.getCurrency(), transaction.getId());
                            break;
                        default:
                            message = String.format("Update on your payment of %s %s. Transaction ID: %s. Status: %s",
                                    transaction.getAmount(), transaction.getCurrency(), transaction.getId(), status);
                    }
                    smsGateway.sendSms(transaction.getRecipientPhoneNumber(), message);
                    logger.info("SMS notification sent for transaction ID: {}", transactionId);
                } else {
                    logger.warn("Could not find transaction {} to send notification.", transactionId);
                }
            } catch (ExternalApiException e) {
                logger.error("Failed to send SMS notification for transaction ID: {}", transactionId, e);
                // Handle failure to send SMS (e.g., retry mechanism, logging)
            } catch (Exception e) {
                logger.error("An unexpected error occurred while sending SMS notification for transaction ID: {}", transactionId, e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        notificationExecutor.shutdown();
    }
}

