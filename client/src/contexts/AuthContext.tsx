/**
 * Authentication Context
 *
 * Provides global authentication state and methods to the entire app.
 * Uses React Context for state management and JWT tokens for persistence.
 *
 * USAGE:
 * 1. Wrap your app with <AuthProvider>
 * 2. Access auth state with useAuth() hook
 *
 * @example
 * ```tsx
 * // In a component:
 * function ProfileButton() {
 *   const { user, isAuthenticated, logout } = useAuth();
 *
 *   if (!isAuthenticated) {
 *     return <Link to="/login">Login</Link>;
 *   }
 *
 *   return (
 *     <div>
 *       <span>{user.username}</span>
 *       <button onClick={logout}>Logout</button>
 *     </div>
 *   );
 * }
 * ```
 */

import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode
} from 'react';
import { loginUser, registerUser, getCurrentUser } from '@/api/auth';
import { getToken, setToken, removeToken } from '@/lib/tokenStorage';
import type { User } from '@/types/api';

// =============================================================================
// TYPES
// =============================================================================

/**
 * The shape of the auth context value.
 * This is what you get when you call useAuth().
 */
export interface AuthContextType {
  /** The currently authenticated user, or null if not logged in */
  user: User | null;

  /** Convenience boolean: true if user is logged in */
  isAuthenticated: boolean;

  /** True during initial session check on app load */
  isLoading: boolean;

  /**
   * Log in with username or email and password.
   * @throws Error if credentials are invalid
   */
  login: (usernameOrEmail: string, password: string) => Promise<void>;

  /**
   * Register a new user and auto-login.
   * @throws Error if username/email already taken
   */
  register: (username: string, email: string, password: string) => Promise<void>;

  /** Log out the current user */
  logout: () => Promise<void>;
}

// =============================================================================
// CONTEXT
// =============================================================================

/**
 * The React Context for auth.
 * Starts as null - must be used within AuthProvider.
 */
const AuthContext = createContext<AuthContextType | null>(null);

// =============================================================================
// PROVIDER
// =============================================================================

interface AuthProviderProps {
  children: ReactNode;
}

/**
 * AuthProvider Component
 *
 * Wraps your app to provide authentication state and methods.
 * Must be placed high in the component tree (typically in main.tsx).
 *
 * On mount, it checks for a stored JWT token and validates it
 * by calling /api/auth/me on the backend.
 */
export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // ---------------------------------------------------------------------------
  // INITIAL SESSION CHECK
  // ---------------------------------------------------------------------------

  useEffect(() => {
    // Check for existing token on mount and validate it
    async function validateToken() {
      const token = getToken();

      if (!token) {
        // No token stored - user is not logged in
        setIsLoading(false);
        return;
      }

      try {
        // Token exists - validate it by calling /api/auth/me
        // The API client will automatically include the token in the header
        const currentUser = await getCurrentUser();
        setUser(currentUser);
      } catch {
        // Token is invalid or expired - clear it
        removeToken();
      } finally {
        setIsLoading(false);
      }
    }

    validateToken();
  }, []);

  // ---------------------------------------------------------------------------
  // AUTH METHODS
  // ---------------------------------------------------------------------------

  /**
   * Login handler - validates credentials, stores token, updates state
   */
  const login = async (usernameOrEmail: string, password: string) => {
    const response = await loginUser({ usernameOrEmail, password });
    setToken(response.token);
    setUser(response.user);
  };

  /**
   * Register handler - creates user, stores token, updates state
   */
  const register = async (username: string, email: string, password: string) => {
    const response = await registerUser({ username, email, password });
    setToken(response.token);
    setUser(response.user);
  };

  /**
   * Logout handler - clears token and updates state
   */
  const logout = async () => {
    removeToken();
    setUser(null);
  };

  // ---------------------------------------------------------------------------
  // CONTEXT VALUE
  // ---------------------------------------------------------------------------

  const value: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    register,
    logout
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// =============================================================================
// HOOK
// =============================================================================

/**
 * useAuth Hook
 *
 * Access authentication state and methods from any component.
 * Must be used within an AuthProvider.
 *
 * @returns AuthContextType with user, isAuthenticated, login, logout, etc.
 * @throws Error if used outside of AuthProvider
 *
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { user, isAuthenticated, logout } = useAuth();
 *
 *   if (!isAuthenticated) {
 *     return <p>Please log in</p>;
 *   }
 *
 *   return <p>Welcome, {user.username}!</p>;
 * }
 * ```
 */
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      'useAuth must be used within an AuthProvider. ' +
        'Make sure your component is wrapped in <AuthProvider>.'
    );
  }

  return context;
}
