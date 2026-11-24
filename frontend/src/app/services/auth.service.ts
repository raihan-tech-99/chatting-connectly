import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models/connectly.models';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'connectly_token';
  private readonly USER_KEY = 'connectly_user';
  private readonly API_BASE = '/api/auth';

  readonly currentUser = signal<User | null>(this.getStoredUser());
  readonly token = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));
  readonly isAuthenticated = computed(() => !!this.token() && !!this.currentUser());

  constructor(private http: HttpClient, private router: Router) {}

  register(data: { username: string; email: string; password: string; displayName: string; avatarUrl?: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_BASE}/register`, data).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  login(data: { username: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_BASE}/login`, data).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  logout(): void {
    if (this.token()) {
      this.http.post(`${this.API_BASE}/logout`, {}).subscribe({
        error: () => {} // ignore error on logout
      });
    }
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  updateCurrentUser(user: User): void {
    this.currentUser.set(user);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  private handleAuthSuccess(res: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    const user: User = {
      id: res.id,
      username: res.username,
      email: res.email,
      displayName: res.displayName,
      avatarUrl: res.avatarUrl,
      statusMessage: res.statusMessage,
      isOnline: true
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.token.set(res.token);
    this.currentUser.set(user);
  }

  private getStoredUser(): User | null {
    const raw = localStorage.getItem(this.USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }
}
