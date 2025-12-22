# 03 - TanStack Query Basics

Data fetching, caching, and server state management.

## Table of Contents
- [What is TanStack Query?](#what-is-tanstack-query)
- [Router Loaders vs Query Hooks](#router-loaders-vs-query-hooks)
- [Core Concepts](#core-concepts)
- [useQuery Hook](#usequery-hook)
- [Query Keys](#query-keys)
- [Loading and Error States](#loading-and-error-states)
- [Caching and Refetching](#caching-and-refetching)
- [Devtools](#devtools)
- [Implementation Reference](#implementation-reference)

---

## What is TanStack Query?

TanStack Query (formerly React Query) is a powerful data fetching and caching library for React. It solves the problem of managing "server state" - data that lives on a server and needs to be synchronized with your UI.

### The Problem It Solves

Without TanStack Query, fetching data typically looks like this:

```tsx
function PostsList() {
  const [posts, setPosts] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setIsLoading(true)
    fetch('/api/posts')
      .then(res => res.json())
      .then(data => {
        setPosts(data)
        setIsLoading(false)
      })
      .catch(err => {
        setError(err)
        setIsLoading(false)
      })
  }, [])

  // Handle loading, error, render...
}
```

This approach has problems:
- **No caching**: Every component mount triggers a new request
- **No deduplication**: If two components need the same data, two requests fire
- **Manual state management**: You manage loading/error/data states yourself
- **No background updates**: Data goes stale and you don't know
- **No retry logic**: If a request fails, you need to implement retry yourself

### TanStack Query Solution

With TanStack Query, the same code becomes:

```tsx
function PostsList() {
  const { data: posts, isPending, isError, error } = useQuery({
    queryKey: ['posts'],
    queryFn: () => fetch('/api/posts').then(res => res.json()),
  })

  // Handle loading, error, render...
}
```

TanStack Query automatically handles:
- **Caching**: Data is cached and reused across components
- **Deduplication**: Multiple components requesting the same data = one request
- **Background refetching**: Stale data is refreshed automatically
- **Retry on failure**: Failed requests are retried with exponential backoff
- **Window focus refetching**: Data refreshes when user returns to the tab

---

## Router Loaders vs Query Hooks

TanStack Router provides a `loader` option for fetching data before a route renders. So why use TanStack Query's `useQuery` instead?

### The Router Loader Approach

```tsx
// Route with loader - data fetches BEFORE component renders
export const Route = createFileRoute('/tasks')({
  loader: async () => {
    const response = await fetch('/api/tasks')
    return response.json()
  },
  component: TasksPage,
})

function TasksPage() {
  // Data is already available - no loading state needed
  const tasks = Route.useLoaderData()
  return <TaskList tasks={tasks} />
}
```

**How it works:**
1. User navigates to `/tasks`
2. Router calls the `loader` function
3. Navigation **waits** until data is fetched
4. Component renders with data immediately available

### The useQuery Approach (What We Use)

```tsx
// Route without loader - data fetches AFTER component mounts
export const Route = createFileRoute('/tasks')({
  component: TasksPage,
})

function TasksPage() {
  const { data: tasks, isPending } = useQuery({
    queryKey: ['tasks'],
    queryFn: fetchTasks,
  })

  if (isPending) return <LoadingSkeleton />
  return <TaskList tasks={tasks} />
}
```

**How it works:**
1. User navigates to `/tasks`
2. Route renders **immediately** with loading state
3. `useQuery` triggers the fetch
4. Component re-renders when data arrives

### Comparison

| Aspect | Router Loader | useQuery |
|--------|---------------|----------|
| **When data loads** | Before route renders | After component mounts |
| **Navigation feel** | Waits, then shows complete page | Instant, shows loading state |
| **Caching** | Manual (or integrate with Query) | Automatic, sophisticated |
| **Background refetch** | Manual | Automatic |
| **Error handling** | Route-level `errorComponent` | Component-level or boundary |
| **Code location** | Route definition | Component |

### Why We Chose useQuery

For this learning project, `useQuery` is the better choice because:

1. **Explicit loading states** - You see and handle loading/error states directly, which is educational
2. **Component-level control** - Data fetching logic lives with the component that needs it
3. **Automatic caching** - No extra setup needed for cache management
4. **Simpler mental model** - Fetch happens where data is used

### When to Use Loaders Instead

Router loaders are better when:
- You want the old-school "wait then render" navigation feel
- Data is critical and the page is meaningless without it
- You're combining with TanStack Query via `ensureQueryData` (advanced pattern)
- SEO/SSR requires data before HTML generation

### The Hybrid Approach (Advanced)

You can combine both - use loaders to **start** fetches early, and `useQuery` to **consume** the cached data:

```tsx
export const Route = createFileRoute('/tasks')({
  loader: ({ context }) => {
    // Start fetch early, don't await
    context.queryClient.prefetchQuery({
      queryKey: ['tasks'],
      queryFn: fetchTasks,
    })
  },
  component: TasksPage,
})

function TasksPage() {
  // Picks up prefetched data from cache
  const { data, isPending } = useQuery({
    queryKey: ['tasks'],
    queryFn: fetchTasks,
  })
  // ...
}
```

This gives faster perceived loading without blocking navigation.

---

## Core Concepts

### Queries vs Mutations

- **Queries** (`useQuery`): READ operations - fetching data
- **Mutations** (`useMutation`): WRITE operations - creating, updating, deleting data

This session covers queries. Mutations are covered in Session 05.

### Query Client

The `QueryClient` is the central hub for all queries. It manages:
- The cache of all query results
- Default configuration for all queries
- Methods to manually interact with the cache

```tsx
// Create once at app root
const queryClient = new QueryClient()

// Provide to app
<QueryClientProvider client={queryClient}>
  <App />
</QueryClientProvider>
```

### Stale vs Fresh Data

TanStack Query has two key time concepts:

- **staleTime**: How long until data is considered "stale" (default: 0ms)
  - Fresh data won't trigger background refetches
  - Stale data will be refetched in the background when accessed

- **gcTime**: How long to keep unused data in cache (default: 5 minutes)
  - After this time, inactive queries are garbage collected
  - Previously called `cacheTime` in older versions

---

## useQuery Hook

The `useQuery` hook is the primary way to fetch data.

### Basic Usage

```tsx
import { useQuery } from '@tanstack/react-query'

function UserProfile({ userId }) {
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['users', userId],
    queryFn: () => fetchUserById(userId),
  })

  if (isPending) return <Spinner />
  if (isError) return <Error message={error.message} />
  return <Profile user={data} />
}
```

### Configuration Options

```tsx
useQuery({
  // Required
  queryKey: ['posts'],           // Unique identifier for caching
  queryFn: fetchPosts,           // Function that returns a Promise

  // Optional
  enabled: true,                 // Set to false to disable automatic fetching
  staleTime: 5 * 60 * 1000,      // Data fresh for 5 minutes
  gcTime: 10 * 60 * 1000,        // Keep in cache for 10 minutes
  refetchOnWindowFocus: true,    // Refetch when window regains focus
  retry: 3,                      // Number of retries on failure
  retryDelay: 1000,              // Delay between retries
})
```

### Return Values

```tsx
const {
  data,           // The resolved data (undefined while loading)
  error,          // Error object if query failed
  isPending,      // True during initial load (no cached data)
  isLoading,      // True during initial load (legacy, same as isPending)
  isFetching,     // True whenever a fetch is in progress (including background)
  isError,        // True if query is in error state
  isSuccess,      // True if query succeeded
  refetch,        // Function to manually trigger a refetch
  status,         // 'pending' | 'error' | 'success'
} = useQuery(...)
```

### Important: isPending vs isFetching

- `isPending`: True only when there's no cached data and a fetch is in progress
- `isFetching`: True whenever any fetch is happening (including background refetches)

Use `isPending` for showing loading skeletons. Use `isFetching` for showing subtle loading indicators during background updates.

---

## Query Keys

Query keys uniquely identify queries for caching and refetching.

### Key Structure

Keys are arrays, typically starting with a string identifier:

```tsx
// Simple key
['posts']

// Key with ID parameter
['posts', postId]

// Key with filters
['posts', { status: 'published', author: userId }]

// Nested structure
['projects', projectId, 'tasks']
```

### Key Matching

Keys are matched hierarchically:

```tsx
// Invalidate all queries starting with 'posts'
queryClient.invalidateQueries({ queryKey: ['posts'] })

// This invalidates:
// - ['posts']
// - ['posts', 1]
// - ['posts', { status: 'published' }]
```

### Query Key Factory Pattern

For consistency, define all keys in one place:

```tsx
export const postKeys = {
  all: ['posts'] as const,
  lists: () => [...postKeys.all, 'list'] as const,
  list: (filters) => [...postKeys.lists(), filters] as const,
  details: () => [...postKeys.all, 'detail'] as const,
  detail: (id) => [...postKeys.details(), id] as const,
}

// Usage
useQuery({ queryKey: postKeys.detail(postId), queryFn: ... })
```

Benefits:
- Type-safe keys with `as const`
- Easy to invalidate related queries
- Single source of truth for key structure

---

## Loading and Error States

### The Standard Pattern

```tsx
function DataDisplay() {
  const { data, isPending, isError, error } = useQuery({...})

  // 1. Check loading state first
  if (isPending) {
    return <LoadingSkeleton />
  }

  // 2. Check error state
  if (isError) {
    return <ErrorMessage error={error} />
  }

  // 3. Render data (TypeScript knows data is defined here)
  return <DataView data={data} />
}
```

### Skeleton Loading

Prefer skeleton loading over spinners for better UX:

```tsx
if (isPending) {
  return (
    <div className="animate-pulse">
      <div className="h-6 bg-gray-200 rounded w-1/3 mb-2" />
      <div className="h-4 bg-gray-100 rounded w-full mb-1" />
      <div className="h-4 bg-gray-100 rounded w-2/3" />
    </div>
  )
}
```

### Error Boundaries

For component-level error handling, use error boundaries with the `throwOnError` option:

```tsx
useQuery({
  queryKey: ['posts'],
  queryFn: fetchPosts,
  throwOnError: true, // Throws to nearest error boundary
})
```

---

## Caching and Refetching

### How Caching Works

1. First request fetches data and caches it
2. Subsequent renders with same key use cached data immediately
3. Background refetch updates cache if data is stale
4. Components re-render with fresh data

### Refetch Triggers

Data is refetched when:
- Component mounts and data is stale
- Query key changes
- Window regains focus (configurable)
- Network reconnects (configurable)
- Manual `refetch()` call
- Cache invalidation

### Disabling Automatic Refetch

```tsx
useQuery({
  queryKey: ['posts'],
  queryFn: fetchPosts,
  refetchOnWindowFocus: false,
  refetchOnMount: false,
  refetchOnReconnect: false,
})
```

### Manual Cache Interaction

```tsx
import { useQueryClient } from '@tanstack/react-query'

function Component() {
  const queryClient = useQueryClient()

  // Invalidate and refetch
  queryClient.invalidateQueries({ queryKey: ['posts'] })

  // Set data directly (useful after mutations)
  queryClient.setQueryData(['posts', 1], newPost)

  // Read cached data
  const cachedPost = queryClient.getQueryData(['posts', 1])
}
```

---

## Devtools

TanStack Query includes powerful devtools for debugging.

### Setup

Already configured in this project at `src/routes/__root.tsx`:

```tsx
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'

// In the root layout
<ReactQueryDevtools initialIsOpen={false} />
```

### Features

- View all queries and their status
- Inspect cache contents
- Manually refetch queries
- View query timing and performance
- Simulate offline mode
- Time travel debugging

### Using Devtools

1. Look for the flower icon in the bottom-right corner
2. Click to open the devtools panel
3. See all active queries, their status, and cached data
4. Click a query to see details and manually trigger refetches

---

## Implementation Reference

Files implemented in this session:

### Types
- `src/types/api.ts` - TypeScript types matching backend DTOs

### API Client
- `src/api/client.ts` - Fetch wrapper with error handling
- `src/api/tasks.ts` - Task API functions and query keys
- `src/api/projects.ts` - Project API functions and query keys

### Routes
- `src/routes/tasks.tsx` - Tasks list with useQuery
- `src/routes/tasks/$taskId.tsx` - Task detail with route params
- `src/routes/projects.tsx` - Projects list with useQuery
- `src/routes/projects/$projectId.tsx` - Project detail with parallel queries

---

## Key Takeaways

1. **useQuery** handles fetching, caching, and state management automatically
2. **Query keys** are arrays that uniquely identify cached data
3. **isPending** is for initial loads, **isFetching** is for any fetch
4. **Query key factories** ensure consistent keys across the app
5. **Parallel queries** are simple - just call useQuery multiple times
6. **The enabled option** prevents queries from running until ready

---

## Next: TypeScript with React

Learn TypeScript patterns specific to React development.
