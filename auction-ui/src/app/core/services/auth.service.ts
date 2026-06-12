import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models/auth-response.model';
import { AuthenticatedUser } from '../models/authenticated-user.model';
import { LoginRequest } from '../models/login-request.model';
import { RegisterRequest } from '../models/register-request.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private static readonly TOKEN_KEY = 'auction.auth.token';
  private static readonly USER_KEY = 'auction.auth.user';

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  readonly currentUser$ = new BehaviorSubject<AuthenticatedUser | null>(this.readStoredUser());
  readonly isAuthenticated$ = new BehaviorSubject<boolean>(!!this.readStoredToken());

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  me(): Observable<AuthenticatedUser> {
    return this.http.get<AuthenticatedUser>(`${this.baseUrl}/me`).pipe(
      tap((user) => {
        this.currentUser$.next(user);
        localStorage.setItem(AuthService.USER_KEY, JSON.stringify(user));
        this.isAuthenticated$.next(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(AuthService.TOKEN_KEY);
    localStorage.removeItem(AuthService.USER_KEY);
    this.currentUser$.next(null);
    this.isAuthenticated$.next(false);
    void this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.readStoredToken();
  }

  getCurrentUser(): AuthenticatedUser | null {
    return this.currentUser$.value;
  }

  getCurrentUserId(): number | null {
    return this.currentUser$.value?.id ?? null;
  }

  isAdmin(): boolean {
    return this.currentUser$.value?.role === 'ADMIN';
  }

  updateCurrentUserNames(firstName: string | null, lastName: string | null): void {
    const currentUser = this.currentUser$.value;
    if (!currentUser) {
      return;
    }

    const updatedUser: AuthenticatedUser = {
      ...currentUser,
      firstName,
      lastName
    };
    localStorage.setItem(AuthService.USER_KEY, JSON.stringify(updatedUser));
    this.currentUser$.next(updatedUser);
  }

  private persistSession(response: AuthResponse): void {
    localStorage.setItem(AuthService.TOKEN_KEY, response.accessToken);
    localStorage.setItem(AuthService.USER_KEY, JSON.stringify(response.user));
    this.currentUser$.next(response.user);
    this.isAuthenticated$.next(true);
  }

  private readStoredToken(): string | null {
    return localStorage.getItem(AuthService.TOKEN_KEY);
  }

  private readStoredUser(): AuthenticatedUser | null {
    const raw = localStorage.getItem(AuthService.USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthenticatedUser;
    } catch {
      localStorage.removeItem(AuthService.USER_KEY);
      return null;
    }
  }
}
