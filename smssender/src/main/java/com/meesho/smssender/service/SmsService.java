package com.meesho.smssender.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.meesho.smssender.dto.KafkaSmsData;
import com.meesho.smssender.dto.KafkaUserData;
import com.meesho.smssender.exception.BlockedNumberException;
import com.meesho.smssender.exception.ValidationException;
import com.meesho.smssender.repository.BlockedNumberRepository;

import jakarta.annotation.PostConstruct;

@Service
public class SmsService {

    @Autowired
    private BlockedNumberRepository blockedNumberRepo;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    Dummy dummy;

    @PostConstruct
    public void init() {
        try {
            kafkaTemplate.partitionsFor("sms-topic");
            System.out.println("KAFKA WARMUP: Producer classes loaded successfully.");
        } catch (Exception e) {
            System.out.println("KAFKA WARMUP WARNING: " + e.getMessage());
        }
    }

    public void validate(String mobileNumber, String message) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new ValidationException("Mobile number cannot be empty.");
        }
        if (mobileNumber.length() != 10) {
            throw new ValidationException("Mobile number must be exactly 10 digits.");
        }
        for (char c : mobileNumber.toCharArray()) {
            if (!Character.isDigit(c)) {
                throw new ValidationException("Mobile number must contain only digits.");
            }
        }
        if (message == null || message.trim().isEmpty()) {
            throw new ValidationException("Message content is required.");
        }
        if (message.length() > 200) {
            throw new ValidationException("Message is too long (Max 160 chars).");
        }
    }

    public void validate(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new ValidationException("Mobile number cannot be empty.");
        }
        if (mobileNumber.length() != 10) {
            throw new ValidationException("Mobile number must be exactly 10 digits.");
        }
        for (char c : mobileNumber.toCharArray()) {
            if (!Character.isDigit(c)) {
                throw new ValidationException("Mobile number must contain only digits.");
            }
        }
    }

    private String serialize(KafkaSmsData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize KafkaSmsData", e);
        }
    }

    private String serialize(KafkaUserData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize KafkaSmsData", e);
        }
    }

    public void sendSms(String mobileNumber, String message) {
        validate(mobileNumber, message);
        Boolean isBlocked = blockedNumberRepo.isBlocked(mobileNumber);
        if (Boolean.TRUE.equals(isBlocked)) {
            KafkaSmsData payload = new KafkaSmsData(mobileNumber, message, "BLOCKED");
            kafkaTemplate.send("sms-topic", mobileNumber, serialize(payload))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("Failed to log " + "BLOCKED" + " event to Kafka: " + ex.getMessage());
                    }
                }
            );
            throw new BlockedNumberException("The number " + mobileNumber + " is blocked.");
        }
        CompletableFuture.runAsync(() -> dummy.dummy())
            .thenCompose(v -> {
                System.out.println("UPDATE: Sleep finished, now sending to Kafka...");
                KafkaSmsData payload = new KafkaSmsData(mobileNumber, message, "SUCCESS");
                return kafkaTemplate.send("sms-topic", mobileNumber, serialize(payload));
            })
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    System.out.println("Failed at some point in the chain" + ex);
                    ex.printStackTrace();
                } else {
                    System.out.println("Success! Kafka offset: " + result.getRecordMetadata().offset());
                }
            }
        );
    }

    public Long blockUser(String mobileNumber) {
        validate(mobileNumber);
        Long status = blockedNumberRepo.blockNumber(mobileNumber);
        if (status == 1) {
            KafkaUserData payload = new KafkaUserData(mobileNumber, "BLOCKED");
            kafkaTemplate.send("user-topic", mobileNumber, serialize(payload))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("Failed to log " + "USER BLOCKING" + " event to Kafka: " + ex.getMessage());
                    } else {
                        System.out.println("Success! Kafka offset: " + result.getRecordMetadata().offset());
                    }
                }
            );
        }
        return status;
    }

    public Long unblockUser(String mobileNumber) {
        validate(mobileNumber);
        Long status = blockedNumberRepo.unblockNumber(mobileNumber);
        if (status == 1) {
            KafkaUserData payload = new KafkaUserData(mobileNumber, "UNBLOCKED");
            kafkaTemplate.send("user-topic", mobileNumber, serialize(payload))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("Failed to log " + "USER UNBLOCKING" + " event to Kafka: " + ex.getMessage());
                    } else {
                        System.out.println("Success! Kafka offset: " + result.getRecordMetadata().offset());
                    }
                }
            );
        }
        return status;
    }
}

@Service
class Dummy {
    @Async
    public void dummy() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Thread was interrupted.");
        }
    }
}