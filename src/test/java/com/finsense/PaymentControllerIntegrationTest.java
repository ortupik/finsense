package com.finsense;

import com.finsense.model.B2CPaymentRequest;
import com.finsense.model.PaymentStatus;
import com.finsense.model.PaymentTransaction;
import com.finsense.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use a test profile for H2 configuration
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @BeforeEach
    void setUp() {
        paymentTransactionRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_payment:initiate"})
    void testInitiatePayment_Success() throws Exception {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber("+254712345678");
        request.setAmount(BigDecimal.valueOf(500.00));
        request.setCurrency("KES");
        request.setProvider("MOCK");
        request.setDescription("Integration test payment");

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientPhoneNumber", is("+254712345678")))
                .andExpect(jsonPath("$.amount", is(500.00)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS"))); // Mock always returns IN_PROGRESS initially
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_payment:initiate"})
    void testInitiatePayment_InvalidRequest() throws Exception {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber(""); // Invalid phone number
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("KES");
        request.setProvider("MOCK");
        request.setDescription("Test payment");

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_payment:status"})
    void testGetPaymentStatus_Found() throws Exception {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId("test-transaction-id");
        transaction.setRecipientPhoneNumber("+254720123456");
        transaction.setAmount(BigDecimal.valueOf(200.00));
        transaction.setCurrency("KES");
        transaction.setProvider("MPESA");
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        mockMvc.perform(get("/api/v1/payments/{transactionId}/status", "test-transaction-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-transaction-id")))
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_payment:status"})
    void testGetPaymentStatus_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{transactionId}/status", "non-existent-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_payment:initiate"})
    void testInitiatePayment_UnsupportedProvider() throws Exception {
        B2CPaymentRequest request = new B2CPaymentRequest();
        request.setRecipientPhoneNumber("+254712345678");
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("KES");
        request.setProvider("UNSUPPORTED"); // Unsupported provider
        request.setDescription("Test payment");

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
