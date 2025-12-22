# 02 - TanStack Router Basics

This document covers TanStack Router fundamentals: file-based routing, navigation, and route parameters.

## Table of Contents
- [Understanding the Generated Code](#understanding-the-generated-code)
- [File-Based Routing](#file-based-routing)
- [Creating Routes](#creating-routes)
- [Navigation](#navigation)
- [Route Parameters](#route-parameters)
- [Nested Routes and Layouts](#nested-routes-and-layouts)
- [Implementation Reference](#implementation-reference)

---

## Understanding the Generated Code

Before creating new routes, let's understand what was scaffolded.

### `main.tsx` - Application Entry Point

```tsx
import { StrictMode } from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider, createRouter } from '@tanstack/react-router'

import * as TanStackQueryProvider from './integrations/tanstack-query/root-provider.tsx'
import { routeTree } from './routeTree.gen'  // Auto-generated!

import './styles.css'

// 1. Get the QueryClient context to share with routes
const TanStackQueryProviderContext = TanStackQueryProvider.getContext()

// 2. Create the router instance
const router = createRouter({
  routeTree,                              // The auto-generated route tree
  context: {
    ...TanStackQueryProviderContext,      // Share QueryClient with all routes
  },
  defaultPreload: 'intent',               // Prefetch on hover (snappy navigation!)
  scrollRestoration: true,                // Remember scroll position
  defaultStructuralSharing: true,         // Optimize re-renders
  defaultPreloadStaleTime: 0,             // Always prefetch fresh data
})

// 3. Register router for TypeScript (enables type-safe navigation)
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

// 4. Render the app
const rootElement = document.getElementById('app')
if (rootElement && !rootElement.innerHTML) {
  const root = ReactDOM.createRoot(rootElement)
  root.render(
    <StrictMode>
      {/* QueryProvider wraps RouterProvider - order matters! */}
      <TanStackQueryProvider.Provider {...TanStackQueryProviderContext}>
        <RouterProvider router={router} />
      </TanStackQueryProvider.Provider>
    </StrictMode>,
  )
}
```

Key concepts:
- `createRouter()` - Creates the router instance with configuration
- `routeTree` - Auto-generated from `src/routes/` folder
- `context` - Shared data available to all routes (QueryClient)
- `defaultPreload: 'intent'` - Prefetches routes on hover
- Type registration - Enables type-safe navigation

### `routes/__root.tsx` - Root Layout

```tsx
import { Outlet, createRootRouteWithContext } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'

// Define what context is available to ALL routes
interface MyRouterContext {
  queryClient: QueryClient
}

// Create the root route with typed context
export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: RootLayout,
})

function RootLayout() {
  return (
    <div>
      {/* Persistent UI goes here (header, sidebar, etc.) */}
      <header>My App</header>

      <main>
        {/*
         * <Outlet /> - Where child routes render
         *
         * Navigate to "/" → HomePage renders here
         * Navigate to "/posts" → PostsPage renders here
         * Navigate to "/posts/123" → PostDetailPage renders here
         *
         * Everything outside <Outlet /> stays in place.
         */}
        <Outlet />
      </main>
    </div>
  )
}
```

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

### How It Works (The "Magic" Explained)

File-based routing isn't actually magic - it's powered by a **Vite plugin** that watches your files and generates code.

Here's what happens:

1. **You create a file** in `src/routes/` (e.g., `about.tsx`)
2. **The Vite plugin detects it** and reads the file
3. **It generates `routeTree.gen.ts`** with all your routes wired up
4. **The router uses this generated file** to know what routes exist

This is configured in `vite.config.ts`:

```ts
import { tanstackRouter } from '@tanstack/router-plugin/vite'

export default defineConfig({
  plugins: [
    tanstackRouter({
      target: 'react',
      autoCodeSplitting: true,
      // These are the defaults - you can customize:
      // routesDirectory: './src/routes',
      // generatedRouteTree: './src/routeTree.gen.ts',
    }),
    // ... other plugins
  ],
})
```

**Key configuration options:**

| Option | Default | Purpose |
|--------|---------|---------|
| `routesDirectory` | `./src/routes` | Where your route files live |
| `generatedRouteTree` | `./src/routeTree.gen.ts` | Where the auto-generated file goes |
| `autoCodeSplitting` | `false` | Lazy-load routes for better performance |

So `src/routes` isn't a magic folder name - it's just the default. You could change it to `src/pages` or `src/views` if you prefer.

### When Does Generation Happen?

The route tree is generated when:

| Scenario | What Happens |
|----------|--------------|
| `npm run dev` starts | Plugin scans routes, generates `routeTree.gen.ts` |
| File added/changed/deleted (dev server running) | Plugin regenerates, HMR updates browser |
| `npm run build` | Plugin generates before building |
| Dev server NOT running | Nothing - changes won't be picked up until next start |

**Important:** If you move or rename route files while the dev server is running, you may need to **restart the dev server** for the route tree to regenerate correctly. This is especially true for structural changes like moving `posts.tsx` to `posts/index.tsx`.

**Manual generation** (useful if things get out of sync):
```bash
npx tsr generate
```

### Basic Conventions

| File | Route Path |
|------|------------|
| `routes/index.tsx` | `/` |
| `routes/about.tsx` | `/about` |
| `routes/posts.tsx` | `/posts` |
| `routes/posts/index.tsx` | `/posts` (same as above) |
| `routes/posts/$postId.tsx` | `/posts/:postId` |

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

export const Route = createFileRoute('/posts')({
  component: PostsPage,
})

function PostsPage() {
  return <div>All Posts</div>
}
```

### Route Configuration Options

The `createFileRoute()` function accepts a configuration object with several options:

```tsx
export const Route = createFileRoute('/posts')({
  // Required
  component: PostsPage,           // The React component to render

  // Optional - Data Loading
  loader: async () => {           // Fetch data before rendering
    const posts = await fetchPosts()
    return { posts }
  },

  // Optional - Loading & Error States
  pendingComponent: Loading,      // Shown while loader is running
  errorComponent: ErrorDisplay,   // Shown if loader throws an error

  // Optional - Other
  beforeLoad: async () => {},     // Runs before loader (auth checks, redirects)
  validateSearch: (search) => {}, // Validate/parse URL search params
})
```

| Option | Purpose |
|--------|---------|
| `component` | The React component to render for this route |
| `loader` | Async function to fetch data before rendering (data available via `Route.useLoaderData()`) |
| `pendingComponent` | Component shown while `loader` is running |
| `errorComponent` | Component shown if `loader` throws an error |
| `beforeLoad` | Runs before `loader` - useful for auth checks or redirects |
| `validateSearch` | Parse and validate URL search parameters |

**Note:** While TanStack Router has built-in `loader` support, this project uses TanStack Query's `useQuery` for data fetching instead. This gives us more control over caching, background refetching, and mutation handling. See `docs/03-tanstack-query.md` for details.

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
      <Link to="/posts">Posts</Link>
      <Link to="/about">About</Link>
    </nav>
  )
}
```

### Active Link Styling

```tsx
<Link
  to="/posts"
  activeProps={{ className: 'font-bold text-blue-600' }}
  inactiveProps={{ className: 'text-gray-600' }}
>
  Posts
</Link>
```

The `activeOptions` prop controls what counts as "active":

```tsx
<Link
  to="/"
  activeOptions={{ exact: true }}  // Only active on exact "/" match
  activeProps={{ className: 'active' }}
>
  Home
</Link>
```

Without `exact: true`, the "/" link would be active on every page since all paths start with "/".

### Programmatic Navigation

```tsx
import { useNavigate } from '@tanstack/react-router'

function SomeComponent() {
  const navigate = useNavigate()

  const handleClick = () => {
    navigate({ to: '/posts' })
  }

  return <button onClick={handleClick}>Go to Posts</button>
}
```

---

## Route Parameters

### Defining Dynamic Routes

Use `$paramName` in the file name:

```
routes/users/$userId.tsx  →  /users/:userId
```

### Accessing Parameters

```tsx
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/users/$userId')({
  component: UserProfilePage,
})

function UserProfilePage() {
  const { userId } = Route.useParams()

  return <div>User ID: {userId}</div>
}
```

### Type Safety

Parameters are fully typed! TypeScript knows `userId` is a string.

**Important:** Parameters are always strings, even if they look like numbers in the URL. If you need a number, parse it:

```tsx
const { userId } = Route.useParams()
const userIdNum = parseInt(userId, 10)
```

### Linking with Parameters

```tsx
<Link to="/users/$userId" params={{ userId: '123' }}>
  View Profile
</Link>
```

### Multiple Parameters

For routes with multiple dynamic segments, stack `$param` folders:

```
routes/orgs/$orgId/members/$memberId.tsx
→ /orgs/:orgId/members/:memberId
```

```tsx
export const Route = createFileRoute('/orgs/$orgId/members/$memberId')({
  component: MemberDetailPage,
})

function MemberDetailPage() {
  // All params available and typed
  const { orgId, memberId } = Route.useParams()

  return <div>Org {orgId}, Member {memberId}</div>
}
```

Linking requires all params:

```tsx
<Link
  to="/orgs/$orgId/members/$memberId"
  params={{ orgId: '1', memberId: '42' }}
>
  View Member
</Link>
```

TypeScript enforces you provide **all** required params - forget one and you get a compile error.

---

## Nested Routes and Layouts

### Folder Structure = Nesting

```
routes/
├── dashboard.tsx           # /dashboard (or layout for /dashboard/*)
├── dashboard/
│   ├── index.tsx           # /dashboard (if dashboard.tsx is layout)
│   ├── settings.tsx        # /dashboard/settings
│   └── $widgetId.tsx       # /dashboard/:widgetId
```

### Sibling Routes vs Layout Routes

**Common gotcha:** If you have both `posts.tsx` and `posts/$postId.tsx`, TanStack Router treats `posts.tsx` as a **layout** that wraps `posts/$postId.tsx`. If `posts.tsx` doesn't have an `<Outlet />`, the child route content won't render!

**Problem structure:**
```
routes/
├── posts.tsx           # Layout for /posts/* (needs <Outlet />!)
└── posts/
    └── $postId.tsx     # Child route - won't render without Outlet in parent
```

**Solution - Use index.tsx for sibling routes:**
```
routes/
└── posts/
    ├── index.tsx       # /posts - the list page
    └── $postId.tsx     # /posts/:postId - the detail page
```

With this structure, both routes are **siblings** under the same folder, not parent-child. Neither needs an `<Outlet />`.

### Why `index.tsx`?

The name `index.tsx` is special - it means "this is the route for the folder itself" (similar to `index.html` in web servers). When you navigate to `/posts`, TanStack Router looks for `posts/index.tsx`.

**When to use each pattern:**

| Pattern | Structure | Use Case |
|---------|-----------|----------|
| `posts.tsx` + `posts/*.tsx` | Parent-child | Shared layout (sidebar, tabs) that stays visible across all `/posts/*` pages |
| `posts/index.tsx` + `posts/*.tsx` | Siblings | Independent pages with no shared UI between list and detail |

**Example: When you WOULD want a layout:**

```tsx
// routes/dashboard.tsx - Layout with persistent sidebar
function DashboardLayout() {
  return (
    <div className="flex">
      <Sidebar />           {/* Always visible */}
      <main>
        <Outlet />          {/* Child routes render here */}
      </main>
    </div>
  )
}
```

Navigate to `/dashboard/settings` → Sidebar stays, settings page renders in `<Outlet />`.

**Example: When you want sibling routes (our approach):**

List page and detail page are completely independent - navigating from `/tasks` to `/tasks/123` replaces the entire page content. No shared UI needed.

### Layout Routes

If a route file contains an `<Outlet />`, it becomes a layout for its child routes:

```tsx
// routes/dashboard.tsx - Layout
import { createFileRoute, Outlet } from '@tanstack/react-router'

export const Route = createFileRoute('/dashboard')({
  component: DashboardLayout,
})

function DashboardLayout() {
  return (
    <div>
      <h1>Dashboard</h1>
      <nav>
        <Link to="/dashboard">Overview</Link>
        <Link to="/dashboard/settings">Settings</Link>
      </nav>
      <Outlet /> {/* Child routes render here */}
    </div>
  )
}
```

When you navigate to `/dashboard/settings`, the `DashboardLayout` stays in place and `<Outlet />` renders the settings page.

---

## Implementation Reference

This section lists the actual files in our Task Manager app. Refer to the source files for complete implementations.

| File | URL | Description |
|------|-----|-------------|
| `src/routes/__root.tsx` | (all) | Root layout with Header and `<Outlet />` |
| `src/routes/index.tsx` | `/` | Home page |
| `src/routes/tasks/index.tsx` | `/tasks` | Task list with links to details |
| `src/routes/tasks/$taskId.tsx` | `/tasks/:taskId` | Task detail using `Route.useParams()` |
| `src/routes/projects/index.tsx` | `/projects` | Project list with links to details |
| `src/routes/projects/$projectId.tsx` | `/projects/:projectId` | Project detail using `Route.useParams()` |
| `src/components/Header.tsx` | — | Navigation with `<Link>` and `activeProps` |

Note: We use `tasks/index.tsx` instead of `tasks.tsx` so that the list page and detail page are **sibling routes**, not parent-child. This avoids needing an `<Outlet />` in the list page.

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
