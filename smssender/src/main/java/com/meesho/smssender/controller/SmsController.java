package com.meesho.smssender.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meesho.smssender.dto.BlockRequest;
import com.meesho.smssender.dto.BlockResponse;
import com.meesho.smssender.dto.SmsRequest;
import com.meesho.smssender.dto.SmsResponse;
import com.meesho.smssender.service.SmsService;

@RestController
@RequestMapping("/v1/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/send")
    public ResponseEntity<SmsResponse> func(@RequestBody SmsRequest smsrequest) {
        smsService.sendSms(smsrequest.mobileNumber(), smsrequest.message());
        return new ResponseEntity<SmsResponse>(
            new SmsResponse("ENQUEUED", "Message enqueued for delivery"), org.springframework.http.HttpStatus.ACCEPTED
        );
    }

    @PostMapping("/block")
    public ResponseEntity<BlockResponse> blockUser(@RequestBody BlockRequest blockReq) {
        String status = (smsService.blockUser(blockReq.mobileNumber()) == 1) ? "BLOCKED" : "ALREADY_BLOCKED";
        return new ResponseEntity<BlockResponse>(
            new BlockResponse(blockReq.mobileNumber(), status), (status == "BLOCKED") ? org.springframework.http.HttpStatus.OK : org.springframework.http.HttpStatus.ACCEPTED
        );
    }

    @PostMapping("/unblock")
    public ResponseEntity<BlockResponse> unblockUser(@RequestBody BlockRequest blockReq) {
        String status = (smsService.unblockUser(blockReq.mobileNumber()) == 1) ? "UNBLOCKED" : "ALREADY_NOT_BLOCKED";
        return new ResponseEntity<BlockResponse>(
            new BlockResponse(blockReq.mobileNumber(), status), (status == "UNBLOCKED") ? org.springframework.http.HttpStatus.OK : org.springframework.http.HttpStatus.ACCEPTED
        );
    }

}
