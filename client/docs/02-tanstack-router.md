# 02 - TanStack Router Basics

This document covers TanStack Router fundamentals: file-based routing, navigation, and route parameters.

## Table of Contents
- [Understanding the Generated Code](#understanding-the-generated-code)
- [File-Based Routing](#file-based-routing)
- [Creating Routes](#creating-routes)
- [Navigation](#navigation)
- [Route Parameters](#route-parameters)
- [Nested Routes and Layouts](#nested-routes-and-layouts)
- [Hands-On: Build Task Manager Routes](#hands-on-build-task-manager-routes)

---

## Understanding the Generated Code

Before creating new routes, let's understand what was scaffolded.

### `main.tsx` - Application Entry Point

<!-- TODO: Code walkthrough -->

Key concepts:
- `createRouter()` - Creates the router instance with configuration
- `routeTree` - Auto-generated from `src/routes/` folder
- `context` - Shared data available to all routes (QueryClient)
- `defaultPreload: 'intent'` - Prefetches routes on hover
- Type registration - Enables type-safe navigation

### `routes/__root.tsx` - Root Layout

<!-- TODO: Code walkthrough -->

Key concepts:
- `createRootRouteWithContext<T>()` - Root route with typed context
- `<Outlet />` - Renders child routes (like Vue's `<router-view>`)
- Every route is a child of `__root.tsx`

### `routeTree.gen.ts` - Auto-Generated

**Never edit this file manually!**

The TanStack Router Vite plugin watches `src/routes/` and regenerates this file when:
- You add a new route file
- You rename or delete a route file
- You change route configuration

---

## File-Based Routing

TanStack Router uses file naming conventions to define routes.

### Basic Conventions

| File | Route Path |
|------|------------|
| `routes/index.tsx` | `/` |
| `routes/about.tsx` | `/about` |
| `routes/tasks.tsx` | `/tasks` |
| `routes/tasks/index.tsx` | `/tasks` (same as above) |
| `routes/tasks/$taskId.tsx` | `/tasks/:taskId` |

### Special Files

| File | Purpose |
|------|---------|
| `__root.tsx` | Root layout (wraps all routes) |
| `_layout.tsx` | Shared layout for sibling routes |
| `$.tsx` | Catch-all / 404 route |

### Route File Structure

Every route file exports a `Route` constant:

```tsx
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/tasks')({
  component: TasksPage,
})

function TasksPage() {
  return <div>Tasks</div>
}
```

---

## Creating Routes

### Step 1: Create the File

Create a new file in `src/routes/`. The file name determines the URL path.

### Step 2: Define the Route

```tsx
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/your-path')({
  component: YourComponent,
})

function YourComponent() {
  return <div>Your content</div>
}
```

### Step 3: Route Tree Auto-Updates

The Vite plugin automatically:
1. Detects the new file
2. Regenerates `routeTree.gen.ts`
3. Hot-reloads the app

---

## Navigation

### Using `<Link>` Component

```tsx
import { Link } from '@tanstack/react-router'

function Navigation() {
  return (
    <nav>
      <Link to="/">Home</Link>
      <Link to="/tasks">Tasks</Link>
      <Link to="/projects">Projects</Link>
    </nav>
  )
}
```

### Active Link Styling

```tsx
<Link
  to="/tasks"
  activeProps={{ className: 'font-bold text-blue-600' }}
  inactiveProps={{ className: 'text-gray-600' }}
>
  Tasks
</Link>
```

### Programmatic Navigation

```tsx
import { useNavigate } from '@tanstack/react-router'

function SomeComponent() {
  const navigate = useNavigate()

  const handleClick = () => {
    navigate({ to: '/tasks' })
  }

  return <button onClick={handleClick}>Go to Tasks</button>
}
```

---

## Route Parameters

### Defining Dynamic Routes

Use `$paramName` in the file name:

```
routes/tasks/$taskId.tsx  →  /tasks/:taskId
```

### Accessing Parameters

```tsx
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/tasks/$taskId')({
  component: TaskDetailPage,
})

function TaskDetailPage() {
  const { taskId } = Route.useParams()

  return <div>Task ID: {taskId}</div>
}
```

### Type Safety

Parameters are fully typed! TypeScript knows `taskId` is a string.

### Linking with Parameters

```tsx
<Link to="/tasks/$taskId" params={{ taskId: '123' }}>
  View Task
</Link>
```

---

## Nested Routes and Layouts

### Folder Structure = Nesting

```
routes/
├── tasks.tsx           # /tasks (or layout for /tasks/*)
├── tasks/
│   ├── index.tsx       # /tasks (if tasks.tsx is layout)
│   └── $taskId.tsx     # /tasks/:taskId
```

### Layout Routes

If `tasks.tsx` contains an `<Outlet />`, it becomes a layout:

```tsx
// routes/tasks.tsx - Layout
export const Route = createFileRoute('/tasks')({
  component: TasksLayout,
})

function TasksLayout() {
  return (
    <div>
      <h1>Tasks</h1>
      <Outlet /> {/* Child routes render here */}
    </div>
  )
}
```

---

## Hands-On: Build Task Manager Routes

### Goal

Create this route structure:
- `/` - Home page (done)
- `/tasks` - Task list
- `/tasks/$taskId` - Task detail
- `/projects` - Project list
- `/projects/$projectId` - Project detail

### Tasks

1. Create `routes/tasks.tsx` (or `routes/tasks/index.tsx`)
2. Create `routes/tasks/$taskId.tsx`
3. Create `routes/projects.tsx` (or `routes/projects/index.tsx`)
4. Create `routes/projects/$projectId.tsx`
5. Build a navigation header component
6. Add `<Link>` components to navigate between routes

---

## Key Takeaways

1. **File name = URL path** - No manual route configuration needed
2. **`$param`** - Dynamic route segments
3. **`__root.tsx`** - Wraps all routes, always renders
4. **`<Outlet />`** - Where child routes render
5. **`<Link>`** - Type-safe navigation
6. **Type safety** - Params and paths are fully typed
7. **Auto-generation** - Never edit `routeTree.gen.ts`

---

## Comparison: Vue Router vs TanStack Router

| Concept | Vue Router | TanStack Router |
|---------|------------|-----------------|
| Route definition | `router/index.ts` config | File-based (`src/routes/`) |
| Child rendering | `<router-view>` | `<Outlet />` |
| Navigation | `<router-link>` | `<Link>` |
| Programmatic nav | `router.push()` | `navigate()` |
| Route params | `$route.params` | `Route.useParams()` |
| Active class | `router-link-active` | `activeProps` |

---

## Next: TanStack Query Basics

In the next section, we'll learn:
- Fetching data with `useQuery`
- Route loaders for prefetching
- Caching and background refetching
- Loading and error states
