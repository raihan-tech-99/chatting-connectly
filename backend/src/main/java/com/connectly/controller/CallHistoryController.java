package com.connectly.controller;

import com.connectly.config.UserPrincipal;
import com.connectly.dto.CallHistoryDto;
import com.connectly.service.CallHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
public class CallHistoryController {

    private final CallHistoryService callHistoryService;

    public CallHistoryController(CallHistoryService callHistoryService) {
        this.callHistoryService = callHistoryService;
    }

    @GetMapping("/history")
    public ResponseEntity<List<CallHistoryDto>> getCallHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(callHistoryService.getUserCallHistory(principal.getId()));
    }
}
