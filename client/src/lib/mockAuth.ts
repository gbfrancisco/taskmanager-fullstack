/**
 * Mock Authentication Service
 *
 * Simulates authentication using localStorage for a learning project.
 * This allows the frontend to have a complete auth flow without backend changes.
 *
 * IMPORTANT: This is for LEARNING/DEMO purposes only.
 * In a real app, authentication would be handled by the backend with:
 * - Password hashing (bcrypt)
 * - JWT or session tokens
 * - Secure HTTP-only cookies
 * - HTTPS
 *
 * Features:
 * - Login with username/password validation
 * - Registration with duplicate checking
 * - Session persistence across page refreshes
 * - Simulated network delay for realistic UX
 */

// =============================================================================
// TYPES
// =============================================================================

/**
 * User data returned after authentication.
 * Note: Password is never included in this type.
 */
export interface AuthUser {
  id: number;
  username: string;
  email: string;
}

/**
 * Credentials for login
 */
export interface LoginCredentials {
  username: string;
  password: string;
}

/**
 * Data for registration
 */
export interface RegisterData {
  username: string;
  email: string;
  password: string;
}

/**
 * Internal type for stored users (includes password)
 * In a real app, passwords would be hashed server-side
 */
interface StoredUser extends AuthUser {
  password: string;
}

// =============================================================================
// CONSTANTS
// =============================================================================

/** localStorage key for current authenticated user */
const AUTH_STORAGE_KEY = 'taskmanager_auth';

/** localStorage key for registered users "database" */
const USERS_STORAGE_KEY = 'taskmanager_users';

/** Default users available on first load */
const DEFAULT_USERS: StoredUser[] = [
  {
    id: 1,
    username: 'demo',
    email: 'demo@example.com',
    password: 'password123'
  }
];

// =============================================================================
// HELPERS
// =============================================================================

/**
 * Simulate network delay for realistic UX.
 * In a real app, this delay would be actual network latency.
 */
async function delay(ms: number = 500): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Get all registered users from localStorage.
 * Initializes with default users if none exist.
 */
function getStoredUsers(): StoredUser[] {
  const stored = localStorage.getItem(USERS_STORAGE_KEY);
  if (stored) {
    return JSON.parse(stored);
  }
  // Initialize with default users on first access
  localStorage.setItem(USERS_STORAGE_KEY, JSON.stringify(DEFAULT_USERS));
  return DEFAULT_USERS;
}

/**
 * Save users array to localStorage
 */
function saveUsers(users: StoredUser[]): void {
  localStorage.setItem(USERS_STORAGE_KEY, JSON.stringify(users));
}

/**
 * Convert StoredUser to AuthUser (strips password)
 */
function toAuthUser(user: StoredUser): AuthUser {
  return {
    id: user.id,
    username: user.username,
    email: user.email
  };
}

// =============================================================================
// AUTH FUNCTIONS
// =============================================================================

/**
 * Login with username and password.
 *
 * @param credentials - Username and password
 * @returns AuthUser on success
 * @throws Error if credentials are invalid
 *
 * @example
 * ```ts
 * try {
 *   const user = await login({ username: 'demo', password: 'password123' });
 *   console.log('Logged in as:', user.username);
 * } catch (err) {
 *   console.error('Login failed:', err.message);
 * }
 * ```
 */
export async function login(credentials: LoginCredentials): Promise<AuthUser> {
  await delay();

  const users = getStoredUsers();
  const user = users.find(
    (u) =>
      u.username === credentials.username && u.password === credentials.password
  );

  if (!user) {
    throw new Error('Invalid username or password');
  }

  // Store authenticated user (without password)
  const authUser = toAuthUser(user);
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authUser));

  return authUser;
}

/**
 * Register a new user.
 *
 * @param data - Registration data (username, email, password)
 * @returns AuthUser on success (auto-logged in)
 * @throws Error if username or email already exists
 *
 * @example
 * ```ts
 * try {
 *   const user = await register({
 *     username: 'newuser',
 *     email: 'new@example.com',
 *     password: 'securepass123'
 *   });
 *   console.log('Registered and logged in as:', user.username);
 * } catch (err) {
 *   console.error('Registration failed:', err.message);
 * }
 * ```
 */
export async function register(data: RegisterData): Promise<AuthUser> {
  await delay();

  const users = getStoredUsers();

  // Check for duplicate username
  if (users.some((u) => u.username.toLowerCase() === data.username.toLowerCase())) {
    throw new Error('Username already taken');
  }

  // Check for duplicate email
  if (users.some((u) => u.email.toLowerCase() === data.email.toLowerCase())) {
    throw new Error('Email already registered');
  }

  // Create new user with next available ID
  const newUser: StoredUser = {
    id: Math.max(...users.map((u) => u.id), 0) + 1,
    username: data.username,
    email: data.email,
    password: data.password
  };

  // Save to "database"
  users.push(newUser);
  saveUsers(users);

  // Auto-login after registration
  const authUser = toAuthUser(newUser);
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authUser));

  return authUser;
}

/**
 * Logout the current user.
 * Clears the session from localStorage.
 */
export async function logout(): Promise<void> {
  await delay(200);
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

/**
 * Get the currently authenticated user.
 * This is a synchronous read from localStorage, used on app initialization.
 *
 * @returns AuthUser if logged in, null otherwise
 *
 * @example
 * ```ts
 * const user = getCurrentUser();
 * if (user) {
 *   console.log('Already logged in as:', user.username);
 * }
 * ```
 */
export function getCurrentUser(): AuthUser | null {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      // Invalid JSON, clear it
      localStorage.removeItem(AUTH_STORAGE_KEY);
    }
  }
  return null;
}
