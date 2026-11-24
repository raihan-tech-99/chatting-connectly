import { Injectable, signal } from '@angular/core';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';
import { User, WebRtcSignalMessage } from '../models/connectly.models';

export type CallState = 'IDLE' | 'CALLING' | 'INCOMING' | 'CONNECTED' | 'ENDED';

@Injectable({
  providedIn: 'root'
})
export class WebRtcService {
  // Call state signals
  readonly callState = signal<CallState>('IDLE');
  readonly callType = signal<'AUDIO' | 'VIDEO'>('VIDEO');
  readonly remoteUser = signal<User | null>(null);
  readonly callId = signal<string | null>(null);
  readonly isMuted = signal<boolean>(false);
  readonly isVideoOff = signal<boolean>(false);
  readonly callDuration = signal<number>(0);
  readonly connectionStatus = signal<string>('Ready');
  readonly networkNotice = signal<string | null>(null);

  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private durationInterval: any = null;
  private iceCandidateQueue: RTCIceCandidateInit[] = [];

  // Video element references
  private localVideoElement: HTMLVideoElement | null = null;
  private remoteVideoElement: HTMLVideoElement | null = null;

  constructor(private chatService: ChatService, private authService: AuthService) {
    // Register signaling handler with ChatService
    this.chatService.onSignalReceived = (signal: WebRtcSignalMessage) => {
      this.handleIncomingSignal(signal);
    };
  }

  setVideoElements(local: HTMLVideoElement, remote: HTMLVideoElement): void {
    this.localVideoElement = local;
    this.remoteVideoElement = remote;

    if (this.localStream && this.localVideoElement) {
      this.localVideoElement.srcObject = this.localStream;
    }
    if (this.remoteStream && this.remoteVideoElement) {
      this.remoteVideoElement.srcObject = this.remoteStream;
    }
  }

  getIceServers(): RTCIceServer[] {
    const servers: RTCIceServer[] = [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' }
    ];

    // Check for custom TURN configuration in localStorage
    const turnUrl = localStorage.getItem('connectly_turn_url');
    const turnUsername = localStorage.getItem('connectly_turn_username');
    const turnCred = localStorage.getItem('connectly_turn_credential');

    if (turnUrl) {
      servers.push({
        urls: turnUrl,
        username: turnUsername || undefined,
        credential: turnCred || undefined
      });
    }

    return servers;
  }

  async startCall(targetUser: User, type: 'AUDIO' | 'VIDEO'): Promise<void> {
    if (this.callState() !== 'IDLE') return;

    this.callType.set(type);
    this.remoteUser.set(targetUser);
    this.callState.set('CALLING');
    this.connectionStatus.set('Ringing...');
    this.isMuted.set(false);
    this.isVideoOff.set(false);
    this.callDuration.set(0);

    try {
      await this.acquireLocalMedia(type);

      const currentUser = this.authService.currentUser();
      const signal: WebRtcSignalMessage = {
        type: 'CALL_REQUEST',
        callerId: currentUser?.id,
        callerUsername: currentUser?.username,
        callerDisplayName: currentUser?.displayName,
        callerAvatarUrl: currentUser?.avatarUrl,
        receiverId: targetUser.id,
        receiverUsername: targetUser.username,
        callType: type
      };

      this.chatService.sendCallSignal(signal);
    } catch (err: any) {
      console.error('Error accessing media devices', err);
      this.networkNotice.set('Unable to access camera or microphone: ' + (err.message || 'Permission denied'));
      this.endCall();
    }
  }

  async acceptCall(): Promise<void> {
    if (this.callState() !== 'INCOMING' || !this.remoteUser()) return;

    try {
      await this.acquireLocalMedia(this.callType());
      this.createPeerConnection();

      const currentUser = this.authService.currentUser();
      const signal: WebRtcSignalMessage = {
        type: 'CALL_ACCEPTED',
        callId: this.callId() || undefined,
        callerId: this.remoteUser()?.id,
        receiverId: currentUser?.id,
        receiverUsername: currentUser?.username,
        callType: this.callType()
      };

      this.chatService.sendCallSignal(signal);
      this.callState.set('CONNECTED');
      this.connectionStatus.set('Connecting media...');
      this.startTimer();
    } catch (err: any) {
      console.error('Failed to accept call', err);
      this.networkNotice.set('Could not initialize media: ' + (err.message || 'Media error'));
      this.rejectCall();
    }
  }

  rejectCall(): void {
    if (!this.remoteUser()) return;

    const currentUser = this.authService.currentUser();
    const signal: WebRtcSignalMessage = {
      type: 'CALL_REJECTED',
      callId: this.callId() || undefined,
      callerId: this.remoteUser()?.id,
      receiverId: currentUser?.id,
      receiverUsername: currentUser?.username,
      callType: this.callType()
    };

    this.chatService.sendCallSignal(signal);
    this.cleanupCall();
  }

  endCall(): void {
    const remote = this.remoteUser();
    const currentUser = this.authService.currentUser();

    if (remote && currentUser) {
      const signal: WebRtcSignalMessage = {
        type: 'CALL_ENDED',
        callId: this.callId() || undefined,
        callerId: currentUser.id,
        receiverId: remote.id,
        callType: this.callType()
      };
      this.chatService.sendCallSignal(signal);
    }

    this.cleanupCall();
  }

  toggleMute(): void {
    if (this.localStream) {
      const audioTracks = this.localStream.getAudioTracks();
      if (audioTracks.length > 0) {
        const nextState = !this.isMuted();
        audioTracks.forEach(track => (track.enabled = !nextState));
        this.isMuted.set(nextState);
      }
    }
  }

  toggleVideo(): void {
    if (this.localStream && this.callType() === 'VIDEO') {
      const videoTracks = this.localStream.getVideoTracks();
      if (videoTracks.length > 0) {
        const nextState = !this.isVideoOff();
        videoTracks.forEach(track => (track.enabled = !nextState));
        this.isVideoOff.set(nextState);
      }
    }
  }

  private async acquireLocalMedia(type: 'AUDIO' | 'VIDEO'): Promise<void> {
    const constraints: MediaStreamConstraints = {
      audio: true,
      video: type === 'VIDEO' ? { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: 'user' } : false
    };

    this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
    if (this.localVideoElement) {
      this.localVideoElement.srcObject = this.localStream;
      this.localVideoElement.muted = true; // prevent local feedback echo
    }
  }

  private createPeerConnection(): void {
    if (this.peerConnection) {
      this.peerConnection.close();
    }

    this.peerConnection = new RTCPeerConnection({
      iceServers: this.getIceServers()
    });

    this.remoteStream = new MediaStream();
    if (this.remoteVideoElement) {
      this.remoteVideoElement.srcObject = this.remoteStream;
    }

    // Add local tracks to peer connection
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => {
        this.peerConnection?.addTrack(track, this.localStream!);
      });
    }

    // Handle remote media track reception
    this.peerConnection.ontrack = (event: RTCTrackEvent) => {
      event.streams[0].getTracks().forEach(track => {
        this.remoteStream?.addTrack(track);
      });
      if (this.remoteVideoElement && this.remoteVideoElement.srcObject !== this.remoteStream) {
        this.remoteVideoElement.srcObject = this.remoteStream;
      }
    };

    // Handle ICE candidates
    this.peerConnection.onicecandidate = (event: RTCPeerConnectionIceEvent) => {
      if (event.candidate && this.remoteUser()) {
        const signal: WebRtcSignalMessage = {
          type: 'ICE_CANDIDATE',
          callId: this.callId() || undefined,
          callerId: this.authService.currentUser()?.id,
          receiverId: this.remoteUser()?.id,
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid || undefined,
          sdpMLineIndex: event.candidate.sdpMLineIndex ?? undefined
        };
        this.chatService.sendCallSignal(signal);
      }
    };

    // Handle connection state changes
    this.peerConnection.oniceconnectionstatechange = () => {
      const state = this.peerConnection?.iceConnectionState;
      if (state === 'connected' || state === 'completed') {
        this.connectionStatus.set('Connected (WebRTC P2P)');
        this.networkNotice.set(null);
      } else if (state === 'failed') {
        this.connectionStatus.set('Connection Failed');
        this.networkNotice.set(
          'Direct P2P connection could not be established across NAT/firewalls. In strict network environments, configure a TURN relay server.'
        );
      } else if (state === 'disconnected') {
        this.connectionStatus.set('Disconnected');
      }
    };
  }

  private async handleIncomingSignal(signal: WebRtcSignalMessage): Promise<void> {
    const type = signal.type;

    if (type === 'CALL_REQUEST') {
      if (this.callState() !== 'IDLE') {
        // Busy with another call
        const busySignal: WebRtcSignalMessage = {
          type: 'CALL_BUSY',
          callId: signal.callId,
          callerId: signal.callerId,
          receiverId: this.authService.currentUser()?.id
        };
        this.chatService.sendCallSignal(busySignal);
        return;
      }

      this.callId.set(signal.callId || null);
      this.callType.set((signal.callType as 'AUDIO' | 'VIDEO') || 'VIDEO');
      this.remoteUser.set({
        id: signal.callerId || '',
        username: signal.callerUsername || 'Unknown',
        displayName: signal.callerDisplayName || signal.callerUsername || 'Caller',
        avatarUrl: signal.callerAvatarUrl
      });
      this.callState.set('INCOMING');
      this.connectionStatus.set('Incoming call...');

    } else if (type === 'CALL_ACCEPTED') {
      this.callId.set(signal.callId || null);
      this.callState.set('CONNECTED');
      this.connectionStatus.set('Connecting media...');
      this.startTimer();

      // Caller creates PeerConnection and creates SDP Offer
      this.createPeerConnection();
      try {
        const offer = await this.peerConnection!.createOffer();
        await this.peerConnection!.setLocalDescription(offer);

        const offerSignal: WebRtcSignalMessage = {
          type: 'OFFER',
          callId: this.callId() || undefined,
          callerId: this.authService.currentUser()?.id,
          receiverId: this.remoteUser()?.id,
          sdp: offer.sdp
        };
        this.chatService.sendCallSignal(offerSignal);
      } catch (e) {
        console.error('Failed to create SDP offer', e);
      }

    } else if (type === 'OFFER') {
      if (!this.peerConnection) {
        this.createPeerConnection();
      }

      try {
        await this.peerConnection!.setRemoteDescription(new RTCSessionDescription({
          type: 'offer',
          sdp: signal.sdp
        }));

        // Flush queued candidates
        while (this.iceCandidateQueue.length > 0) {
          const candidate = this.iceCandidateQueue.shift();
          if (candidate) {
            await this.peerConnection!.addIceCandidate(new RTCIceCandidate(candidate));
          }
        }

        const answer = await this.peerConnection!.createAnswer();
        await this.peerConnection!.setLocalDescription(answer);

        const answerSignal: WebRtcSignalMessage = {
          type: 'ANSWER',
          callId: this.callId() || undefined,
          callerId: this.authService.currentUser()?.id,
          receiverId: this.remoteUser()?.id,
          sdp: answer.sdp
        };
        this.chatService.sendCallSignal(answerSignal);
      } catch (e) {
        console.error('Failed to handle SDP offer', e);
      }

    } else if (type === 'ANSWER') {
      if (this.peerConnection && signal.sdp) {
        try {
          await this.peerConnection.setRemoteDescription(new RTCSessionDescription({
            type: 'answer',
            sdp: signal.sdp
          }));

          while (this.iceCandidateQueue.length > 0) {
            const candidate = this.iceCandidateQueue.shift();
            if (candidate) {
              await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
            }
          }
        } catch (e) {
          console.error('Failed to set remote answer description', e);
        }
      }

    } else if (type === 'ICE_CANDIDATE') {
      if (signal.candidate) {
        const candidateInit: RTCIceCandidateInit = {
          candidate: signal.candidate,
          sdpMid: signal.sdpMid,
          sdpMLineIndex: signal.sdpMLineIndex
        };

        if (this.peerConnection && this.peerConnection.remoteDescription) {
          try {
            await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidateInit));
          } catch (e) {
            console.error('Failed to add received ICE candidate', e);
          }
        } else {
          this.iceCandidateQueue.push(candidateInit);
        }
      }

    } else if (type === 'CALL_REJECTED' || type === 'CALL_BUSY') {
      this.connectionStatus.set(type === 'CALL_BUSY' ? 'User is busy' : 'Call declined');
      setTimeout(() => this.cleanupCall(), 2000);

    } else if (type === 'CALL_ENDED') {
      this.connectionStatus.set('Call ended');
      this.cleanupCall();
    }
  }

  private startTimer(): void {
    if (this.durationInterval) {
      clearInterval(this.durationInterval);
    }
    this.callDuration.set(0);
    this.durationInterval = setInterval(() => {
      this.callDuration.update(d => d + 1);
    }, 1000);
  }

  private cleanupCall(): void {
    if (this.durationInterval) {
      clearInterval(this.durationInterval);
      this.durationInterval = null;
    }

    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;
    }

    if (this.remoteStream) {
      this.remoteStream.getTracks().forEach(track => track.stop());
      this.remoteStream = null;
    }

    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }

    if (this.localVideoElement) {
      this.localVideoElement.srcObject = null;
    }
    if (this.remoteVideoElement) {
      this.remoteVideoElement.srcObject = null;
    }

    this.iceCandidateQueue = [];
    this.callState.set('IDLE');
    this.remoteUser.set(null);
    this.callId.set(null);
    this.connectionStatus.set('Ready');
  }
}
