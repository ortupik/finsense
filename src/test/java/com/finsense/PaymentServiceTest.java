package com.finsense;

import com.finsense.exception.PaymentException;
import com.finsense.model.B2CPaymentRequest;
import com.finsense.model.PaymentStatus;
import com.finsense.model.PaymentTransaction;
import com.finsense.repository.PaymentTransactionRepository;
import com.finsense.exception.InvalidRequestException;
import com.finsense.exception.ExternalApiException;
import com.finsense.service.MobileMoneyService;
import com.finsense.service.PaymentService;
import com.finsense.service.mock.MockMobileMoneyService;
import com.finsense.service.mock.MockSmsGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private MockMobileMoneyService mockMobileMoneyService; // Mock the specific mock implementation

    @Mock
    private MockSmsGateway mockSmsGateway; // Mock the specific mock implementation

    private List<MobileMoneyService> mobileMoneyServices;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        mobileMoneyServices = Collections.singletonList(mockMobileMoneyService); // Provide the mock service
        paymentService = new PaymentService(paymentTransactionRepository, mobileMoneyServices, mockSmsGateway);

        // Configure the mock mobile money service to return a specific provider type
        when(mockMobileMoneyService.getProviderType()).thenReturn("MOCK");
    }

    @Test
    void testInitiatePayment_Success() {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber("+254712345678");
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("KES");
        request.setProvider("MOCK");
        request.setDescription("Test payment");

        PaymentTransaction initialTransaction = new PaymentTransaction();
        initialTransaction.setId("test-id");
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(initialTransaction);
        when(mockMobileMoneyService.initiateB2CPayment(any(PaymentTransaction.class))).thenReturn("provider-tx-id");

        PaymentTransaction result = paymentService.initiatePayment(request);

        assertNotNull(result);
        assertEquals(PaymentStatus.IN_PROGRESS, result.getStatus());
        assertEquals("provider-tx-id", result.getProviderTransactionId());

        verify(paymentTransactionRepository, times(2)).save(any(PaymentTransaction.class)); // Initial save and update
        verify(mockMobileMoneyService, times(1)).initiateB2CPayment(any(PaymentTransaction.class));
        // verify(mockSmsGateway, times(1)).sendSms(anyString(), anyString()); // Notification is async
    }

    @Test
    void testInitiatePayment_InvalidAmount() {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber("+254712345678");
        request.setAmount(BigDecimal.valueOf(0.00)); // Invalid amount
        request.setCurrency("KES");
        request.setProvider("MOCK");
        request.setDescription("Test payment");

        assertThrows(InvalidRequestException.class, () -> paymentService.initiatePayment(request));

        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(mockMobileMoneyService, never()).initiateB2CPayment(any(PaymentTransaction.class));
        verify(mockSmsGateway, never()).sendSms(anyString(), anyString());
    }

    @Test
    void testInitiatePayment_UnsupportedProvider() {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber("+254712345678");
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("KES");
        request.setProvider("UNSUPPORTED_PROVIDER"); // Unsupported provider
        request.setDescription("Test payment");

        assertThrows(InvalidRequestException.class, () -> paymentService.initiatePayment(request));

        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(mockMobileMoneyService, never()).initiateB2CPayment(any(PaymentTransaction.class));
        verify(mockSmsGateway, never()).sendSms(anyString(), anyString());
    }

    @Test
    void testInitiatePayment_ExternalApiFailure() {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber("+254712345678");
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("KES");
        request.setProvider("MOCK");
        request.setDescription("Test payment");

        PaymentTransaction initialTransaction = new PaymentTransaction();
        initialTransaction.setId("test-id");
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(initialTransaction);
        when(mockMobileMoneyService.initiateB2CPayment(any(PaymentTransaction.class)))
                .thenThrow(new ExternalApiException("Provider API error"));

        assertThrows(PaymentException.class, () -> paymentService.initiatePayment(request));

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository, times(2)).save(transactionCaptor.capture()); // Initial save and update

        PaymentTransaction failedTransaction = transactionCaptor.getAllValues().get(1);
        assertEquals(PaymentStatus.FAILED, failedTransaction.getStatus());
        assertTrue(failedTransaction.getFailureReason().contains("External API error"));

        verify(mockMobileMoneyService, times(1)).initiateB2CPayment(any(PaymentTransaction.class));
        // verify(mockSmsGateway, times(1)).sendSms(anyString(), anyString()); // Notification is async
    }


    @Test
    void testGetPaymentStatus_Found() {
        String transactionId = "existing-id";
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(transactionId);
        transaction.setStatus(PaymentStatus.SUCCESS);

        when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        Optional<PaymentTransaction> result = paymentService.getPaymentStatus(transactionId);

        assertTrue(result.isPresent());
        assertEquals(PaymentStatus.SUCCESS, result.get().getStatus());
        verify(paymentTransactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void testGetPaymentStatus_NotFound() {
        String transactionId = "non-existent-id";
        when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        Optional<PaymentTransaction> result = paymentService.getPaymentStatus(transactionId);

        assertFalse(result.isPresent());
        verify(paymentTransactionRepository, times(1)).findById(transactionId);
    }
}




