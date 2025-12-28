/**
 * Token Storage - Simple localStorage wrapper for JWT token
 *
 * Centralizes token storage operations to:
 * 1. Keep storage key consistent
 * 2. Provide type-safe access
 * 3. Make it easy to change storage mechanism later (e.g., to sessionStorage)
 *
 * SECURITY NOTE:
 * localStorage is vulnerable to XSS attacks. For production apps with
 * sensitive data, consider:
 * - HttpOnly cookies (requires backend changes)
 * - In-memory storage with refresh tokens
 * - Short token expiration times
 *
 * For this tutorial, localStorage is acceptable as it demonstrates
 * the concept clearly.
 */

const TOKEN_STORAGE_KEY = 'taskmanager_token';

/**
 * Get the stored JWT token
 *
 * @returns The token string, or null if not stored
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

/**
 * Store a JWT token
 *
 * @param token - The JWT token to store
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

/**
 * Remove the stored token (logout)
 */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

/**
 * Check if a token is stored
 *
 * @returns true if a token exists
 */
export function hasToken(): boolean {
  return getToken() !== null;
}
