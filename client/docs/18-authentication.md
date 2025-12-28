# 18 - Authentication in React

This document covers frontend authentication patterns: React Context for global state, protected routes with TanStack Router, and session persistence.

## Table of Contents
- [Authentication Architecture](#authentication-architecture)
- [React Context for Global State](#react-context-for-global-state)
- [Creating an Auth Context](#creating-an-auth-context)
- [Integrating Auth with TanStack Router](#integrating-auth-with-tanstack-router)
- [Protected Routes](#protected-routes)
- [Login Flow with Redirects](#login-flow-with-redirects)
- [Session Persistence](#session-persistence)
  - [Blocking Router Until Auth Initializes](#blocking-router-until-auth-initializes-critical-pattern)
- [Mock Authentication Pattern](#mock-authentication-pattern)
- [JWT Authentication Integration](#jwt-authentication-integration)
- [HttpOnly Cookie Authentication (Future)](#httponly-cookie-authentication-future)
- [Implementation Reference](#implementation-reference)

---

## Authentication Architecture

Frontend authentication typically involves these pieces:

```
┌─────────────────────────────────────────────────────────────────┐
│                        React App                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    AuthProvider                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                  AuthContext                         │  │  │
│  │  │  • user: AuthUser | null                            │  │  │
│  │  │  • isAuthenticated: boolean                         │  │  │
│  │  │  • login(username, password)                        │  │  │
│  │  │  • register(username, email, password)              │  │  │
│  │  │  • logout()                                         │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                           │                                │  │
│  │           ┌───────────────┼───────────────┐               │  │
│  │           ▼               ▼               ▼               │  │
│  │       Header          Routes          Components          │  │
│  │    (shows user)    (beforeLoad)     (useAuth())          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              ▼                                   │
│                      ┌──────────────┐                           │
│                      │ localStorage │  ← Session persistence     │
│                      └──────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

**Key Components:**

| Component | Responsibility |
|-----------|---------------|
| **AuthContext** | Holds auth state (user, isAuthenticated) |
| **AuthProvider** | Wraps app, provides context value |
| **useAuth()** | Hook to access auth state in components |
| **Auth Service** | Handles login/logout/register logic |
| **localStorage** | Persists session across page refreshes |
| **beforeLoad** | Route guards that check auth before rendering |

---

## React Context for Global State

### Why Context for Auth?

Authentication state is **truly global** - it's needed everywhere:
- Header (show username, logout button)
- Protected routes (check if authenticated)
- Forms (get current user ID)
- API calls (attach auth token)

React Context is perfect for this because:
1. **No prop drilling** - Any component can access auth
2. **Single source of truth** - One place for auth state
3. **Built into React** - No extra dependencies
4. **Triggers re-renders** - Components update when auth changes

### Context vs Other Solutions

| Solution | Best For | Auth Use Case |
|----------|----------|---------------|
| **React Context** | Global UI state | Yes - simple, built-in |
| **TanStack Query** | Server state (cached data) | No - auth isn't server state |
| **Zustand/Redux** | Complex client state | Overkill for just auth |
| **Props** | Local component state | No - too much drilling |

---

## Creating an Auth Context

### Step 1: Define the Context Type

First, define what data and methods the context provides:

```tsx
// What the context contains
interface AuthContextType {
  // State
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Methods
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

// User data (never includes password)
interface AuthUser {
  id: number;
  username: string;
  email: string;
}
```

### Step 2: Create the Context

```tsx
import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

// Create context with null default (must be used within provider)
const AuthContext = createContext<AuthContextType | null>(null);
```

**Why `null` default?**

Using `null` ensures components crash early if they use `useAuth()` outside the provider. This catches bugs during development rather than causing silent failures.

### Step 3: Create the Provider Component

The provider holds state and provides methods:

```tsx
interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Check for existing session on mount
  useEffect(() => {
    const existingUser = getCurrentUserFromStorage();
    setUser(existingUser);
    setIsLoading(false);
  }, []);

  // Login handler
  const login = async (username: string, password: string) => {
    const loggedInUser = await authService.login({ username, password });
    setUser(loggedInUser);
  };

  // Register handler
  const register = async (username: string, email: string, password: string) => {
    const newUser = await authService.register({ username, email, password });
    setUser(newUser);
  };

  // Logout handler
  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  // Provide value to children
  const value: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    register,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
```

**Key Patterns:**

1. **`isAuthenticated` is derived** - It's just `user !== null`, not separate state
2. **`isLoading` for initial check** - Prevents flash of wrong UI on page load
3. **Methods update state** - `login` sets user, `logout` clears it
4. **Side effects in handlers** - Storage/API calls happen in login/logout

### Step 4: Create the Consumer Hook

```tsx
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
```

**Why throw an error?**

This catches the common mistake of using `useAuth()` outside the provider. The error message tells developers exactly how to fix it.

### Step 5: Wrap Your App

```tsx
// main.tsx
root.render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>
);
```

### Step 6: Use in Components

```tsx
function Header() {
  const { user, isAuthenticated, logout } = useAuth();

  if (!isAuthenticated) {
    return <Link to="/login">Login</Link>;
  }

  return (
    <div>
      <span>Welcome, {user.username}</span>
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

---

## Integrating Auth with TanStack Router

TanStack Router has its own context system for passing data to routes. We need to connect our Auth Context to the Router Context.

### The Challenge

```
AuthProvider (React Context)
     │
     ▼
RouterProvider (Router Context)
     │
     ▼
Routes (need auth from React Context, but only have Router Context)
```

Routes can only access **Router Context** in `beforeLoad`, not React Context. We need to pass auth through the router.

### Solution: Pass Auth to Router Context

**Step 1: Update Router Context Interface**

```tsx
// routes/__root.tsx
import type { QueryClient } from '@tanstack/react-query';
import type { AuthContextType } from '@/contexts/AuthContext';

interface MyRouterContext {
  queryClient: QueryClient;
  auth: AuthContextType;  // Add auth to router context
}

export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: RootLayout
});
```

**Step 2: Create Router Inside React Tree**

The router needs access to auth context, but `createRouter()` runs at module level (before React renders). Solution: create the router inside a component.

```tsx
// main.tsx
import { AuthProvider, useAuth } from './contexts/AuthContext';

// Create router at module level for type registration
const router = createRouter({
  routeTree,
  context: {
    queryClient,
    auth: undefined!  // Placeholder - will be provided by InnerApp
  }
});

// Type registration (must be at module level)
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

// Component that connects auth to router
function InnerApp() {
  const auth = useAuth();  // Now we have access to auth context

  return (
    <RouterProvider
      router={router}
      context={{
        queryClient,
        auth  // Pass real auth to router
      }}
    />
  );
}

// Render with AuthProvider outside InnerApp
root.render(
  <StrictMode>
    <AuthProvider>
      <QueryProvider>
        <InnerApp />
      </QueryProvider>
    </AuthProvider>
  </StrictMode>
);
```

**Why This Pattern?**

1. `createRouter()` must happen at module level for TypeScript type registration
2. But `useAuth()` only works inside `<AuthProvider>`
3. Solution: Create router with placeholder, override context in `<InnerApp>`
4. `RouterProvider` accepts a `context` prop that overrides the initial context

---

## Protected Routes

### Using `beforeLoad` for Route Guards

TanStack Router's `beforeLoad` runs **before** the route component renders. Perfect for auth checks:

```tsx
import { createFileRoute, redirect } from '@tanstack/react-router';

export const Route = createFileRoute('/dashboard')({
  beforeLoad: ({ context }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({ to: '/login' });
    }
  },
  component: DashboardPage
});
```

### How `beforeLoad` Works

```
User navigates to /dashboard
         │
         ▼
┌─────────────────────┐
│    beforeLoad()     │  ← Runs FIRST
│  Check auth state   │
└─────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
Authenticated?   Not authenticated?
    │              │
    ▼              ▼
┌─────────┐   ┌───────────────────┐
│ loader  │   │ throw redirect()  │
│ (fetch) │   │ → goes to /login  │
└─────────┘   └───────────────────┘
    │
    ▼
┌───────────┐
│ component │
│ (render)  │
└───────────┘
```

**Key Benefits:**

1. **Runs before loader** - Doesn't waste API calls for unauthenticated users
2. **Type-safe context** - `context.auth` is fully typed
3. **Declarative** - Auth logic lives with the route definition
4. **Redirect with `throw`** - Stops execution and redirects immediately

### Preserving the Intended Destination

When redirecting to login, save where the user was going:

```tsx
beforeLoad: ({ context, location }) => {
  if (!context.auth.isAuthenticated) {
    throw redirect({
      to: '/login',
      search: { redirect: location.pathname }  // Save destination
    });
  }
}
```

This creates URLs like `/login?redirect=/dashboard`.

### Guarding Multiple Routes

Apply the same guard to all protected routes:

```tsx
// routes/tasks/index.tsx
export const Route = createFileRoute('/tasks/')({
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname }
      });
    }
  },
  component: TasksPage
});

// routes/projects/index.tsx
export const Route = createFileRoute('/projects/')({
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname }
      });
    }
  },
  component: ProjectsPage
});
```

**Note:** In larger apps, you might create a helper function or use route middleware to avoid repetition.

---

## Login Flow with Redirects

### Validating Search Parameters

TanStack Router can validate URL search params with Zod:

```tsx
import { createFileRoute, redirect, useNavigate } from '@tanstack/react-router';
import { z } from 'zod';

// Define expected search params
const loginSearchSchema = z.object({
  redirect: z.string().optional()
});

export const Route = createFileRoute('/login')({
  // Validate and parse search params
  validateSearch: loginSearchSchema,

  // Redirect away if already logged in
  beforeLoad: ({ context, search }) => {
    if (context.auth.isAuthenticated) {
      throw redirect({ to: search.redirect || '/' });
    }
  },

  component: LoginPage
});
```

### Accessing Search Params in Components

```tsx
function LoginPage() {
  const navigate = useNavigate();
  const { redirect: redirectTo } = Route.useSearch();  // Typed!

  function handleLoginSuccess() {
    // Go to saved destination or home
    navigate({ to: redirectTo || '/' });
  }

  return <LoginForm onSuccess={handleLoginSuccess} />;
}
```

### Complete Login Flow

```
User clicks "Tasks" (not authenticated)
         │
         ▼
/tasks beforeLoad checks auth
         │
         ▼
throw redirect({ to: '/login', search: { redirect: '/tasks' } })
         │
         ▼
Browser shows /login?redirect=/tasks
         │
         ▼
User enters credentials, clicks "Sign In"
         │
         ▼
LoginForm calls auth.login(username, password)
         │
         ▼
On success, calls onSuccess callback
         │
         ▼
navigate({ to: redirectTo || '/' })  // redirectTo = '/tasks'
         │
         ▼
Browser shows /tasks (now authenticated)
```

---

## Session Persistence

### Why Persist Sessions?

Without persistence, users would need to log in again on every page refresh. We store auth state in `localStorage` to survive browser refreshes.

### Storage Strategy

```tsx
const AUTH_STORAGE_KEY = 'myapp_auth';

// Save user to storage (on login/register)
function saveSession(user: AuthUser): void {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
}

// Load user from storage (on app start)
function loadSession(): AuthUser | null {
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

// Clear storage (on logout)
function clearSession(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}
```

### Checking Session on App Load

The AuthProvider checks for an existing session when the app starts:

```tsx
function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);  // Start loading

  useEffect(() => {
    // Synchronous read from localStorage
    const existingUser = loadSession();
    setUser(existingUser);
    setIsLoading(false);  // Done loading
  }, []);

  // ... rest of provider
}
```

**Why `isLoading`?**

Without it, there's a brief moment where:
1. App renders with `user = null`
2. Protected routes redirect to login
3. Then session loads and user is actually authenticated

The `isLoading` state lets you show a loading spinner or delay rendering until the session check completes.

### Blocking Router Until Auth Initializes (Critical Pattern)

The `isLoading` state must be handled **before** the router renders. This is the industry standard pattern used by Auth0, Clerk, NextAuth, and other auth libraries.

**The Problem:**

```tsx
// ❌ WRONG - Router renders immediately
function InnerApp() {
  const auth = useAuth();
  // On first render: isLoading=true, isAuthenticated=false
  // Router evaluates routes with incorrect auth state!
  return <RouterProvider router={router} context={{ auth }} />;
}
```

This causes a race condition:
1. App starts, `isLoading=true`, `isAuthenticated=false`
2. Router immediately evaluates `beforeLoad` on routes
3. `beforeLoad` sees `isAuthenticated=false` → redirects to login (wrong!)
4. OR loader runs and makes API calls without a token → 401 errors

**The Solution:**

```tsx
// ✅ CORRECT - Block until auth is known
function InnerApp() {
  const auth = useAuth();

  // Don't render router until auth state is known
  if (auth.isLoading) {
    return (
      <div className="loading-screen">
        <Spinner />
        <p>Loading...</p>
      </div>
    );
  }

  // Now isLoading=false, isAuthenticated is accurate
  return <RouterProvider router={router} context={{ auth }} />;
}
```

**Why this works:**
- Router never sees `isLoading=true`
- `beforeLoad` always has accurate `isAuthenticated` state
- No race conditions between auth init and route evaluation

**Flow diagram:**

```
App Starts
    │
    ▼
AuthProvider initializes
(isLoading=true, user=null)
    │
    ▼
InnerApp checks isLoading ──────► true? Show loading spinner
    │                                    (Router NOT rendered)
    │
    ▼
Token validation completes
(isLoading=false, user=data or null)
    │
    ▼
InnerApp re-renders
    │
    ▼
isLoading=false ──────────────► Render RouterProvider
    │
    ▼
Routes evaluate with correct auth state
    │
    ├─── isAuthenticated=true ──► beforeLoad passes, loader runs
    │
    └─── isAuthenticated=false ─► beforeLoad redirects to /login
```

### localStorage vs sessionStorage vs Cookies

| Storage | Persistence | Use Case |
|---------|------------|----------|
| **localStorage** | Until cleared | "Remember me" sessions |
| **sessionStorage** | Until tab closes | Single-session apps |
| **HTTP-only Cookie** | Server-controlled | Production auth (most secure) |

For learning/mock auth, `localStorage` is fine. Production apps typically use HTTP-only cookies set by the server (not accessible to JavaScript, preventing XSS attacks).

### JWT Token Storage (Production)

When using JWT authentication with a backend, you store the **token** instead of the user object:

```tsx
// lib/tokenStorage.ts
const TOKEN_KEY = 'myapp_token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}
```

**Key difference from mock auth:**
- Mock auth stores the entire user object (synchronous read)
- JWT auth stores only the token, then validates with the backend (async)

```tsx
// JWT session check (async - requires API call)
useEffect(() => {
  async function validateToken() {
    const token = getToken();
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      const user = await getCurrentUser();  // GET /api/auth/me
      setUser(user);
    } catch {
      removeToken();  // Invalid/expired token
    } finally {
      setIsLoading(false);
    }
  }
  validateToken();
}, []);
```

See [JWT Authentication Integration](#jwt-authentication-integration) for full implementation details.

---

## Mock Authentication Pattern

For learning or prototyping, you can simulate authentication without a backend.

### Mock Auth Service

```tsx
// lib/mockAuth.ts

interface StoredUser {
  id: number;
  username: string;
  email: string;
  password: string;  // In real app, NEVER store passwords client-side!
}

const USERS_KEY = 'myapp_users';
const AUTH_KEY = 'myapp_auth';

// Default users for demo
const DEFAULT_USERS: StoredUser[] = [
  { id: 1, username: 'demo', email: 'demo@example.com', password: 'password123' }
];

// Simulate network delay
async function delay(ms = 500): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Get "database" of users
function getUsers(): StoredUser[] {
  const stored = localStorage.getItem(USERS_KEY);
  if (stored) return JSON.parse(stored);

  // Initialize with defaults
  localStorage.setItem(USERS_KEY, JSON.stringify(DEFAULT_USERS));
  return DEFAULT_USERS;
}

// Login
export async function login(credentials: {
  username: string;
  password: string;
}): Promise<AuthUser> {
  await delay();  // Simulate network

  const users = getUsers();
  const user = users.find(
    u => u.username === credentials.username &&
         u.password === credentials.password
  );

  if (!user) {
    throw new Error('Invalid username or password');
  }

  // Store session (without password!)
  const authUser = { id: user.id, username: user.username, email: user.email };
  localStorage.setItem(AUTH_KEY, JSON.stringify(authUser));

  return authUser;
}

// Register
export async function register(data: {
  username: string;
  email: string;
  password: string;
}): Promise<AuthUser> {
  await delay();

  const users = getUsers();

  // Check for duplicates
  if (users.some(u => u.username === data.username)) {
    throw new Error('Username already taken');
  }
  if (users.some(u => u.email === data.email)) {
    throw new Error('Email already registered');
  }

  // Create user
  const newUser: StoredUser = {
    id: Math.max(...users.map(u => u.id)) + 1,
    username: data.username,
    email: data.email,
    password: data.password
  };

  users.push(newUser);
  localStorage.setItem(USERS_KEY, JSON.stringify(users));

  // Auto-login
  const authUser = { id: newUser.id, username: newUser.username, email: newUser.email };
  localStorage.setItem(AUTH_KEY, JSON.stringify(authUser));

  return authUser;
}

// Logout
export async function logout(): Promise<void> {
  await delay(200);
  localStorage.removeItem(AUTH_KEY);
}

// Get current user (sync, for initial load)
export function getCurrentUser(): AuthUser | null {
  const stored = localStorage.getItem(AUTH_KEY);
  return stored ? JSON.parse(stored) : null;
}
```

### When to Use Mock Auth

| Scenario | Use Mock Auth? |
|----------|---------------|
| Learning React patterns | Yes |
| Prototyping UI | Yes |
| Frontend-only demo | Yes |
| Production app | No - use real backend |
| Security-sensitive app | No - never |

**Important:** Mock auth stores passwords in plain text in localStorage. This is for learning only. Real apps must:
- Hash passwords on the server (bcrypt)
- Use HTTPS
- Store tokens in HTTP-only cookies
- Implement CSRF protection

---

## JWT Authentication Integration

Once you have a backend with JWT authentication, you replace the mock auth service with real API calls. This section shows how to integrate JWT authentication while keeping the same React Context structure.

### What Changes from Mock Auth?

| Aspect | Mock Auth | JWT Auth |
|--------|-----------|----------|
| **Storage** | User object in localStorage | JWT token in localStorage |
| **Login response** | Returns user directly | Returns token + user |
| **Session validation** | Read stored user | Call `/api/auth/me` with token |
| **API requests** | No auth headers | Include `Authorization: Bearer <token>` |

### Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                         React App                                     │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                     AuthProvider                                │  │
│  │  ┌──────────────────────────────────────────────────────────┐  │  │
│  │  │                   AuthContext                             │  │  │
│  │  │  • user: User | null                                     │  │  │
│  │  │  • isAuthenticated: boolean                              │  │  │
│  │  │  • login(username, password) → calls API                 │  │  │
│  │  │  • register(username, email, password) → calls API       │  │  │
│  │  │  • logout() → clears token                               │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                              │                                        │
│              ┌───────────────┼───────────────┐                       │
│              ▼               ▼               ▼                       │
│          API Client      Routes          Components                   │
│       (adds Bearer)   (beforeLoad)      (useAuth)                    │
│              │                                                        │
│              ▼                                                        │
│     ┌─────────────────┐                                              │
│     │ Token Storage   │  ← JWT stored here                           │
│     │ (localStorage)  │                                              │
│     └─────────────────┘                                              │
│              │                                                        │
│              ▼                                                        │
│     ┌─────────────────┐                                              │
│     │  Backend API    │  ← Validates token on each request           │
│     └─────────────────┘                                              │
└──────────────────────────────────────────────────────────────────────┘
```

### Step 1: Define Auth Types

Create TypeScript types matching your backend DTOs:

```tsx
// types/api.ts

// Request for POST /api/auth/login
interface LoginRequest {
  username: string;
  password: string;
}

// Request for POST /api/auth/register
interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

// Response from login and register endpoints
interface AuthResponse {
  token: string;        // JWT token
  tokenType: string;    // "Bearer"
  expiresIn: number;    // seconds until expiration
  user: User;           // user info
}

// User info (same as before)
interface User {
  id: number;
  username: string;
  email: string;
}
```

### Step 2: Create Token Storage

Centralize token storage operations:

```tsx
// lib/tokenStorage.ts

const TOKEN_STORAGE_KEY = 'myapp_token';

// Get the stored JWT token
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

// Store a JWT token
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

// Remove the stored token (logout)
export function removeToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

// Check if a token is stored
export function hasToken(): boolean {
  return getToken() !== null;
}
```

**Why a separate module?**
- Single source of truth for the storage key
- Easy to change storage mechanism (e.g., sessionStorage)
- Type-safe access throughout the app

### Step 3: Create Auth API Functions

Create API functions for authentication endpoints:

```tsx
// api/auth.ts
import { get, post } from './client';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types/api';

// POST /api/auth/login
export function loginUser(credentials: LoginRequest): Promise<AuthResponse> {
  return post<AuthResponse>('/api/auth/login', credentials);
}

// POST /api/auth/register
export function registerUser(data: RegisterRequest): Promise<AuthResponse> {
  return post<AuthResponse>('/api/auth/register', data);
}

// GET /api/auth/me - Validate token and get current user
export function getCurrentUser(): Promise<User> {
  return get<User>('/api/auth/me');
}
```

### Step 4: Add Authorization Header to API Client

The API client automatically includes the JWT token in all requests:

```tsx
// api/client.ts
import { getToken } from '../lib/tokenStorage';

function buildHeaders(includeContentType = false): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: 'application/json'
  };

  if (includeContentType) {
    headers['Content-Type'] = 'application/json';
  }

  // Add Authorization header if token exists
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
}

// Use buildHeaders in all request functions
export async function get<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'GET',
    headers: buildHeaders()  // ← Includes Bearer token automatically
  });
  return handleResponse<T>(response);
}

export async function post<T>(endpoint: string, data: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'POST',
    headers: buildHeaders(true),  // ← Content-Type + Bearer token
    body: JSON.stringify(data)
  });
  return handleResponse<T>(response);
}
```

**Key Point:** Every API request automatically includes the JWT token. No need to manually add headers in components.

### Step 5: Update AuthContext for Real API

Replace mock auth calls with real API calls:

```tsx
// contexts/AuthContext.tsx
import { loginUser, registerUser, getCurrentUser } from '@/api/auth';
import { getToken, setToken, removeToken } from '@/lib/tokenStorage';
import type { User } from '@/types/api';

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Validate existing token on app load
  useEffect(() => {
    async function validateToken() {
      const token = getToken();

      if (!token) {
        // No token stored - user is not logged in
        setIsLoading(false);
        return;
      }

      try {
        // Token exists - validate by calling /api/auth/me
        // The API client automatically includes the token
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

  // Login handler
  const login = async (username: string, password: string) => {
    const response = await loginUser({ username, password });
    setToken(response.token);     // Store the JWT
    setUser(response.user);       // Update state
  };

  // Register handler
  const register = async (username: string, email: string, password: string) => {
    const response = await registerUser({ username, email, password });
    setToken(response.token);     // Store the JWT
    setUser(response.user);       // Update state
  };

  // Logout handler
  const logout = async () => {
    removeToken();                // Clear the JWT
    setUser(null);                // Update state
  };

  // ... rest of provider
}
```

### Token Validation Flow

```
App Starts
    │
    ▼
Check localStorage for token
    │
    ├─── No token found ──────► setIsLoading(false), user stays null
    │
    └─── Token found ─────────► Call GET /api/auth/me
                                     │
                         ┌───────────┴───────────┐
                         ▼                       ▼
                    Success (200)           Error (401)
                         │                       │
                         ▼                       ▼
                  setUser(response)        removeToken()
                  setIsLoading(false)      setIsLoading(false)
```

### Differences from Mock Auth

| Mock Auth | JWT Auth |
|-----------|----------|
| `mockAuth.login()` validates locally | `loginUser()` calls backend |
| Stores user object in localStorage | Stores JWT token in localStorage |
| `mockAuth.getCurrentUser()` reads localStorage | `getCurrentUser()` calls `/api/auth/me` |
| No network requests | Real HTTP requests |
| `AuthUser` from mock | `User` from backend DTO |

### Security Considerations

**localStorage for JWT tokens:**
- Vulnerable to XSS attacks (malicious scripts can read tokens)
- Acceptable for learning/development
- Production apps should consider:
  - HttpOnly cookies (set by backend)
  - Short token expiration + refresh tokens
  - Content Security Policy headers

**Token expiration:**
- Backend sets expiration in the JWT
- When token expires, `/api/auth/me` returns 401
- AuthContext clears the invalid token
- User is redirected to login

### Testing JWT Integration

1. **Registration flow:**
   - Register a new user
   - Verify token is stored in localStorage
   - Refresh page - should stay logged in

2. **Login flow:**
   - Login with credentials
   - Check network tab - see POST to `/api/auth/login`
   - Verify subsequent requests include `Authorization: Bearer ...`

3. **Session persistence:**
   - Login, then refresh the page
   - Check network tab - see GET to `/api/auth/me`
   - Verify user is still logged in

4. **Logout:**
   - Click logout
   - Verify token is removed from localStorage
   - Verify protected routes redirect to login

---

## HttpOnly Cookie Authentication (Future)

> **Status**: Not yet implemented. This section documents the production-recommended approach for future reference.

HttpOnly cookies are the **industry standard for production authentication**. Unlike localStorage, cookies with the HttpOnly flag cannot be accessed by JavaScript, making them immune to XSS attacks.

### Why HttpOnly Cookies?

| Aspect | localStorage | HttpOnly Cookie |
|--------|--------------|-----------------|
| **XSS vulnerability** | Yes - any script can read token | No - invisible to JavaScript |
| **CSRF vulnerability** | No | Yes - requires CSRF protection |
| **Token handling** | Manual (Authorization header) | Automatic (browser sends cookie) |
| **Frontend complexity** | More code | Less code |
| **Logout** | Client removes token | Server invalidates cookie |

**The trade-off**: HttpOnly cookies protect against XSS but introduce CSRF risk. However, CSRF is easier to mitigate (SameSite attribute, CSRF tokens) than XSS.

### Architecture Comparison

**Current (localStorage):**
```
Login Response:
{ "token": "eyJ...", "user": {...} }

Frontend stores token, adds to every request:
Authorization: Bearer eyJ...
```

**HttpOnly Cookie:**
```
Login Response:
Set-Cookie: token=eyJ...; HttpOnly; Secure; SameSite=Strict; Path=/
{ "user": {...} }

Browser automatically sends cookie with every request.
Frontend never sees the token.
```

### Backend Changes Required (Spring Boot)

**1. Return cookie instead of token in response body:**

```java
@PostMapping("/login")
public ResponseEntity<UserResponse> login(
    @RequestBody LoginRequest request,
    HttpServletResponse response
) {
    // Authenticate and generate token
    String token = jwtService.generateToken(user);

    // Set HttpOnly cookie
    Cookie cookie = new Cookie("token", token);
    cookie.setHttpOnly(true);        // JavaScript cannot access
    cookie.setSecure(true);          // HTTPS only
    cookie.setPath("/");             // Sent with all requests
    cookie.setMaxAge(86400);         // 24 hours
    cookie.setAttribute("SameSite", "Strict");  // CSRF protection
    response.addCookie(cookie);

    // Return user info only (no token in body)
    return ResponseEntity.ok(new UserResponse(user));
}
```

**2. Read token from cookie instead of Authorization header:**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) {
        // Extract token from cookie (not header)
        String token = extractTokenFromCookie(request);

        if (token != null && jwtService.isTokenValid(token)) {
            // Set authentication...
        }

        chain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

**3. Logout clears the cookie:**

```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(HttpServletResponse response) {
    Cookie cookie = new Cookie("token", "");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);  // Delete cookie
    response.addCookie(cookie);

    return ResponseEntity.noContent().build();
}
```

### Frontend Changes Required

**1. API client becomes simpler:**

```tsx
// No more token handling!
// api/client.ts

function buildHeaders(includeContentType = false): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: 'application/json'
  };

  if (includeContentType) {
    headers['Content-Type'] = 'application/json';
  }

  // No Authorization header needed - cookie is automatic
  return headers;
}

// Important: Enable credentials for cookies to be sent
export async function get<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'GET',
    headers: buildHeaders(),
    credentials: 'include'  // ← Required for cookies
  });
  return handleResponse<T>(response);
}
```

**2. Remove tokenStorage.ts entirely:**

```tsx
// DELETE: src/lib/tokenStorage.ts
// No longer needed - browser manages the cookie
```

**3. Simplified AuthContext:**

```tsx
// contexts/AuthContext.tsx

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Check session on mount - no token to check, just call API
  useEffect(() => {
    async function checkSession() {
      try {
        // Cookie is sent automatically
        const currentUser = await getCurrentUser();
        setUser(currentUser);
      } catch {
        // Not authenticated (no valid cookie)
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    }
    checkSession();
  }, []);

  const login = async (username: string, password: string) => {
    // Server sets the cookie, we just get user back
    const response = await loginUser({ username, password });
    setUser(response.user);
  };

  const logout = async () => {
    // Server clears the cookie
    await logoutUser();  // POST /api/auth/logout
    setUser(null);
  };

  // ...
}
```

**4. Auth API changes:**

```tsx
// api/auth.ts

// Login no longer returns token
interface LoginResponse {
  user: User;  // No token field
}

export function loginUser(credentials: LoginRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/api/auth/login', credentials);
}

// New logout endpoint (server clears cookie)
export function logoutUser(): Promise<void> {
  return post<void>('/api/auth/logout', {});
}
```

### CORS Configuration for Cookies

When using cookies across origins, CORS must be configured to allow credentials:

**Backend (Spring Boot):**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")  // Specific origin required
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true);  // ← Required for cookies
    }
}
```

**Note**: When `allowCredentials(true)`, you cannot use `allowedOrigins("*")`. You must specify exact origins.

### Cookie Attributes Reference

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `HttpOnly` | true | Prevents JavaScript access (XSS protection) |
| `Secure` | true | Only sent over HTTPS |
| `SameSite` | Strict | Only sent with same-site requests (CSRF protection) |
| `Path` | / | Sent with all requests to the domain |
| `MaxAge` | seconds | Cookie expiration (omit for session cookie) |

### When to Implement

Consider migrating to HttpOnly cookies when:
- Moving to production
- Handling sensitive user data
- Security audit requires it
- You need server-controlled session invalidation

For learning and development, localStorage + JWT is acceptable and easier to debug (you can see the token in DevTools).

---

## Implementation Reference

This section lists the actual files in our Task Manager app.

### Auth Infrastructure

| File | Purpose |
|------|---------|
| `src/contexts/AuthContext.tsx` | AuthProvider + useAuth hook |
| `src/api/auth.ts` | Auth API functions (login, register, getCurrentUser) |
| `src/lib/tokenStorage.ts` | JWT token storage utilities |
| `src/api/client.ts` | API client with automatic Authorization header |
| `src/types/api.ts` | Auth types (LoginRequest, RegisterRequest, AuthResponse) |
| `src/lib/mockAuth.ts` | Mock auth (development/learning only) |

### Route Files

| File | Purpose |
|------|---------|
| `src/routes/login.tsx` | Login page with redirect handling |
| `src/routes/register.tsx` | Registration page |
| `src/routes/__root.tsx` | Root layout with auth in context |
| `src/main.tsx` | App entry with AuthProvider + InnerApp pattern |

### Protected Routes

| File | Route | Guard |
|------|-------|-------|
| `src/routes/tasks/index.tsx` | `/tasks` | beforeLoad redirect |
| `src/routes/tasks/$taskId.tsx` | `/tasks/:id` | beforeLoad redirect |
| `src/routes/projects/index.tsx` | `/projects` | beforeLoad redirect |
| `src/routes/projects/$projectId.tsx` | `/projects/:id` | beforeLoad redirect |

### Components

| File | Purpose |
|------|---------|
| `src/components/LoginForm.tsx` | Login form with RHF + Zod |
| `src/components/RegisterForm.tsx` | Registration form with RHF + Zod |
| `src/components/Header.tsx` | Conditional nav based on auth state |

### Validation Schemas

| File | Schemas |
|------|---------|
| `src/schemas/auth.ts` | `loginSchema`, `registerSchema` |

---

## Key Takeaways

1. **React Context** is ideal for auth - it's global, built-in, and triggers re-renders
2. **AuthProvider** wraps your app and provides `useAuth()` hook
3. **Router Context** receives auth via `InnerApp` pattern
4. **`beforeLoad`** guards routes before rendering or fetching data
5. **`throw redirect()`** immediately stops execution and navigates
6. **Search params** preserve the intended destination (`?redirect=/tasks`)
7. **localStorage** persists sessions across page refreshes
8. **Mock auth** is great for learning but never for production

---

## Comparison: Auth Patterns

| Pattern | Pros | Cons |
|---------|------|------|
| **React Context** | Simple, built-in, reactive | Requires provider hierarchy |
| **Zustand/Redux** | More features, devtools | Extra dependency, overkill for auth |
| **TanStack Query** | Great caching | Designed for server state, not auth |
| **localStorage** | Persists, simple | XSS vulnerable, sync only |
| **HTTP-only Cookie** | Secure, server-controlled | Requires backend support |

---

## Next Steps

After implementing mock auth, consider:
1. **Real backend auth** - JWT tokens, password hashing
2. **Token refresh** - Handle expired tokens
3. **Role-based access** - Admin vs user routes
4. **OAuth/SSO** - Google, GitHub login
5. **Remember me** - Different session durations
