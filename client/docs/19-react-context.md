# 19 - React Context

This document covers React Context: what it is, when to use it, and how to implement it effectively.

## Table of Contents
- [What is Context?](#what-is-context)
- [The Problem: Prop Drilling](#the-problem-prop-drilling)
- [Creating Context Step by Step](#creating-context-step-by-step)
- [The Provider Pattern](#the-provider-pattern)
- [Consuming Context](#consuming-context)
- [Custom Hooks for Context](#custom-hooks-for-context)
- [Context vs Other Solutions](#context-vs-other-solutions)
- [When to Use Context](#when-to-use-context)
- [Performance Considerations](#performance-considerations)
- [Common Patterns](#common-patterns)
- [Implementation Reference](#implementation-reference)
- [Integrating Context with TanStack Router](#integrating-context-with-tanstack-router)

---

## What is Context?

React Context is a way to share data between components without passing props through every level of the component tree. It creates a "global" scope for a subtree of components.

```
Without Context (prop drilling):
┌─────────────────────────────────────┐
│ App                                 │
│   user={user} ──────────────┐       │
│   ┌─────────────────────────▼─────┐ │
│   │ Layout                        │ │
│   │   user={user} ────────┐       │ │
│   │   ┌───────────────────▼─────┐ │ │
│   │   │ Sidebar                 │ │ │
│   │   │   user={user} ───┐      │ │ │
│   │   │   ┌──────────────▼────┐ │ │ │
│   │   │   │ UserAvatar        │ │ │ │
│   │   │   │   Finally uses it!│ │ │ │
│   │   │   └───────────────────┘ │ │ │
│   │   └─────────────────────────┘ │ │
│   └───────────────────────────────┘ │
└─────────────────────────────────────┘

With Context:
┌─────────────────────────────────────┐
│ UserContext.Provider value={user}   │
│   ┌─────────────────────────────┐   │
│   │ App                         │   │
│   │   ┌─────────────────────┐   │   │
│   │   │ Layout              │   │   │
│   │   │   ┌─────────────┐   │   │   │
│   │   │   │ Sidebar     │   │   │   │
│   │   │   │   ┌───────┐ │   │   │   │
│   │   │   │   │Avatar │◄┼───┼───┼───┤ useContext(UserContext)
│   │   │   │   └───────┘ │   │   │   │
│   │   │   └─────────────┘   │   │   │
│   │   └─────────────────────┘   │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

**Key Concept:** Context lets any component in the tree access shared data directly, without intermediate components needing to know about it.

---

## The Problem: Prop Drilling

"Prop drilling" is when you pass props through many component layers just to get data to a deeply nested component.

### Example: Prop Drilling

```tsx
// Every component in the chain needs to accept and pass the prop
function App() {
  const [theme, setTheme] = useState('light');
  return <Layout theme={theme} setTheme={setTheme} />;
}

function Layout({ theme, setTheme }) {
  return (
    <div>
      <Header theme={theme} setTheme={setTheme} />
      <Main theme={theme} />
    </div>
  );
}

function Header({ theme, setTheme }) {
  return (
    <header>
      <Logo theme={theme} />
      <ThemeToggle theme={theme} setTheme={setTheme} />
    </header>
  );
}

function ThemeToggle({ theme, setTheme }) {
  // Finally! The component that actually uses it
  return (
    <button onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}>
      Toggle Theme
    </button>
  );
}
```

**Problems with prop drilling:**
1. **Verbose** - Every intermediate component needs the prop
2. **Fragile** - Renaming a prop requires changes everywhere
3. **Coupling** - Components know about data they don't use
4. **Maintenance** - Adding new shared data means updating the whole chain

---

## Creating Context Step by Step

### Step 1: Create the Context

```tsx
import { createContext } from 'react';

// Define the shape of your context value
interface ThemeContextType {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

// Create context with a default value (or null)
const ThemeContext = createContext<ThemeContextType | null>(null);
```

**Why `null` as default?**

Using `null` and checking for it later ensures components crash early if used outside the Provider. This catches bugs during development rather than causing silent failures with a fake default value.

### Step 2: Create the Provider Component

The Provider holds the actual state and provides it to children:

```tsx
import { useState, ReactNode } from 'react';

interface ThemeProviderProps {
  children: ReactNode;
}

function ThemeProvider({ children }: ThemeProviderProps) {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  // The value object contains everything consumers can access
  const value: ThemeContextType = {
    theme,
    toggleTheme
  };

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}
```

### Step 3: Create a Custom Hook

Wrap `useContext` in a custom hook with error handling:

```tsx
import { useContext } from 'react';

function useTheme(): ThemeContextType {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error(
      'useTheme must be used within a ThemeProvider. ' +
      'Wrap your component tree with <ThemeProvider>.'
    );
  }

  return context;
}
```

### Step 4: Wrap Your App

```tsx
// main.tsx or App.tsx
function App() {
  return (
    <ThemeProvider>
      <Layout />
    </ThemeProvider>
  );
}
```

### Step 5: Consume in Any Component

```tsx
function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <button onClick={toggleTheme}>
      Current: {theme}
    </button>
  );
}

function Header() {
  const { theme } = useTheme();

  return (
    <header className={theme === 'dark' ? 'bg-gray-900' : 'bg-white'}>
      <Logo />
      <ThemeToggle />
    </header>
  );
}
```

---

## The Provider Pattern

The Provider pattern encapsulates state management in a dedicated component:

```tsx
// contexts/ThemeContext.tsx - Complete file structure

import {
  createContext,
  useContext,
  useState,
  type ReactNode
} from 'react';

// =============================================================================
// TYPES
// =============================================================================

interface ThemeContextType {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  setTheme: (theme: 'light' | 'dark') => void;
}

// =============================================================================
// CONTEXT
// =============================================================================

const ThemeContext = createContext<ThemeContextType | null>(null);

// =============================================================================
// PROVIDER
// =============================================================================

interface ThemeProviderProps {
  children: ReactNode;
  defaultTheme?: 'light' | 'dark';
}

export function ThemeProvider({
  children,
  defaultTheme = 'light'
}: ThemeProviderProps) {
  const [theme, setTheme] = useState<'light' | 'dark'>(defaultTheme);

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  const value: ThemeContextType = {
    theme,
    toggleTheme,
    setTheme
  };

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}

// =============================================================================
// HOOK
// =============================================================================

export function useTheme(): ThemeContextType {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }

  return context;
}
```

**Benefits of this pattern:**
1. **Single file** - Context, Provider, and hook in one place
2. **Encapsulated** - State logic is hidden from consumers
3. **Type-safe** - TypeScript ensures correct usage
4. **Testable** - Provider can be mocked in tests

---

## Consuming Context

### Method 1: Custom Hook (Recommended)

```tsx
function MyComponent() {
  const { theme, toggleTheme } = useTheme();
  // ...
}
```

### Method 2: useContext Directly

```tsx
import { useContext } from 'react';
import { ThemeContext } from './ThemeContext';

function MyComponent() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('Missing provider');
  // ...
}
```

### Method 3: Consumer Component (Legacy)

```tsx
// Older pattern, rarely used with hooks
<ThemeContext.Consumer>
  {({ theme }) => <div className={theme}>...</div>}
</ThemeContext.Consumer>
```

**Always prefer Method 1** - Custom hooks provide better error messages and are more ergonomic.

---

## Custom Hooks for Context

Custom hooks make context easier to use and provide better developer experience:

### Basic Hook

```tsx
export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}
```

### Hook with Selector (Performance)

For contexts with many values, you can create focused hooks:

```tsx
// Full context
interface AppContextType {
  user: User | null;
  theme: 'light' | 'dark';
  notifications: Notification[];
  // ... many more
}

// Focused hooks - components only subscribe to what they need
export function useUser() {
  const { user } = useAppContext();
  return user;
}

export function useTheme() {
  const { theme } = useAppContext();
  return theme;
}

export function useNotifications() {
  const { notifications } = useAppContext();
  return notifications;
}
```

### Hook with Actions Only

Sometimes components only need actions, not state:

```tsx
export function useThemeActions() {
  const { toggleTheme, setTheme } = useTheme();
  return { toggleTheme, setTheme };
}

// Usage - component won't re-render on theme changes
function ThemeButton() {
  const { toggleTheme } = useThemeActions();
  return <button onClick={toggleTheme}>Toggle</button>;
}
```

---

## Context vs Other Solutions

| Solution | Best For | Complexity | Bundle Size |
|----------|----------|------------|-------------|
| **React Context** | Simple global state, dependency injection | Low | None (built-in) |
| **Props** | Parent-child data passing | Lowest | None |
| **Zustand** | Complex client state, many actions | Medium | ~2KB |
| **Redux** | Large apps, time-travel debugging | High | ~7KB |
| **Jotai/Recoil** | Atomic state, fine-grained updates | Medium | ~3KB |
| **TanStack Query** | Server state (API data) | Medium | ~12KB |

### When Context is the Right Choice

| Use Case | Context? | Why |
|----------|----------|-----|
| Auth state (user, isLoggedIn) | Yes | Truly global, infrequent updates |
| Theme (light/dark) | Yes | Global, infrequent updates |
| Locale/i18n | Yes | Global, infrequent updates |
| Shopping cart | Maybe | Consider Zustand if complex |
| Form state | No | Use React Hook Form |
| API data | No | Use TanStack Query |
| Frequently updating data | No | Causes too many re-renders |

---

## When to Use Context

### Good Use Cases

1. **Authentication** - User info needed everywhere
2. **Theming** - Colors, fonts across all components
3. **Localization** - Language/locale settings
4. **Feature Flags** - Enable/disable features globally
5. **Dependency Injection** - Passing services (QueryClient, etc.)

### Bad Use Cases

1. **Form State** - Use React Hook Form or local state
2. **Server Data** - Use TanStack Query
3. **Frequently Changing Data** - Causes excessive re-renders
4. **Component-Specific State** - Use local useState
5. **Parent-Child Only** - Just use props

### Rule of Thumb

> Use Context when data is **truly global** and **changes infrequently**.

---

## Performance Considerations

### The Re-render Problem

When context value changes, **all consumers re-render**:

```tsx
// Problem: New object on every render
function BadProvider({ children }) {
  const [count, setCount] = useState(0);

  // This creates a new object every render!
  return (
    <MyContext.Provider value={{ count, setCount }}>
      {children}
    </MyContext.Provider>
  );
}
```

### Solution 1: Stable Value References

```tsx
function GoodProvider({ children }) {
  const [count, setCount] = useState(0);

  // With React Compiler (React 19), this is automatic!
  // Without it, you'd use useMemo:
  // const value = useMemo(() => ({ count, setCount }), [count]);

  const value = { count, setCount };

  return (
    <MyContext.Provider value={value}>
      {children}
    </MyContext.Provider>
  );
}
```

**Note:** With React Compiler (which this project uses), memoization is automatic. You don't need manual `useMemo` or `useCallback`.

### Solution 2: Split Contexts

Separate frequently-changing from stable values:

```tsx
// Split into two contexts
const CountContext = createContext<number>(0);
const CountActionsContext = createContext<{ increment: () => void } | null>(null);

function CountProvider({ children }) {
  const [count, setCount] = useState(0);
  const actions = { increment: () => setCount(c => c + 1) };

  return (
    <CountActionsContext.Provider value={actions}>
      <CountContext.Provider value={count}>
        {children}
      </CountContext.Provider>
    </CountActionsContext.Provider>
  );
}

// Components using only actions don't re-render when count changes
function IncrementButton() {
  const { increment } = useContext(CountActionsContext)!;
  return <button onClick={increment}>+1</button>;
}
```

### Solution 3: Component Composition

Move state closer to where it's used:

```tsx
// Instead of putting everything in context...
function App() {
  const [count, setCount] = useState(0);

  return (
    <Layout>
      {/* Only pass to components that need it */}
      <Counter count={count} setCount={setCount} />
      <Display count={count} />
    </Layout>
  );
}
```

---

## Common Patterns

### Pattern 1: Provider with Initial Data

```tsx
interface AuthProviderProps {
  children: ReactNode;
  initialUser?: User | null;
}

function AuthProvider({ children, initialUser = null }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(initialUser);
  // ...
}

// Usage
<AuthProvider initialUser={serverUser}>
  <App />
</AuthProvider>
```

### Pattern 2: Provider with Side Effects

```tsx
function AuthProvider({ children }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Check for existing session on mount
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setIsLoading(false);
  }, []);

  // Sync to localStorage when user changes
  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  // ...
}
```

### Pattern 3: Nested Providers

```tsx
// main.tsx
<QueryProvider>
  <AuthProvider>
    <ThemeProvider>
      <App />
    </ThemeProvider>
  </AuthProvider>
</QueryProvider>
```

#### Provider Order Matters

**Rule: Inner providers can use outer provider contexts, but not vice versa.**

```
┌─────────────────────────────────────┐
│ OuterProvider                       │
│  ┌───────────────────────────────┐  │
│  │ InnerProvider                 │  │
│  │                               │  │
│  │ ✅ Can call useOuter()        │  │
│  │                               │  │
│  └───────────────────────────────┘  │
│                                     │
│ ❌ Cannot call useInner()           │
└─────────────────────────────────────┘
```

Example - ThemeProvider can use AuthProvider because Auth is outside:

```tsx
// ThemeProvider can use useAuth() because AuthProvider is outside
function ThemeProvider({ children }) {
  const { user } = useAuth();  // ✅ Works - AuthProvider is an ancestor
  const defaultTheme = user?.preferences.theme || 'light';
  // ...
}
```

#### Recommended Order for This Project

```tsx
<QueryProvider>           // 1. Outermost - other providers may need useQuery/useMutation
  <AuthProvider>          // 2. Auth - may use useMutation for login API calls
    <ThemeProvider>       // 3. Theme - may use useAuth for user preferences
      <InnerApp />        // 4. Router - uses both auth and queryClient
    </ThemeProvider>
  </AuthProvider>
</QueryProvider>
```

**Why QueryProvider should be outermost:**

| Provider | May Need |
|----------|----------|
| `AuthProvider` | `useMutation` for login/register API calls (with real backend) |
| `ThemeProvider` | `useQuery` to fetch user preferences from server |
| `InnerApp` | Both `useAuth()` and router context with `queryClient` |

Even if your AuthProvider doesn't use TanStack Query today (like our mock auth), placing QueryProvider outside is **future-proof** for when you add real API authentication.

#### Current Mock Auth (Independent)

Our current `AuthProvider` uses only `useState` and `localStorage`, so it doesn't depend on QueryProvider. But when adding real JWT authentication with API calls, you'd use `useMutation`:

```tsx
// Future: AuthProvider with real API
function AuthProvider({ children }) {
  const loginMutation = useMutation({
    mutationFn: (credentials) => api.post('/auth/login', credentials)
  });

  const login = async (username, password) => {
    const user = await loginMutation.mutateAsync({ username, password });
    setUser(user);
  };
  // ...
}
```

This would require `QueryProvider` to be an ancestor of `AuthProvider`.

### Pattern 4: Context for Compound Components

```tsx
// Context for component composition
const TabsContext = createContext<{
  activeTab: string;
  setActiveTab: (tab: string) => void;
} | null>(null);

function Tabs({ children, defaultTab }) {
  const [activeTab, setActiveTab] = useState(defaultTab);

  return (
    <TabsContext.Provider value={{ activeTab, setActiveTab }}>
      <div className="tabs">{children}</div>
    </TabsContext.Provider>
  );
}

function Tab({ id, children }) {
  const { activeTab, setActiveTab } = useContext(TabsContext)!;

  return (
    <button
      className={activeTab === id ? 'active' : ''}
      onClick={() => setActiveTab(id)}
    >
      {children}
    </button>
  );
}

function TabPanel({ id, children }) {
  const { activeTab } = useContext(TabsContext)!;
  return activeTab === id ? <div>{children}</div> : null;
}

// Usage
<Tabs defaultTab="one">
  <Tab id="one">Tab 1</Tab>
  <Tab id="two">Tab 2</Tab>
  <TabPanel id="one">Content 1</TabPanel>
  <TabPanel id="two">Content 2</TabPanel>
</Tabs>
```

---

## Implementation Reference

This section lists the actual context files in our Task Manager app.

| File | Purpose |
|------|---------|
| `src/contexts/AuthContext.tsx` | Authentication state, login/logout methods |
| `src/lib/mockAuth.ts` | Mock auth service used by AuthContext |

### AuthContext Structure

```tsx
// What AuthContext provides
interface AuthContextType {
  user: AuthUser | null;           // Current user data
  isAuthenticated: boolean;        // Convenience boolean
  isLoading: boolean;              // True during initial session check
  login: (username, password) => Promise<void>;
  register: (username, email, password) => Promise<void>;
  logout: () => Promise<void>;
}
```

### Provider Hierarchy

```tsx
// main.tsx - Provider order (outer to inner)
<StrictMode>
  <TanStackQueryProvider>         {/* 1. Data caching - outermost so others can use useQuery/useMutation */}
    <AuthProvider>                {/* 2. Auth state - may use useMutation for real API auth */}
      <InnerApp />                {/* 3. Router with auth + queryClient context */}
    </AuthProvider>
  </TanStackQueryProvider>
</StrictMode>
```

**Why this order?** See [Pattern 3: Nested Providers](#pattern-3-nested-providers) for explanation.

### Usage in Components

```tsx
// Header.tsx
import { useAuth } from '@/contexts/AuthContext';

function Header() {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <header>
      {isAuthenticated ? (
        <>
          <span>{user?.username}</span>
          <button onClick={logout}>Logout</button>
        </>
      ) : (
        <Link to="/login">Login</Link>
      )}
    </header>
  );
}
```

---

## Integrating Context with TanStack Router

TanStack Router has its own context system that runs **before React renders**. This creates a challenge: how do you use React Context (like `useAuth()`) in route configuration?

### The Problem

TanStack Router creates the router at **module level** for type safety:

```tsx
// main.tsx - This runs at import time, BEFORE React renders

const router = createRouter({
  routeTree,
  context: {
    queryClient,
    // ❌ Can't call useAuth() here - no React component!
  }
});
```

But `useAuth()` is a React hook that only works inside components. You can't use it at module level.

### The Solution: InnerApp Pattern

Create an inner component that bridges React Context to Router Context:

```tsx
// main.tsx

// 1. Create router at module level (required for types)
const router = createRouter({
  routeTree,
  context: {
    queryClient,
    auth: undefined!  // Placeholder - will be overridden
  }
});

// 2. Inner component can use React hooks
function InnerApp() {
  const auth = useAuth();  // ✅ Now this works!

  return (
    <RouterProvider
      router={router}
      context={{ queryClient, auth }}  // Override with real auth
    />
  );
}

// 3. Wrap InnerApp with AuthProvider
function App() {
  return (
    <AuthProvider>
      <InnerApp />
    </AuthProvider>
  );
}
```

**Why this works:**
- Router is created at module level (TypeScript needs this for route types)
- `InnerApp` is a React component, so it can call `useAuth()`
- `RouterProvider`'s `context` prop **overrides** the initial context
- Routes receive the real auth values at runtime

### Accessing Context in Route Guards

Once context is properly set up, access it in route configuration:

```tsx
// routes/tasks/index.tsx

export const Route = createFileRoute('/tasks/')({
  // beforeLoad runs BEFORE the component renders
  beforeLoad: ({ context, location }) => {
    // context.auth is now available!
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname }
      });
    }
  },
  component: TasksPage
});
```

### Type Safety with Router Context

Define the router context interface in `__root.tsx`:

```tsx
// routes/__root.tsx
import type { AuthContextType } from '@/contexts/AuthContext';

export interface MyRouterContext {
  queryClient: QueryClient;
  auth: AuthContextType;  // Add auth to router context
}

export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: RootLayout
});
```

Now TypeScript knows `context.auth` exists in all routes:

```tsx
// Any route file
beforeLoad: ({ context }) => {
  context.auth.isAuthenticated;  // ✅ TypeScript knows this exists
  context.auth.user?.username;    // ✅ Properly typed
}
```

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ App Initialization                                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Module load: router = createRouter({ context: {...} })  │
│                                                             │
│  2. React render:                                           │
│     <AuthProvider>                    ← State lives here    │
│       <InnerApp>                      ← Reads auth state    │
│         <RouterProvider context={}>   ← Passes to router    │
│           <Routes>                    ← Can access auth     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### When This Pattern is Needed

Use the InnerApp pattern when:
- You need React Context values in route configuration
- You're using `beforeLoad` guards that check auth
- You want type-safe access to context in routes

You **don't** need this pattern if you only use context in components (not route config).

---

## Key Takeaways

1. **Context solves prop drilling** - Share data without passing through every level
2. **Create with `createContext()`** - Pass a default value or `null`
3. **Provide with `<Context.Provider>`** - Wrap the tree that needs access
4. **Consume with custom hooks** - `useMyContext()` with error handling
5. **Use for truly global state** - Auth, theme, locale
6. **Don't overuse** - Props are fine for parent-child; TanStack Query for server data
7. **Consider performance** - Split contexts if values update at different rates
8. **React Compiler helps** - Automatic memoization in React 19

---

## Further Reading

- [React Docs: useContext](https://react.dev/reference/react/useContext)
- [React Docs: createContext](https://react.dev/reference/react/createContext)
- [Patterns.dev: Provider Pattern](https://www.patterns.dev/react/provider-pattern)
- [Kent C. Dodds: How to use React Context effectively](https://kentcdodds.com/blog/how-to-use-react-context-effectively)
