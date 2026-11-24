package com.connectly.controller;

import com.connectly.config.UserPrincipal;
import com.connectly.dto.UserProfileDto;
import com.connectly.dto.UpdateProfileRequest;
import com.connectly.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDto>> searchUsers(
            @RequestParam(name = "q", defaultValue = "") String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.searchUsers(query, principal.getId()));
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }
}
