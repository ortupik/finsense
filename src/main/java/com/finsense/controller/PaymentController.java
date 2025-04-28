package com.finsense.controller;

import com.finsense.exception.PaymentException;
import com.finsense.model.B2CPaymentRequest;
import com.finsense.model.PaymentTransaction;
import com.finsense.service.PaymentService;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('SCOPE_payment:initiate')") // Secure this endpoint
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody B2CPaymentRequest request) {
        logger.info("Received payment initiation request for recipient: {}", request.getRecipientPhoneNumber());
        try {
            PaymentTransaction transaction = paymentService.initiatePayment(request);
            logger.info("Payment initiation request processed successfully for transaction ID: {}", transaction.getId());
            return new ResponseEntity<>(transaction, HttpStatus.CREATED);
        } catch (PaymentException e) {
            logger.error("Payment initiation failed", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // Or a more specific error response
        } catch (Exception e) {
            logger.error("An unexpected error occurred during payment initiation", e);
            return new ResponseEntity<>("An internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{transactionId}/status")
    @PreAuthorize("hasAuthority('SCOPE_payment:status')") // Secure this endpoint
    public ResponseEntity<?> getPaymentStatus(@PathVariable String transactionId) {
        logger.info("Received request for payment status for transaction ID: {}", transactionId);
        Optional<PaymentTransaction> transaction = paymentService.getPaymentStatus(transactionId);

        if (transaction.isPresent()) {
            logger.info("Found transaction status for ID {}: {}", transactionId, transaction.get().getStatus());
            return new ResponseEntity<>(transaction.get(), HttpStatus.OK);
        } else {
            logger.warn("Payment transaction not found with ID: {}", transactionId);
            return new ResponseEntity<>("Payment transaction not found.", HttpStatus.NOT_FOUND);
        }
    }


}