import { Component, ElementRef, ViewChild, AfterViewInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WebRtcService } from '../../services/webrtc.service';

@Component({
  selector: 'app-call-overlay',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './call-overlay.component.html',
  styleUrls: ['./call-overlay.component.scss']
})
export class CallOverlayComponent implements AfterViewInit {
  @ViewChild('localVideo') localVideoRef!: ElementRef<HTMLVideoElement>;
  @ViewChild('remoteVideo') remoteVideoRef!: ElementRef<HTMLVideoElement>;

  readonly showTurnSettings = signal<boolean>(false);
  turnUrl = localStorage.getItem('connectly_turn_url') || '';
  turnUsername = localStorage.getItem('connectly_turn_username') || '';
  turnCredential = localStorage.getItem('connectly_turn_credential') || '';

  readonly formattedDuration = computed(() => {
    const totalSec = this.webrtcService.callDuration();
    const minutes = Math.floor(totalSec / 60);
    const seconds = totalSec % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  });

  constructor(public webrtcService: WebRtcService) {}

  ngAfterViewInit(): void {
    if (this.localVideoRef && this.remoteVideoRef) {
      this.webrtcService.setVideoElements(
        this.localVideoRef.nativeElement,
        this.remoteVideoRef.nativeElement
      );
    }
  }

  saveTurnSettings(): void {
    if (this.turnUrl.trim()) {
      localStorage.setItem('connectly_turn_url', this.turnUrl.trim());
      localStorage.setItem('connectly_turn_username', this.turnUsername.trim());
      localStorage.setItem('connectly_turn_credential', this.turnCredential.trim());
    } else {
      localStorage.removeItem('connectly_turn_url');
      localStorage.removeItem('connectly_turn_username');
      localStorage.removeItem('connectly_turn_credential');
    }
    this.showTurnSettings.set(false);
  }
}
