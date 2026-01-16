package com.meesho.smssender.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.smssender.dto.BlockRequest;
import com.meesho.smssender.dto.SmsRequest;
import com.meesho.smssender.exception.BlockedNumberException;
import com.meesho.smssender.exception.ValidationException;
import com.meesho.smssender.service.SmsService;

@WebMvcTest(SmsController.class)
class SmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SmsService smsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendSms_shouldReturnAccepted_whenRequestIsValid() throws Exception {
        SmsRequest request = new SmsRequest("1234567890", "Hello");

        mockMvc.perform(post("/v1/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ENQUEUED"));
    }

    @Test
    void sendSms_shouldReturnForbidden_whenUserIsBlocked() throws Exception {
        SmsRequest request = new SmsRequest("9999999999", "Hello");
        
        doThrow(new BlockedNumberException("The number is blocked"))
            .when(smsService).sendSms(anyString(), anyString());

        mockMvc.perform(post("/v1/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()) // 403
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void sendSms_shouldReturnBadRequest_whenValidationFails() throws Exception {
        SmsRequest request = new SmsRequest("123", "Hello"); // Invalid number
        
        doThrow(new ValidationException("Mobile number must be exactly 10 digits."))
            .when(smsService).sendSms(anyString(), anyString());

        mockMvc.perform(post("/v1/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void blockUser_shouldReturnOk_whenUserBlockedSuccessfully() throws Exception {
        BlockRequest request = new BlockRequest("1234567890");
        when(smsService.blockUser("1234567890")).thenReturn(1L);

        mockMvc.perform(post("/v1/sms/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void blockUser_shouldReturnAccepted_whenUserAlreadyBlocked() throws Exception {
        BlockRequest request = new BlockRequest("1234567890");
        when(smsService.blockUser("1234567890")).thenReturn(0L);

        mockMvc.perform(post("/v1/sms/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ALREADY_BLOCKED"));
    }
}