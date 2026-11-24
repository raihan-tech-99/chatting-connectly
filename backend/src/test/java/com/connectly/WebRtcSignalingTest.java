package com.connectly;

import com.connectly.dto.CallHistoryDto;
import com.connectly.dto.WebRtcSignalMessage;
import com.connectly.entity.User;
import com.connectly.repository.CallHistoryRepository;
import com.connectly.repository.UserRepository;
import com.connectly.service.CallHistoryService;
import com.connectly.service.WebRtcSignalingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WebRtcSignalingTest {

    @Autowired
    private WebRtcSignalingService signalingService;

    @Autowired
    private CallHistoryService callHistoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CallHistoryRepository callHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User caller;
    private User receiver;

    @BeforeEach
    void setUp() {
        callHistoryRepository.deleteAll();
        userRepository.deleteAll();

        caller = new User();
        caller.setUsername("caller1");
        caller.setEmail("caller1@example.com");
        caller.setPasswordHash(passwordEncoder.encode("pass123"));
        caller.setDisplayName("Caller One");
        caller = userRepository.save(caller);

        receiver = new User();
        receiver.setUsername("receiver1");
        receiver.setEmail("receiver1@example.com");
        receiver.setPasswordHash(passwordEncoder.encode("pass123"));
        receiver.setDisplayName("Receiver One");
        receiver = userRepository.save(receiver);
    }

    @Test
    void testCallSignalingAndHistoryFlow() {
        // Step 1: Caller sends CALL_REQUEST
        WebRtcSignalMessage requestSignal = new WebRtcSignalMessage();
        requestSignal.setType("CALL_REQUEST");
        requestSignal.setCallerId(caller.getId());
        requestSignal.setCallerUsername(caller.getUsername());
        requestSignal.setCallerDisplayName(caller.getDisplayName());
        requestSignal.setReceiverId(receiver.getId());
        requestSignal.setCallType("VIDEO");

        signalingService.processSignal(requestSignal);

        assertNotNull(requestSignal.getCallId());

        // Verify call history was recorded
        List<CallHistoryDto> callerHistory = callHistoryService.getUserCallHistory(caller.getId());
        assertEquals(1, callerHistory.size());
        assertEquals("INITIATED", callerHistory.get(0).getStatus());
        assertTrue(callerHistory.get(0).isOutgoing());

        // Step 2: Receiver sends CALL_ACCEPTED
        WebRtcSignalMessage acceptSignal = new WebRtcSignalMessage();
        acceptSignal.setType("CALL_ACCEPTED");
        acceptSignal.setCallId(requestSignal.getCallId());
        acceptSignal.setCallerId(caller.getId());
        acceptSignal.setReceiverId(receiver.getId());
        signalingService.processSignal(acceptSignal);

        // Step 3: WebRTC Offer / Answer exchange
        WebRtcSignalMessage offer = new WebRtcSignalMessage();
        offer.setType("OFFER");
        offer.setReceiverId(receiver.getId());
        offer.setSdp("v=0\r\no=alice 2890844526 2890844526 IN IP4 127.0.0.1\r\ns=Connectly Call\r\n");
        signalingService.processSignal(offer);

        WebRtcSignalMessage answer = new WebRtcSignalMessage();
        answer.setType("ANSWER");
        answer.setReceiverId(caller.getId());
        answer.setSdp("v=0\r\no=bob 2890844526 2890844526 IN IP4 127.0.0.1\r\ns=Connectly Call\r\n");
        signalingService.processSignal(answer);

        // Step 4: ICE Candidates exchange
        WebRtcSignalMessage iceCandidate = new WebRtcSignalMessage();
        iceCandidate.setType("ICE_CANDIDATE");
        iceCandidate.setReceiverId(receiver.getId());
        iceCandidate.setCandidate("candidate:842163049 1 udp 1677729535 192.168.1.2 56143 typ host");
        iceCandidate.setSdpMid("0");
        iceCandidate.setSdpMLineIndex(0);
        signalingService.processSignal(iceCandidate);

        // Step 5: Call End
        WebRtcSignalMessage endSignal = new WebRtcSignalMessage();
        endSignal.setType("CALL_ENDED");
        endSignal.setCallId(requestSignal.getCallId());
        endSignal.setCallerId(caller.getId());
        endSignal.setReceiverId(receiver.getId());
        signalingService.processSignal(endSignal);

        // Verify updated call status
        List<CallHistoryDto> receiverHistory = callHistoryService.getUserCallHistory(receiver.getId());
        assertEquals(1, receiverHistory.size());
        assertEquals("COMPLETED", receiverHistory.get(0).getStatus());
        assertFalse(receiverHistory.get(0).isOutgoing());
    }
}
