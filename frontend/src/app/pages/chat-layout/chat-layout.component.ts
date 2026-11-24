import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  signal,
  computed,
  effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { WebRtcService } from '../../services/webrtc.service';
import { ThemeService } from '../../services/theme.service';
import { Conversation, User, Message, CallHistory } from '../../models/connectly.models';
import { CallOverlayComponent } from '../../components/call-overlay/call-overlay.component';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, CallOverlayComponent],
  templateUrl: './chat-layout.component.html',
  styleUrls: ['./chat-layout.component.scss']
})
export class ChatLayoutComponent implements OnInit, OnDestroy {
  @ViewChild('messagesScroll') messagesScroll!: ElementRef<HTMLDivElement>;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  // UI state
  readonly sidebarTab = signal<'CHATS' | 'CALLS'>('CHATS');
  readonly searchQuery = signal<string>('');
  readonly searchResults = signal<User[]>([]);
  readonly isSearching = signal<boolean>(false);
  readonly showEmojiPicker = signal<boolean>(false);
  readonly showProfileModal = signal<boolean>(false);
  readonly isUploading = signal<boolean>(false);
  readonly mobileShowChat = signal<boolean>(false);

  // Message input state
  messageInput = '';
  private typingTimeout: any = null;

  // Profile modal edit fields
  profileDisplayName = '';
  profileStatusMessage = '';
  profileAvatarUrl = '';

  // Emojis list
  readonly emojis = [
    '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇',
    '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😋', '😜', '🤪',
    '😎', '🤩', '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🥺',
    '😭', '😤', '😡', '🤯', '😱', '🤫', '🤔', '🤐', '😴', '😷',
    '👍', '👎', '👏', '🙌', '🤝', '🙏', '❤️', '🔥', '✨', '🎉'
  ];

  constructor(
    public authService: AuthService,
    public chatService: ChatService,
    public webrtcService: WebRtcService,
    public themeService: ThemeService
  ) {
    // Auto-scroll when new messages arrive
    effect(() => {
      const msgs = this.chatService.messages();
      if (msgs) {
        setTimeout(() => this.scrollToBottom(), 50);
      }
    });
  }

  ngOnInit(): void {
    this.chatService.connectWebSocket();
    this.chatService.loadConversations().subscribe();
    this.chatService.loadCallHistory().subscribe();

    const current = this.authService.currentUser();
    if (current) {
      this.profileDisplayName = current.displayName || '';
      this.profileStatusMessage = current.statusMessage || '';
      this.profileAvatarUrl = current.avatarUrl || '';
    }
  }

  ngOnDestroy(): void {
    this.chatService.disconnectWebSocket();
  }

  onSearchChange(): void {
    const query = this.searchQuery().trim();
    if (!query) {
      this.searchResults.set([]);
      this.isSearching.set(false);
      return;
    }

    this.isSearching.set(true);
    this.chatService.searchUsers(query).subscribe({
      next: (users) => {
        this.searchResults.set(users);
      },
      error: () => {
        this.searchResults.set([]);
      }
    });
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchResults.set([]);
    this.isSearching.set(false);
  }

  startChatWithUser(user: User): void {
    this.chatService.startDirectChat(user.id).subscribe(() => {
      this.clearSearch();
      this.mobileShowChat.set(true);
    });
  }

  selectConversation(conv: Conversation): void {
    this.chatService.selectConversation(conv);
    this.mobileShowChat.set(true);
  }

  backToSidebar(): void {
    this.mobileShowChat.set(false);
  }

  onInputKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    } else {
      this.handleTyping();
    }
  }

  handleTyping(): void {
    this.chatService.sendTyping(true);
    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }
    this.typingTimeout = setTimeout(() => {
      this.chatService.sendTyping(false);
    }, 2500);
  }

  sendMessage(): void {
    const content = this.messageInput.trim();
    if (!content) return;

    this.chatService.sendMessage(content, 'TEXT');
    this.messageInput = '';
    this.chatService.sendTyping(false);
    this.showEmojiPicker.set(false);
    this.scrollToBottom();
  }

  appendEmoji(emoji: string): void {
    this.messageInput += emoji;
    this.handleTyping();
  }

  toggleEmojiPicker(): void {
    this.showEmojiPicker.update(v => !v);
  }

  triggerFileUpload(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.isUploading.set(true);

    this.chatService.uploadFile(file).subscribe({
      next: (uploaded) => {
        this.isUploading.set(false);
        const isImage = file.type.startsWith('image/');
        const messageType = isImage ? 'IMAGE' : 'FILE';
        this.chatService.sendMessage(
          isImage ? 'Shared an image' : `Shared file: ${uploaded.name}`,
          messageType,
          uploaded.url,
          uploaded.name,
          uploaded.size
        );
        input.value = '';
      },
      error: (err) => {
        this.isUploading.set(false);
        alert('File upload failed: ' + (err.error?.message || err.message));
        input.value = '';
      }
    });
  }

  startAudioCall(): void {
    const participant = this.chatService.activeConversation()?.participant;
    if (participant) {
      this.webrtcService.startCall(participant, 'AUDIO');
    }
  }

  startVideoCall(): void {
    const participant = this.chatService.activeConversation()?.participant;
    if (participant) {
      this.webrtcService.startCall(participant, 'VIDEO');
    }
  }

  callUserFromHistory(item: CallHistory): void {
    const targetUser: User = {
      id: item.outgoing ? item.receiverId : item.callerId,
      username: item.outgoing ? item.receiverUsername : item.callerUsername,
      displayName: item.outgoing ? item.receiverDisplayName : item.callerDisplayName,
      avatarUrl: item.outgoing ? item.receiverAvatarUrl : item.callerAvatarUrl
    };
    this.webrtcService.startCall(targetUser, item.callType);
  }

  openProfileModal(): void {
    const current = this.authService.currentUser();
    if (current) {
      this.profileDisplayName = current.displayName || '';
      this.profileStatusMessage = current.statusMessage || '';
      this.profileAvatarUrl = current.avatarUrl || '';
    }
    this.showProfileModal.set(true);
  }

  saveProfile(): void {
    const current = this.authService.currentUser();
    if (!current) return;

    const updatedUser: User = {
      ...current,
      displayName: this.profileDisplayName.trim() || current.displayName,
      statusMessage: this.profileStatusMessage.trim(),
      avatarUrl: this.profileAvatarUrl.trim()
    };
    this.authService.updateCurrentUser(updatedUser);
    this.showProfileModal.set(false);
  }

  formatTime(isoString?: string): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  formatDateHeader(isoString?: string): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    const today = new Date();
    if (date.toDateString() === today.toDateString()) {
      return 'Today';
    }
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday';
    }
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  }

  private scrollToBottom(): void {
    if (this.messagesScroll) {
      try {
        const el = this.messagesScroll.nativeElement;
        el.scrollTop = el.scrollHeight;
      } catch (err) {}
    }
  }
}
