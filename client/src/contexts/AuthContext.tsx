/**
 * Authentication Context
 *
 * Provides global authentication state and methods to the entire app.
 * Uses React Context for state management and the mockAuth service for persistence.
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
import * as mockAuth from '@/lib/mockAuth';
import type { AuthUser } from '@/lib/mockAuth';

// =============================================================================
// TYPES
// =============================================================================

/**
 * The shape of the auth context value.
 * This is what you get when you call useAuth().
 */
export interface AuthContextType {
  /** The currently authenticated user, or null if not logged in */
  user: AuthUser | null;

  /** Convenience boolean: true if user is logged in */
  isAuthenticated: boolean;

  /** True during initial session check on app load */
  isLoading: boolean;

  /**
   * Log in with username and password.
   * @throws Error if credentials are invalid
   */
  login: (username: string, password: string) => Promise<void>;

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
 * On mount, it checks localStorage for an existing session.
 */
export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // ---------------------------------------------------------------------------
  // INITIAL SESSION CHECK
  // ---------------------------------------------------------------------------

  useEffect(() => {
    // Check for existing session on mount
    // getCurrentUser() is synchronous (reads localStorage)
    const existingUser = mockAuth.getCurrentUser();
    setUser(existingUser);
    setIsLoading(false);
  }, []);

  // ---------------------------------------------------------------------------
  // AUTH METHODS
  // ---------------------------------------------------------------------------

  /**
   * Login handler - validates credentials and updates state
   */
  const login = async (username: string, password: string) => {
    const loggedInUser = await mockAuth.login({ username, password });
    setUser(loggedInUser);
  };

  /**
   * Register handler - creates user, auto-logs in, updates state
   */
  const register = async (username: string, email: string, password: string) => {
    const newUser = await mockAuth.register({ username, email, password });
    setUser(newUser);
  };

  /**
   * Logout handler - clears session and updates state
   */
  const logout = async () => {
    await mockAuth.logout();
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
