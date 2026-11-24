export interface User {
  id: string;
  username: string;
  email?: string;
  displayName: string;
  avatarUrl?: string;
  statusMessage?: string;
  isOnline?: boolean;
  lastSeenAt?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  id: string;
  username: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  statusMessage?: string;
}

export interface Conversation {
  id: string;
  conversationType: string;
  createdAt: string;
  lastMessageAt?: string;
  lastMessagePreview?: string;
  participant?: User;
  unreadCount: number;
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername: string;
  senderDisplayName: string;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE';
  mediaUrl?: string;
  mediaName?: string;
  mediaSize?: number;
  status: 'SENT' | 'DELIVERED' | 'READ';
  createdAt: string;
  deliveredAt?: string;
  readAt?: string;
}

export interface TypingEvent {
  conversationId: string;
  userId: string;
  username: string;
  typing: boolean;
}

export interface UserStatusEvent {
  userId: string;
  username: string;
  online: boolean;
  lastSeenAt?: string;
}

export interface MessageStatusEvent {
  messageId?: string;
  conversationId: string;
  recipientId: string;
  status: 'DELIVERED' | 'READ';
  timestamp: string;
}

export interface WebRtcSignalMessage {
  type: 'CALL_REQUEST' | 'CALL_ACCEPTED' | 'CALL_REJECTED' | 'CALL_BUSY' | 'CALL_ENDED' | 'OFFER' | 'ANSWER' | 'ICE_CANDIDATE';
  callId?: string;
  callerId?: string;
  callerUsername?: string;
  callerDisplayName?: string;
  callerAvatarUrl?: string;
  receiverId?: string;
  receiverUsername?: string;
  callType?: 'AUDIO' | 'VIDEO';
  sdp?: string;
  candidate?: string;
  sdpMid?: string;
  sdpMLineIndex?: number;
  timestamp?: string;
}

export interface CallHistory {
  id: string;
  callerId: string;
  callerUsername: string;
  callerDisplayName: string;
  callerAvatarUrl?: string;
  receiverId: string;
  receiverUsername: string;
  receiverDisplayName: string;
  receiverAvatarUrl?: string;
  callType: 'AUDIO' | 'VIDEO';
  status: 'INITIATED' | 'ACCEPTED' | 'REJECTED' | 'MISSED' | 'COMPLETED' | 'BUSY';
  startedAt: string;
  endedAt?: string;
  durationSeconds: number;
  outgoing: boolean;
}
