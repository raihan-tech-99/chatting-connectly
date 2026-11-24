import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import {
  Conversation,
  Message,
  MessageStatusEvent,
  TypingEvent,
  User,
  UserStatusEvent,
  WebRtcSignalMessage,
  CallHistory
} from '../models/connectly.models';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private conversationSub: StompSubscription | null = null;
  private typingSub: StompSubscription | null = null;
  private statusSub: StompSubscription | null = null;
  private callSignalSub: StompSubscription | null = null;
  private userStatusSub: StompSubscription | null = null;

  // Live state signals
  readonly conversations = signal<Conversation[]>([]);
  readonly activeConversation = signal<Conversation | null>(null);
  readonly messages = signal<Message[]>([]);
  readonly activeTypingUser = signal<string | null>(null);
  readonly onlineUserIds = signal<Set<string>>(new Set());
  readonly callHistory = signal<CallHistory[]>([]);
  readonly isConnected = signal<boolean>(false);

  // Callback hook for WebRtcService to process signals
  onSignalReceived?: (signal: WebRtcSignalMessage) => void;

  constructor(private http: HttpClient, private authService: AuthService) {}

  connectWebSocket(): void {
    const token = this.authService.token();
    const currentUser = this.authService.currentUser();
    if (!token || !currentUser) return;

    if (this.stompClient && this.stompClient.active) {
      return; // already connected
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {} // quiet debug
    });

    this.stompClient.onConnect = () => {
      this.isConnected.set(true);

      // Subscribe to global user presence/status
      this.userStatusSub = this.stompClient?.subscribe('/topic/status', (msg: IMessage) => {
        try {
          const event: UserStatusEvent = JSON.parse(msg.body);
          this.handleUserStatusUpdate(event);
        } catch (e) {
          console.error('Error parsing user status', e);
        }
      }) || null;

      // Subscribe to personal WebRTC call signaling channel
      this.callSignalSub = this.stompClient?.subscribe(`/topic/calls/${currentUser.id}`, (msg: IMessage) => {
        try {
          const signal: WebRtcSignalMessage = JSON.parse(msg.body);
          if (this.onSignalReceived) {
            this.onSignalReceived(signal);
          }
        } catch (e) {
          console.error('Error parsing incoming call signal', e);
        }
      }) || null;

      // Re-subscribe active conversation if one is selected
      const active = this.activeConversation();
      if (active) {
        this.subscribeToConversation(active.id);
      }
    };

    this.stompClient.onDisconnect = () => {
      this.isConnected.set(false);
    };

    this.stompClient.activate();
  }

  disconnectWebSocket(): void {
    this.unsubscribeFromConversation();
    if (this.callSignalSub) {
      this.callSignalSub.unsubscribe();
      this.callSignalSub = null;
    }
    if (this.userStatusSub) {
      this.userStatusSub.unsubscribe();
      this.userStatusSub = null;
    }
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    this.isConnected.set(false);
  }

  loadConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>('/api/conversations').pipe(
      tap(convs => {
        this.conversations.set(convs);
        // initialize online status map
        const onlineSet = new Set<string>();
        convs.forEach(c => {
          if (c.participant?.isOnline) {
            onlineSet.add(c.participant.id);
          }
        });
        this.onlineUserIds.set(onlineSet);
      })
    );
  }

  selectConversation(conv: Conversation): void {
    this.activeConversation.set(conv);
    this.activeTypingUser.set(null);
    this.subscribeToConversation(conv.id);
    this.loadMessages(conv.id).subscribe();
    this.markAsRead(conv.id);
  }

  loadMessages(conversationId: string): Observable<Message[]> {
    return this.http.get<Message[]>(`/api/messages/conversation/${conversationId}`).pipe(
      tap(msgs => this.messages.set(msgs))
    );
  }

  startDirectChat(recipientId: string): Observable<Conversation> {
    return this.http.post<Conversation>('/api/conversations/direct', { recipientId }).pipe(
      tap(newConv => {
        const existing = this.conversations().find(c => c.id === newConv.id);
        if (!existing) {
          this.conversations.update(list => [newConv, ...list]);
        }
        this.selectConversation(newConv);
      })
    );
  }

  sendMessage(content: string, messageType: 'TEXT' | 'IMAGE' | 'FILE' = 'TEXT', mediaUrl?: string, mediaName?: string, mediaSize?: number): void {
    const active = this.activeConversation();
    if (!active) return;

    const payload = {
      conversationId: active.id,
      content,
      messageType,
      mediaUrl,
      mediaName,
      mediaSize
    };

    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(payload)
      });
    } else {
      this.http.post<Message>('/api/messages', payload).subscribe(msg => {
        this.appendMessageIfNew(msg);
      });
    }
  }

  sendTyping(isTyping: boolean): void {
    const active = this.activeConversation();
    if (!active || !this.stompClient || !this.stompClient.connected) return;

    const currentUser = this.authService.currentUser();
    this.stompClient.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({
        conversationId: active.id,
        typing: isTyping,
        username: currentUser?.username || ''
      })
    });
  }

  markAsRead(conversationId: string): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/chat.read',
        body: JSON.stringify({ conversationId })
      });
    } else {
      this.http.post(`/api/messages/conversation/${conversationId}/read`, {}).subscribe();
    }

    // Clear unread count locally
    this.conversations.update(list =>
      list.map(c => (c.id === conversationId ? { ...c, unreadCount: 0 } : c))
    );
  }

  sendCallSignal(signal: WebRtcSignalMessage): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/call.signal',
        body: JSON.stringify(signal)
      });
    }
  }

  searchUsers(query: string): Observable<User[]> {
    return this.http.get<User[]>(`/api/users/search?q=${encodeURIComponent(query)}`);
  }

  uploadFile(file: File): Observable<{ url: string; name: string; size: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string; name: string; size: number }>('/api/media/upload', formData);
  }

  loadCallHistory(): Observable<CallHistory[]> {
    return this.http.get<CallHistory[]>('/api/calls/history').pipe(
      tap(history => this.callHistory.set(history))
    );
  }

  private subscribeToConversation(conversationId: string): void {
    this.unsubscribeFromConversation();
    if (!this.stompClient || !this.stompClient.connected) return;

    // Messages subscription
    this.conversationSub = this.stompClient.subscribe(`/topic/conversations/${conversationId}`, (msg: IMessage) => {
      try {
        const message: Message = JSON.parse(msg.body);
        this.appendMessageIfNew(message);
        this.updateConversationPreview(conversationId, message);
      } catch (e) {
        console.error('Error parsing incoming message', e);
      }
    });

    // Typing indicators subscription
    this.typingSub = this.stompClient.subscribe(`/topic/conversations/${conversationId}/typing`, (msg: IMessage) => {
      try {
        const event: TypingEvent = JSON.parse(msg.body);
        const currentUser = this.authService.currentUser();
        if (event.userId !== currentUser?.id) {
          this.activeTypingUser.set(event.typing ? event.username : null);
        }
      } catch (e) {
        console.error('Error parsing typing event', e);
      }
    });

    // Read receipt status updates
    this.statusSub = this.stompClient.subscribe(`/topic/conversations/${conversationId}/status`, (msg: IMessage) => {
      try {
        const statusEvent: MessageStatusEvent = JSON.parse(msg.body);
        if (statusEvent.status === 'READ') {
          this.messages.update(msgs =>
            msgs.map(m => (m.senderId === this.authService.currentUser()?.id ? { ...m, status: 'READ' } : m))
          );
        }
      } catch (e) {
        console.error('Error parsing status event', e);
      }
    });
  }

  private unsubscribeFromConversation(): void {
    if (this.conversationSub) {
      this.conversationSub.unsubscribe();
      this.conversationSub = null;
    }
    if (this.typingSub) {
      this.typingSub.unsubscribe();
      this.typingSub = null;
    }
    if (this.statusSub) {
      this.statusSub.unsubscribe();
      this.statusSub = null;
    }
  }

  private appendMessageIfNew(msg: Message): void {
    const active = this.activeConversation();
    if (active && active.id === msg.conversationId) {
      this.messages.update(msgs => {
        if (!msgs.some(m => m.id === msg.id)) {
          return [...msgs, msg];
        }
        return msgs;
      });

      // If received from someone else while viewing, mark as read
      if (msg.senderId !== this.authService.currentUser()?.id) {
        this.markAsRead(msg.conversationId);
      }
    }
  }

  private updateConversationPreview(conversationId: string, msg: Message): void {
    const preview = msg.messageType === 'TEXT' ? msg.content : `[${msg.messageType}]`;
    this.conversations.update(list => {
      return list.map(c => {
        if (c.id === conversationId) {
          const isActive = this.activeConversation()?.id === conversationId;
          const unreadInc = (!isActive && msg.senderId !== this.authService.currentUser()?.id) ? 1 : 0;
          return {
            ...c,
            lastMessagePreview: preview,
            lastMessageAt: msg.createdAt,
            unreadCount: c.unreadCount + unreadInc
          };
        }
        return c;
      }).sort((a, b) => new Date(b.lastMessageAt || 0).getTime() - new Date(a.lastMessageAt || 0).getTime());
    });
  }

  private handleUserStatusUpdate(event: UserStatusEvent): void {
    this.onlineUserIds.update(set => {
      const next = new Set(set);
      if (event.online) {
        next.add(event.userId);
      } else {
        next.delete(event.userId);
      }
      return next;
    });

    this.conversations.update(list =>
      list.map(c => {
        if (c.participant && c.participant.id === event.userId) {
          return {
            ...c,
            participant: {
              ...c.participant,
              isOnline: event.online,
              lastSeenAt: event.lastSeenAt
            }
          };
        }
        return c;
      })
    );
  }
}
