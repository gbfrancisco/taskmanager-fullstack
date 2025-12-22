# 10 - Mutations

Create, update, and delete operations with TanStack Query.

## Table of Contents
- [What are Mutations?](#what-are-mutations)
- [useMutation Hook](#usemutation-hook)
- [Mutation States](#mutation-states)
- [Cache Invalidation](#cache-invalidation)
- [Delete with Confirmation](#delete-with-confirmation)
- [Navigation After Mutation](#navigation-after-mutation)
- [Error Handling](#error-handling)
- [Implementation Reference](#implementation-reference)

---

## What are Mutations?

In TanStack Query terminology:

- **Queries** = READ operations (GET requests)
- **Mutations** = WRITE operations (POST, PUT, DELETE requests)

Queries run automatically and cache results. Mutations run on demand (when you call them) and typically invalidate cached data.

```tsx
// Query - runs automatically, caches result
const { data } = useQuery({
  queryKey: ['tasks'],
  queryFn: fetchTasks,
})

// Mutation - runs when you call mutate()
const mutation = useMutation({
  mutationFn: createTask,
})

// Trigger the mutation
mutation.mutate({ title: 'New Task', appUserId: 1 })
```

---

## useMutation Hook

### Basic Syntax

```tsx
const mutation = useMutation({
  mutationFn: createTask,  // The async function to call
  onSuccess: () => {
    // Called when mutation succeeds
  },
  onError: (error) => {
    // Called when mutation fails
  },
})
```

### Calling the Mutation

```tsx
// Fire and forget
mutation.mutate(data)

// With per-call callbacks
mutation.mutate(data, {
  onSuccess: () => console.log('Created!'),
  onError: (err) => console.error(err),
})
```

### mutate vs mutateAsync

```tsx
// mutate - fire and forget, use callbacks
mutation.mutate(data)

// mutateAsync - returns a Promise, use await
try {
  const result = await mutation.mutateAsync(data)
  console.log('Created:', result)
} catch (error) {
  console.error('Failed:', error)
}
```

**When to use each:**
- `mutate` - Most cases, cleaner with callbacks
- `mutateAsync` - When you need the result in a Promise chain

---

## Mutation States

useMutation returns state about the mutation:

```tsx
const {
  mutate,      // Function to trigger mutation
  isPending,   // True while mutation is in flight
  isSuccess,   // True after successful mutation
  isError,     // True if mutation failed
  error,       // The error object if failed
  data,        // The response data if successful
  reset,       // Reset mutation state
} = useMutation({...})
```

### Using States in UI

```tsx
<button
  onClick={() => mutation.mutate(data)}
  disabled={mutation.isPending}
>
  {mutation.isPending ? 'Saving...' : 'Save'}
</button>

{mutation.isError && (
  <p className="text-red-600">
    {mutation.error instanceof Error
      ? mutation.error.message
      : 'An error occurred'}
  </p>
)}
```

---

## Cache Invalidation

After a mutation, cached data is stale. We tell TanStack Query to refetch:

### Using queryClient

```tsx
import { useQueryClient } from '@tanstack/react-query'

function TaskForm() {
  const queryClient = useQueryClient()

  const createMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => {
      // Invalidate the tasks list - triggers refetch
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    },
  })
}
```

### Invalidation Patterns

```tsx
// Invalidate all queries starting with 'tasks'
queryClient.invalidateQueries({ queryKey: ['tasks'] })

// Invalidate just the list queries
queryClient.invalidateQueries({ queryKey: taskKeys.lists() })

// Invalidate a specific task detail
queryClient.invalidateQueries({ queryKey: taskKeys.detail(taskId) })
```

### When to Invalidate What

| Action | Invalidate |
|--------|------------|
| Create task | `taskKeys.lists()` |
| Update task | `taskKeys.lists()` + `taskKeys.detail(id)` |
| Delete task | `taskKeys.lists()` |

### Why Query Key Factories Matter

With consistent query keys, invalidation is predictable:

```tsx
// In api/tasks.ts
export const taskKeys = {
  all: ['tasks'] as const,
  lists: () => [...taskKeys.all, 'list'] as const,
  detail: (id: number) => [...taskKeys.all, 'detail', id] as const,
}

// Invalidating taskKeys.lists() catches all list variations
// Invalidating taskKeys.all catches everything task-related
```

---

## Delete with Confirmation

Deletes are destructive - always confirm with the user:

```tsx
const [showConfirm, setShowConfirm] = useState(false)

const deleteMutation = useMutation({
  mutationFn: deleteTask,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    navigate({ to: '/tasks' })  // Redirect after delete
  },
})

// UI
{showConfirm ? (
  <div className="bg-red-50 p-4 rounded">
    <p>Are you sure you want to delete this task?</p>
    <button
      onClick={() => deleteMutation.mutate(taskId)}
      disabled={deleteMutation.isPending}
    >
      {deleteMutation.isPending ? 'Deleting...' : 'Yes, Delete'}
    </button>
    <button onClick={() => setShowConfirm(false)}>
      Cancel
    </button>
  </div>
) : (
  <button onClick={() => setShowConfirm(true)}>
    Delete Task
  </button>
)}
```

---

## Navigation After Mutation

After deleting, the current resource no longer exists. Navigate away:

```tsx
import { useNavigate } from '@tanstack/react-router'

function TaskDetailPage() {
  const navigate = useNavigate()

  const deleteMutation = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      navigate({ to: '/tasks' })  // Go back to the list
    },
  })
}
```

---

## Error Handling

### Displaying Errors

```tsx
{mutation.isError && (
  <div className="bg-red-50 border border-red-200 rounded p-3">
    <p className="text-red-800">
      {mutation.error instanceof Error
        ? mutation.error.message
        : 'An error occurred. Please try again.'}
    </p>
  </div>
)}
```

### Error in onError Callback

```tsx
const mutation = useMutation({
  mutationFn: createTask,
  onError: (error) => {
    console.error('Mutation failed:', error)
    // Could show a toast notification here
  },
})
```

### Retry Logic

Unlike queries, mutations don't retry by default:

```tsx
const mutation = useMutation({
  mutationFn: createTask,
  retry: 3,  // Retry up to 3 times on failure
  retryDelay: 1000,  // Wait 1 second between retries
})
```

---

## Implementation Reference

### Create Mutation Pattern

```tsx
// In component
const createMutation = useMutation({
  mutationFn: createTask,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    onSuccess?.()  // Callback to parent (close form, etc.)
  },
})

// In form submit
function handleSubmit(e: React.FormEvent) {
  e.preventDefault()
  createMutation.mutate({
    title,
    description: description || undefined,
    status,
    appUserId: TEMP_USER_ID,
  })
}
```

### Update Mutation Pattern

```tsx
const updateMutation = useMutation({
  mutationFn: (input: { id: number; data: TaskUpdateInput }) =>
    updateTask(input.id, input.data),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    queryClient.invalidateQueries({ queryKey: taskKeys.detail(task.id) })
    onSuccess?.()
  },
})

// Usage
updateMutation.mutate({
  id: task.id,
  data: { title, description, status },
})
```

### Delete Mutation Pattern

```tsx
const deleteMutation = useMutation({
  mutationFn: deleteTask,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    navigate({ to: '/tasks' })
  },
})

// Usage
deleteMutation.mutate(taskId)
```

### Multiple Mutations Pattern

Sometimes an edit requires calling multiple endpoints. For example, updating a task's fields uses one endpoint, but changing its project uses a separate endpoint.

```tsx
// Field update mutation
const updateMutation = useMutation({
  mutationFn: (input: { id: number; data: TaskUpdateInput }) =>
    updateTask(input.id, input.data),
})

// Project assignment mutation (separate endpoints)
const projectAssignmentMutation = useMutation({
  mutationFn: async ({ taskId, newProjectId }: { taskId: number; newProjectId: number | null }) => {
    if (newProjectId === null) {
      return removeTaskFromProject(taskId)
    } else {
      return assignTaskToProject(taskId, newProjectId)
    }
  },
})

// Coordinate both in handleSubmit
async function handleSubmit(e: React.FormEvent) {
  e.preventDefault()

  try {
    // First, update task fields
    await updateMutation.mutateAsync({ id: task.id, data: { title, status } })

    // Then, if project changed, call separate endpoint
    if (projectId !== originalProjectId) {
      await projectAssignmentMutation.mutateAsync({
        taskId: task.id,
        newProjectId: projectId,
      })
    }

    // Invalidate after all mutations succeed
    queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    queryClient.invalidateQueries({ queryKey: taskKeys.detail(task.id) })

    onSuccess?.()
  } catch {
    // Errors handled by mutation.isError
  }
}

// Combine pending states for UI
const isPending = updateMutation.isPending || projectAssignmentMutation.isPending
```

**Key points:**
- Use `mutateAsync` (not `mutate`) to await completion
- Check if related data changed before calling additional mutations
- Invalidate queries after **all** mutations succeed
- Combine `isPending` from all mutations for accurate loading state

---

## Files Reference

| File | Mutations Used |
|------|----------------|
| `src/components/TaskForm.tsx` | createTask, updateTask |
| `src/components/ProjectForm.tsx` | createProject, updateProject |
| `src/routes/tasks/$taskId.tsx` | deleteTask |
| `src/routes/projects/$projectId.tsx` | deleteProject |

---

## Key Takeaways

1. **useMutation** for write operations, runs on demand
2. **Invalidate queries** after mutations to refresh cached data
3. **Query key factories** make invalidation predictable
4. **isPending** for loading states, **isError** for error states
5. **Confirm before delete** - destructive actions need user confirmation
6. **Navigate after delete** - the resource no longer exists
7. **Show errors** - display mutation errors to the user

---

## Next: Optimistic Updates

Updating the UI before the server responds for instant feedback.
