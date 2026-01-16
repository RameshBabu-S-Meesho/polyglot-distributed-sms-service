package com.meesho.smssender.dto;

public record KafkaSmsData(String mobileNumber, String message, String status) {}

