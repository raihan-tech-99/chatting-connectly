package com.connectly;

import com.connectly.dto.AuthResponse;
import com.connectly.dto.LoginRequest;
import com.connectly.dto.RegisterRequest;
import com.connectly.entity.User;
import com.connectly.exception.BadRequestException;
import com.connectly.repository.UserRepository;
import com.connectly.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");
        req.setDisplayName("Alice Wonderland");

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals("alice", res.getUsername());
        assertEquals("Alice Wonderland", res.getDisplayName());

        assertTrue(userRepository.findByUsername("alice").isPresent());
    }

    @Test
    void testRegisterDuplicateUsernameThrowsException() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("bob");
        req1.setEmail("bob1@example.com");
        req1.setPassword("password123");
        req1.setDisplayName("Bob One");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("bob");
        req2.setEmail("bob2@example.com");
        req2.setPassword("password123");
        req2.setDisplayName("Bob Two");

        assertThrows(BadRequestException.class, () -> authService.register(req2));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("charlie1");
        req1.setEmail("charlie@example.com");
        req1.setPassword("password123");
        req1.setDisplayName("Charlie 1");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("charlie2");
        req2.setEmail("charlie@example.com");
        req2.setPassword("password123");
        req2.setDisplayName("Charlie 2");

        assertThrows(BadRequestException.class, () -> authService.register(req2));
    }

    @Test
    void testLoginSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("david");
        req.setEmail("david@example.com");
        req.setPassword("secretPass99");
        req.setDisplayName("David D");
        authService.register(req);

        LoginRequest loginReq = new LoginRequest("david", "secretPass99");
        AuthResponse loginRes = authService.login(loginReq);

        assertNotNull(loginRes);
        assertNotNull(loginRes.getToken());
        assertEquals("david", loginRes.getUsername());
    }

    @Test
    void testLoginInvalidPasswordFails() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("eve");
        req.setEmail("eve@example.com");
        req.setPassword("correctPassword");
        req.setDisplayName("Eve E");
        authService.register(req);

        LoginRequest loginReq = new LoginRequest("eve", "wrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq));
    }
}
