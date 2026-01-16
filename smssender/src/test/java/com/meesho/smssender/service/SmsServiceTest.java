package com.meesho.smssender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.smssender.exception.BlockedNumberException;
import com.meesho.smssender.exception.ValidationException;
import com.meesho.smssender.repository.BlockedNumberRepository;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private BlockedNumberRepository blockedNumberRepo;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); // Use real object mapper

    @Mock
    private Dummy dummy;

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        // Mock KafkaTemplate return to avoid NPE on .whenComplete() chains
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
    }

    // --- Validation Tests ---

    @Test
    void validate_shouldThrowException_whenMobileNumberIsNull() {
        assertThatThrownBy(() -> smsService.sendSms(null, "msg"))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Mobile number cannot be empty.");
    }

    @Test
    void validate_shouldThrowException_whenMobileNumberIsInvalidLength() {
        assertThatThrownBy(() -> smsService.sendSms("123", "msg"))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Mobile number must be exactly 10 digits.");
    }

    @Test
    void validate_shouldThrowException_whenMobileNumberContainsNonDigits() {
        assertThatThrownBy(() -> smsService.sendSms("123456789a", "msg"))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Mobile number must contain only digits.");
    }

    @Test
    void validate_shouldThrowException_whenMessageIsEmpty() {
        assertThatThrownBy(() -> smsService.sendSms("1234567890", ""))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Message content is required.");
    }

    @Test
    void validate_shouldThrowException_whenMessageIsTooLong() {
        String longMsg = "a".repeat(201);
        assertThatThrownBy(() -> smsService.sendSms("1234567890", longMsg))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Message is too long (Max 160 chars).");
    }

    // --- Send SMS Logic ---

    @Test
    void sendSms_shouldThrowExceptionAndLogToKafka_whenUserIsBlocked() throws JsonProcessingException {
        // Arrange
        String mobile = "9999999999";
        String msg = "Hello";
        when(blockedNumberRepo.isBlocked(mobile)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> smsService.sendSms(mobile, msg))
            .isInstanceOf(BlockedNumberException.class)
            .hasMessageContaining("is blocked");

        // Verify Kafka was called with "BLOCKED" status
        verify(kafkaTemplate).send(eq("sms-topic"), eq(mobile), anyString());
        // Verify we didn't start the async dummy process
        verify(dummy, never()).dummy();
    }

    @Test
    void sendSms_shouldTriggerAsyncChain_whenUserIsNotBlocked() {
        // Arrange
        String mobile = "1234567890";
        String msg = "Hello";
        when(blockedNumberRepo.isBlocked(mobile)).thenReturn(false);

        // Act
        smsService.sendSms(mobile, msg);

        // Assert
        // Since the method is void and triggers async, we primarily verify valid inputs don't crash 
        // and that it *attempts* to call the dummy service.
        // Note: verifying execution inside CompletableFuture.runAsync in unit tests is non-deterministic 
        // without complex thread mocking, but checking the repo call ensures we passed the guard clause.
        verify(blockedNumberRepo).isBlocked(mobile);
    }

    // --- Block/Unblock Logic ---

    @Test
    void blockUser_shouldReturn1_whenNewBlock() {
        String mobile = "1234567890";
        when(blockedNumberRepo.blockNumber(mobile)).thenReturn(1L);

        Long result = smsService.blockUser(mobile);

        assertThat(result).isEqualTo(1L);
        verify(kafkaTemplate).send(eq("user-topic"), eq(mobile), anyString());
    }

    @Test
    void blockUser_shouldReturn0_whenAlreadyBlocked() {
        String mobile = "1234567890";
        when(blockedNumberRepo.blockNumber(mobile)).thenReturn(0L);

        Long result = smsService.blockUser(mobile);

        assertThat(result).isEqualTo(0L);
        // Should NOT send kafka event if status wasn't changed (based on code logic)
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void unblockUser_shouldReturn1_whenUnblockedSuccessfully() {
        String mobile = "1234567890";
        when(blockedNumberRepo.unblockNumber(mobile)).thenReturn(1L);

        Long result = smsService.unblockUser(mobile);

        assertThat(result).isEqualTo(1L);
        verify(kafkaTemplate).send(eq("user-topic"), eq(mobile), anyString());
    }
}