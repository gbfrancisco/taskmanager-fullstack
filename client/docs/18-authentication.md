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
- [Mock Authentication Pattern](#mock-authentication-pattern)
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

### localStorage vs sessionStorage vs Cookies

| Storage | Persistence | Use Case |
|---------|------------|----------|
| **localStorage** | Until cleared | "Remember me" sessions |
| **sessionStorage** | Until tab closes | Single-session apps |
| **HTTP-only Cookie** | Server-controlled | Production auth (most secure) |

For learning/mock auth, `localStorage` is fine. Production apps typically use HTTP-only cookies set by the server (not accessible to JavaScript, preventing XSS attacks).

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

## Implementation Reference

This section lists the actual files in our Task Manager app.

### Auth Infrastructure

| File | Purpose |
|------|---------|
| `src/lib/mockAuth.ts` | Mock auth service with localStorage |
| `src/contexts/AuthContext.tsx` | AuthProvider + useAuth hook |

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
