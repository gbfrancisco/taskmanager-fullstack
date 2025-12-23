# 08 - Error Handling

Error boundaries and graceful error states in React applications.

## Table of Contents
- [Types of Errors](#types-of-errors)
- [Error Boundaries](#error-boundaries)
- [Route Error Components](#route-error-components)
- [Query Error Handling](#query-error-handling)
- [Mutation Error Handling](#mutation-error-handling)
- [Implementation Reference](#implementation-reference)

---

## Types of Errors

React applications encounter different types of errors:

### 1. Rendering Errors
Errors that occur during component rendering:
```tsx
function BrokenComponent() {
  // This throws during render
  throw new Error('Oops!');
  return <div>Never reached</div>;
}
```

**Caught by:** Error Boundaries

### 2. Event Handler Errors
Errors in click handlers, form submissions, etc.:
```tsx
function Button() {
  function handleClick() {
    throw new Error('Click failed!'); // NOT caught by Error Boundary
  }
  return <button onClick={handleClick}>Click me</button>;
}
```

**Caught by:** Try/catch in the handler

### 3. Async Errors
Errors in promises, fetch calls, setTimeout:
```tsx
async function fetchData() {
  const response = await fetch('/api/data');
  if (!response.ok) {
    throw new Error('Fetch failed!'); // NOT caught by Error Boundary
  }
}
```

**Caught by:** Try/catch, `.catch()`, or TanStack Query error states

### 4. Route Loading Errors
Errors when navigating to a route fails:
```tsx
export const Route = createFileRoute('/tasks/$taskId')({
  loader: async ({ params }) => {
    const task = await fetchTask(params.taskId);
    if (!task) throw new Error('Task not found');
    return task;
  }
});
```

**Caught by:** TanStack Router's `errorComponent`

---

## Error Boundaries

Error Boundaries are React components that catch JavaScript errors in their child component tree.

### What They Catch
- Errors during rendering
- Errors in lifecycle methods
- Errors in constructors of child components

### What They DON'T Catch
- Event handlers (use try/catch)
- Async code (promises, setTimeout)
- Server-side rendering errors
- Errors in the error boundary itself

### Creating an Error Boundary

Error Boundaries must be class components (hooks don't support this yet):

```tsx
import { Component } from 'react';
import type { ReactNode, ErrorInfo } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  // Called when a child throws - update state to show fallback
  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  // Called after error - use for logging
  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('Error caught:', error);
    console.error('Component stack:', errorInfo.componentStack);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return this.props.fallback || <div>Something went wrong</div>;
    }
    return this.props.children;
  }
}
```

### Using Error Boundaries

Wrap components that might fail:

```tsx
// Wrap the entire app
<ErrorBoundary>
  <App />
</ErrorBoundary>

// Or wrap specific sections
<ErrorBoundary fallback={<div>Widget failed to load</div>}>
  <UnstableWidget />
</ErrorBoundary>
```

### Reset Functionality

Allow users to retry after an error:

```tsx
class ErrorBoundary extends Component<Props, State> {
  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div>
          <p>Something went wrong</p>
          <button onClick={this.handleReset}>Try Again</button>
        </div>
      );
    }
    return this.props.children;
  }
}
```

---

## Route Error Components

TanStack Router provides `errorComponent` for handling route-level errors.

### Setting a Global Error Component

In your root route:

```tsx
import { createRootRoute } from '@tanstack/react-router';

export const Route = createRootRoute({
  component: RootLayout,
  errorComponent: RouteErrorComponent  // Catches all route errors
});
```

### Route Error Component Props

TanStack Router passes error info to the component:

```tsx
import type { ErrorComponentProps } from '@tanstack/react-router';

function RouteErrorComponent({ error, reset }: ErrorComponentProps) {
  return (
    <div>
      <h2>Page failed to load</h2>
      {error instanceof Error && <p>{error.message}</p>}
      <button onClick={reset}>Try Again</button>
    </div>
  );
}
```

### Error vs Reset

- `error`: The Error object that was thrown
- `reset()`: Function to clear the error and retry navigation

### Per-Route Error Components

Override the global error component for specific routes:

```tsx
export const Route = createFileRoute('/admin')({
  component: AdminPage,
  errorComponent: AdminErrorComponent  // Custom error UI for admin
});
```

---

## Query Error Handling

TanStack Query provides error states for data fetching.

### Error State in useQuery

```tsx
const { data, isPending, isError, error } = useQuery({
  queryKey: ['tasks'],
  queryFn: fetchTasks
});

if (isError) {
  return (
    <div className="bg-red-50 border border-red-200 rounded p-4">
      <p className="text-red-800 font-medium">Failed to load tasks</p>
      <p className="text-red-600 text-sm">
        {error instanceof Error ? error.message : 'Unknown error'}
      </p>
    </div>
  );
}
```

### Retry Configuration

TanStack Query retries failed requests by default:

```tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 3,           // Retry 3 times before showing error
      retryDelay: 1000,   // Wait 1 second between retries
    }
  }
});
```

### Manual Retry

Use `refetch` to let users retry:

```tsx
const { isError, refetch } = useQuery({...});

if (isError) {
  return (
    <div>
      <p>Failed to load</p>
      <button onClick={() => refetch()}>Retry</button>
    </div>
  );
}
```

---

## Mutation Error Handling

Handle errors from create/update/delete operations.

### Error State in useMutation

```tsx
const mutation = useMutation({
  mutationFn: createTask
});

// In your form
{mutation.isError && (
  <div className="bg-red-50 border border-red-200 rounded p-3">
    <p className="text-red-800">
      {mutation.error instanceof Error
        ? mutation.error.message
        : 'Failed to save'}
    </p>
  </div>
)}
```

### onError Callback

Handle errors programmatically:

```tsx
const mutation = useMutation({
  mutationFn: createTask,
  onError: (error) => {
    console.error('Mutation failed:', error);
    // Could show a toast notification here
  }
});
```

### Combining with Form State

Show errors below the form:

```tsx
function TaskForm() {
  const mutation = useMutation({ mutationFn: createTask });

  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}

      <button disabled={mutation.isPending}>
        {mutation.isPending ? 'Saving...' : 'Save'}
      </button>

      {mutation.isError && (
        <p className="text-red-600 mt-2">
          {mutation.error instanceof Error
            ? mutation.error.message
            : 'An error occurred'}
        </p>
      )}
    </form>
  );
}
```

---

## Implementation Reference

### Files

| File | Purpose |
|------|---------|
| `src/components/ErrorBoundary.tsx` | React Error Boundary for rendering errors |
| `src/components/RouteErrorComponent.tsx` | TanStack Router error component |
| `src/routes/__root.tsx` | Global error handling setup |
| `src/api/client.ts` | `ApiClientError` class for API errors |

### Error Boundary Usage

In `__root.tsx`:
```tsx
import { ErrorBoundary } from '../components/ErrorBoundary';
import { RouteErrorComponent } from '../components/RouteErrorComponent';

export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: RootLayout,
  errorComponent: RouteErrorComponent  // Route errors
});

function RootLayout() {
  return (
    <div>
      <Header />
      <main>
        <ErrorBoundary>   {/* Rendering errors */}
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  );
}
```

### Error Boundary vs Route Error Component

| Feature | ErrorBoundary | RouteErrorComponent |
|---------|--------------|---------------------|
| Catches | Rendering errors | Route loading errors |
| Type | Class component | Function component |
| Reset | Manual state reset | `reset()` from props |
| Scope | Any React subtree | Route-level |

---

## Key Takeaways

1. **Error Boundaries** catch rendering errors in child components
2. **Route errorComponent** handles route loading failures
3. **useQuery/useMutation** provide `isError` and `error` states
4. **Try/catch** is still needed for event handlers and async code
5. **Always show user-friendly messages** - don't expose raw error stacks
6. **Provide recovery options** - "Try Again" buttons, navigation links

---

## Next: Forms in React

Controlled inputs, form state, and handling user input.
