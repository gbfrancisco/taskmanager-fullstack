# 06 - Route Loaders

Prefetching data before route renders.

## Table of Contents
- [What are Route Loaders?](#what-are-route-loaders)
- [Basic Loader Syntax](#basic-loader-syntax)
- [Loader with Parameters](#loader-with-parameters)
- [TanStack Query Integration](#tanstack-query-integration)
- [useLoaderData vs useQuery](#useloaderdata-vs-usequery)
- [Loading States](#loading-states)
- [Error Handling](#error-handling)
- [Implementation Reference](#implementation-reference)

---

## What are Route Loaders?

Loaders fetch data **before** a route component renders, eliminating the loading flash.

### The Problem (Without Loaders)

```
1. User navigates to /tasks
2. Component mounts
3. useQuery starts fetching → isPending = true → Skeleton shown
4. Data arrives → isPending = false → Real content
```

User sees a brief flash of skeleton/loading state.

### The Solution (With Loaders)

```
1. User navigates to /tasks
2. Loader runs → fetches data
3. Component mounts with data ready
4. Real content shown immediately
```

No loading flash. Data is available on first render.

### Avoiding Waterfalls

Without loaders, nested data fetching creates "waterfalls":

```
Component A mounts → fetches →
  Component B mounts → fetches →
    Component C mounts → fetches → done
```

With loaders, all data fetches can happen in parallel before any component renders.

---

## Basic Loader Syntax

```tsx
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/tasks/')({
  // Loader runs before component renders
  loader: async () => {
    const response = await fetch('/api/tasks');
    return response.json();
  },
  component: TasksPage,
});

function TasksPage() {
  // Access loader data with useLoaderData hook
  const tasks = Route.useLoaderData();

  return (
    <ul>
      {tasks.map(task => <li key={task.id}>{task.title}</li>)}
    </ul>
  );
}
```

**Key points:**
- `loader` is an async function that returns data
- `Route.useLoaderData()` retrieves that data in the component
- Component won't render until loader completes

---

## Loader with Parameters

### Route Params

```tsx
// routes/tasks/$taskId.tsx
export const Route = createFileRoute('/tasks/$taskId')({
  loader: async ({ params }) => {
    // params.taskId is available here
    const response = await fetch(`/api/tasks/${params.taskId}`);
    return response.json();
  },
  component: TaskDetailPage,
});
```

### Search Params

```tsx
export const Route = createFileRoute('/tasks/')({
  loader: async ({ search }) => {
    // search contains query string params
    const response = await fetch(`/api/tasks?status=${search.status}`);
    return response.json();
  },
  component: TasksPage,
});
```

### Context (QueryClient, Auth, etc.)

```tsx
export const Route = createFileRoute('/tasks/')({
  loader: ({ context }) => {
    // context.queryClient, context.auth, etc.
    const { queryClient } = context;
    // ...
  },
  component: TasksPage,
});
```

---

## TanStack Query Integration

Loaders work best with TanStack Query for caching and reactivity.

### Setup: Pass QueryClient Through Context

```tsx
// routes/__root.tsx
import { createRootRouteWithContext } from '@tanstack/react-router';
import type { QueryClient } from '@tanstack/react-query';

interface RouterContext {
  queryClient: QueryClient;
}

export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
});
```

```tsx
// main.tsx
const queryClient = new QueryClient();

<RouterProvider
  router={router}
  context={{ queryClient }}
/>
```

### Using ensureQueryData in Loaders

```tsx
export const Route = createFileRoute('/tasks/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData({
      queryKey: ['tasks'],
      queryFn: fetchTasks,
    }),
  component: TasksPage,
});
```

### QueryClient Methods Comparison

| Method | Behavior |
|--------|----------|
| `ensureQueryData` | Returns cached data if fresh, otherwise fetches |
| `fetchQuery` | Always fetches, updates cache |
| `prefetchQuery` | Like ensureQueryData but returns void |
| `getQueryData` | Returns cached data or undefined (no fetch) |

**Use `ensureQueryData`** for loaders - it avoids unnecessary refetches if data is already cached.

---

## useLoaderData vs useQuery

After adding a loader, you have two ways to access data:

### Option 1: useLoaderData Only

```tsx
function TasksPage() {
  const tasks = Route.useLoaderData();

  return <TaskList tasks={tasks} />;
}
```

**Pros:** Simple, no extra dependencies
**Cons:** Static data - no automatic updates

### Option 2: Loader + useQuery (Recommended)

```tsx
function TasksPage() {
  const { data: tasks } = useQuery({
    queryKey: taskKeys.list(),
    queryFn: fetchTasks,
  });

  return <TaskList tasks={tasks} />;
}
```

**Pros:** Reactive updates, background refetching
**Cons:** Slightly more code

### What is "Reactive"?

Reactive = component automatically re-renders when data changes.

**Example: Creating a new task**

```
TasksPage                          TaskForm
    │                                 │
    │ useQuery(['tasks'])             │
    │ ← subscribed to cache           │
    │                                 ├─ User creates task
    │                                 ├─ Mutation succeeds
    │                                 ├─ invalidateQueries(['tasks'])
    │                                 │
    │ Cache invalidated! ─────────────┘
    │ useQuery refetches
    │ Component re-renders
    │ New task appears in list
    ▼
```

**With `useLoaderData`:** List stays stale until page reload.
**With `useQuery`:** List updates automatically.

### When to Use Each

| Approach | Use When |
|----------|----------|
| `useLoaderData` only | Static pages, no mutations, simple cases |
| Loader + `useQuery` | Lists with CRUD, real-time updates needed |

### Analogy

- **`useLoaderData`:** Taking a photo (static snapshot)
- **`useQuery`:** Live video feed (updates in real-time)

---

## Loading States

### Route-Level Loading (pendingComponent)

```tsx
export const Route = createFileRoute('/tasks/')({
  loader: async () => { /* ... */ },
  pendingComponent: () => <div>Loading tasks...</div>,
  component: TasksPage,
});
```

Shows while the loader is running.

### Keep Skeleton as Fallback

Even with loaders, keep the `isPending` check for edge cases:

```tsx
function TasksPage() {
  const { data: tasks, isPending } = useQuery({
    queryKey: taskKeys.list(),
    queryFn: fetchTasks,
  });

  // Won't show on initial load (loader prefetched)
  // But useful if cache is cleared or refetch fails
  if (isPending) {
    return <SkeletonCards />;
  }

  return <TaskList tasks={tasks} />;
}
```

---

## Error Handling

### Route-Level Error Component

```tsx
export const Route = createFileRoute('/tasks/')({
  loader: async () => {
    const response = await fetch('/api/tasks');
    if (!response.ok) throw new Error('Failed to load tasks');
    return response.json();
  },
  errorComponent: ({ error }) => (
    <div className="error">
      <h2>Error loading tasks</h2>
      <p>{error.message}</p>
    </div>
  ),
  component: TasksPage,
});
```

### With TanStack Query

Errors thrown in `ensureQueryData` will be caught by the route's `errorComponent`:

```tsx
export const Route = createFileRoute('/tasks/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData({
      queryKey: taskKeys.list(),
      queryFn: fetchTasks, // If this throws, errorComponent shows
    }),
  errorComponent: TasksErrorComponent,
  component: TasksPage,
});
```

---

## Implementation Reference

### Files in This Project

| File | Description |
|------|-------------|
| `routes/__root.tsx` | Passes queryClient through context |
| `routes/tasks/index.tsx` | Tasks list with loader |
| `routes/projects/index.tsx` | Projects list with loader |
| `main.tsx` | Creates QueryClient, passes to router |

### Tasks List Implementation

```tsx
// routes/tasks/index.tsx
export const Route = createFileRoute('/tasks/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData({
      queryKey: taskKeys.list(),
      queryFn: fetchTasks,
    }),
  component: TasksPage,
});

function TasksPage() {
  // useQuery reads from cache (populated by loader)
  const { data: tasks, isPending, isError } = useQuery({
    queryKey: taskKeys.list(),
    queryFn: fetchTasks,
  });

  if (isPending) return <Skeleton />;  // Won't flash on initial load
  if (isError) return <Error />;

  return <TaskList tasks={tasks} />;
}
```

---

## Summary

| Concept | Key Takeaway |
|---------|--------------|
| Route Loaders | Fetch data before component renders |
| `ensureQueryData` | Fetch if not cached, return cached if available |
| `useLoaderData` | Static snapshot from loader |
| Loader + `useQuery` | Prefetch + reactive updates (recommended) |
| Reactive | Component re-renders when cache changes |
