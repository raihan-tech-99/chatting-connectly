package com.connectly.service;

import com.connectly.config.JwtTokenProvider;
import com.connectly.config.UserPrincipal;
import com.connectly.dto.AuthResponse;
import com.connectly.dto.LoginRequest;
import com.connectly.dto.RegisterRequest;
import com.connectly.entity.User;
import com.connectly.exception.BadRequestException;
import com.connectly.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim().toLowerCase())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim().toLowerCase());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setIsOnline(true);
        user.setLastSeenAt(Instant.now());

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateToken(savedUser.getUsername(), savedUser.getId());

        userService.broadcastUserStatus(savedUser.getId(), savedUser.getUsername(), true, savedUser.getLastSeenAt());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                savedUser.getAvatarUrl(),
                savedUser.getStatusMessage()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        user.setIsOnline(true);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getId());

        userService.broadcastUserStatus(user.getId(), user.getUsername(), true, user.getLastSeenAt());

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getStatusMessage()
        );
    }

    @Transactional
    public void logout(UserPrincipal principal) {
        if (principal != null) {
            User user = principal.getUser();
            user.setIsOnline(false);
            user.setLastSeenAt(Instant.now());
            userRepository.save(user);

            userService.broadcastUserStatus(user.getId(), user.getUsername(), false, user.getLastSeenAt());
        }
    }
}
