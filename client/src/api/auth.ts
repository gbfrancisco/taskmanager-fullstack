/**
 * Auth API - Functions for authentication API calls
 *
 * This module provides typed functions for authentication operations.
 * Unlike other API modules, auth responses are managed by AuthContext,
 * not TanStack Query (no caching needed for auth state).
 *
 * @see server/src/main/java/com/tutorial/taskmanager/controller/AuthController.java
 */

import { get, post } from './client';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User
} from '../types/api';

/**
 * Login with username and password
 *
 * POST /api/auth/login
 *
 * @param credentials - Username and password
 * @returns Auth response with JWT token and user info
 * @throws ApiClientError with status 401 if credentials are invalid
 */
export function loginUser(credentials: LoginRequest): Promise<AuthResponse> {
  return post<AuthResponse>('/api/auth/login', credentials);
}

/**
 * Register a new user account
 *
 * POST /api/auth/register
 *
 * @param data - Registration data (username, email, password)
 * @returns Auth response with JWT token and user info
 * @throws ApiClientError with status 400 if username/email already exists
 */
export function registerUser(data: RegisterRequest): Promise<AuthResponse> {
  return post<AuthResponse>('/api/auth/register', data);
}

/**
 * Get current authenticated user
 *
 * GET /api/auth/me
 *
 * This endpoint requires a valid JWT token in the Authorization header.
 * Used to validate stored tokens on app startup.
 *
 * @returns The authenticated user's info
 * @throws ApiClientError with status 401 if token is invalid/expired
 */
export function getCurrentUser(): Promise<User> {
  return get<User>('/api/auth/me');
}
