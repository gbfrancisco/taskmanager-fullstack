# 05 - API Client Setup

Creating a typed fetch wrapper for backend communication.

## Table of Contents
- [API Client Architecture](#api-client-architecture)
- [Fetch Wrapper](#fetch-wrapper)
- [Error Handling](#error-handling)
- [API Functions](#api-functions)
- [Query Key Factories](#query-key-factories)
- [CORS and Cross-Origin Requests](#cors-and-cross-origin-requests)
- [Implementation Reference](#implementation-reference)

---

## API Client Architecture

### Why Create a Client Layer?

Instead of calling `fetch()` directly in components, we create an API client layer:

```
Components/Routes
       ↓
   useQuery / useMutation
       ↓
   API Functions (src/api/tasks.ts)
       ↓
   Fetch Wrapper (src/api/client.ts)
       ↓
   Backend API
```

**Benefits:**

1. **Single source of truth** - Base URL defined once
2. **Consistent error handling** - All requests use the same error format
3. **Type safety** - Generic functions return typed data
4. **Easy to modify** - Add auth headers in one place later
5. **Testable** - Mock the client layer in tests

### Structure

```
src/
├── types/
│   └── api.ts           # Types matching backend DTOs
├── api/
│   ├── client.ts        # Fetch wrapper (get, post, put, del)
│   ├── tasks.ts         # Task API functions + query keys
│   └── projects.ts      # Project API functions + query keys
```

---

## Fetch Wrapper

The fetch wrapper (`src/api/client.ts`) provides typed HTTP methods:

### Base Configuration

```typescript
const API_BASE_URL = 'http://localhost:8080'
```

In production, use environment variables:

```typescript
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
```

### Generic HTTP Methods

```typescript
// GET - Fetch a resource
export async function get<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
  })
  return handleResponse<T>(response)
}

// POST - Create a resource
export async function post<T>(endpoint: string, data: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(data),
  })
  return handleResponse<T>(response)
}

// PUT - Update a resource
export async function put<T>(endpoint: string, data: unknown): Promise<T>

// DELETE - Remove a resource
export async function del(endpoint: string): Promise<void>
```

### Why Generics?

The `<T>` generic lets callers specify what type they expect:

```typescript
// TypeScript knows this returns Task[]
const tasks = await get<Task[]>('/api/tasks')

// TypeScript knows this returns Task
const task = await get<Task>('/api/tasks/1')
```

---

## Error Handling

### Custom Error Class

```typescript
export class ApiClientError extends Error {
  constructor(
    message: string,
    public status: number,
    public body?: ApiError,
  ) {
    super(message)
    this.name = 'ApiClientError'
  }
}
```

### Why a Custom Error?

`fetch()` doesn't throw on 4xx/5xx responses - only on network failures. We need to:

1. Check `response.ok`
2. Parse the error body
3. Throw a meaningful error

```typescript
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    await handleErrorResponse(response)  // Throws ApiClientError
  }
  const text = await response.text()
  return text ? JSON.parse(text) : undefined
}
```

### Error Response Format

Matches the backend's `ErrorResponse`:

```typescript
interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}
```

---

## API Functions

Each domain has its own API file with functions matching backend endpoints:

### Tasks API (`src/api/tasks.ts`)

```typescript
// Queries (GET)
export function fetchTasks(): Promise<Task[]>
export function fetchTaskById(id: number): Promise<Task>
export function fetchTasksByUserId(userId: number): Promise<Task[]>
export function fetchTasksByProjectId(projectId: number): Promise<Task[]>
export function fetchTasksByStatus(status: TaskStatus): Promise<Task[]>

// Mutations (POST/PUT/DELETE)
export function createTask(input: TaskCreateInput): Promise<Task>
export function updateTask(id: number, input: TaskUpdateInput): Promise<Task>
export function deleteTask(id: number): Promise<void>
export function assignTaskToProject(taskId: number, projectId: number): Promise<Task>
```

### Projects API (`src/api/projects.ts`)

```typescript
// Queries
export function fetchProjects(): Promise<Project[]>
export function fetchProjectById(id: number): Promise<Project>
export function fetchProjectsByUserId(userId: number): Promise<Project[]>
export function searchProjectsByName(name: string): Promise<Project[]>

// Mutations
export function createProject(input: ProjectCreateInput): Promise<Project>
export function updateProject(id: number, input: ProjectUpdateInput): Promise<Project>
export function deleteProject(id: number): Promise<void>
```

---

## Query Key Factories

TanStack Query uses keys to identify cached data. We use factories for consistency:

```typescript
export const taskKeys = {
  all: ['tasks'] as const,
  lists: () => [...taskKeys.all, 'list'] as const,
  list: () => [...taskKeys.lists()] as const,
  listByUser: (userId: number) => [...taskKeys.lists(), { userId }] as const,
  listByProject: (projectId: number) => [...taskKeys.lists(), { projectId }] as const,
  details: () => [...taskKeys.all, 'detail'] as const,
  detail: (id: number) => [...taskKeys.details(), id] as const,
}
```

### Why Factories?

1. **Consistent keys** - Same structure everywhere
2. **Type-safe** - `as const` preserves literal types
3. **Easy invalidation** - Invalidate all lists at once

```typescript
// Invalidate all task lists
queryClient.invalidateQueries({ queryKey: taskKeys.lists() })

// Invalidate a specific task
queryClient.invalidateQueries({ queryKey: taskKeys.detail(1) })
```

---

## CORS and Cross-Origin Requests

### The Problem

When your frontend and backend run on different ports:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

The browser blocks responses due to **CORS (Cross-Origin Resource Sharing)**.

### Symptoms

- Network tab shows `200 OK` but with a red X
- Console shows: `Access to fetch has been blocked by CORS policy`
- Your code receives an error, not the data

### Solutions

**Option 1: Backend CORS Configuration (Recommended)**

Configure Spring Boot to allow your frontend origin using `@ConfigurationProperties`:

```java
// CorsProperties.java - Type-safe configuration binding
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    private List<String> allowedOrigins = new ArrayList<>();
    // getters and setters
}

// WebConfig.java - CORS configuration
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

```yaml
# application.yml - Proper YAML list syntax
app:
  cors:
    allowed-origins:
      - http://localhost:5173
      - http://localhost:3000
```

See `server/docs/04-rest-controllers.md` for details on why `@ConfigurationProperties` is preferred over `@Value`.

**Option 2: Vite Proxy**

Make requests appear same-origin by proxying through Vite:

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

Then use relative URLs in your client:

```typescript
const API_BASE_URL = '/api'  // Vite proxies to localhost:8080
```

**Tradeoffs:**

| Approach | Pros | Cons |
|----------|------|------|
| Backend CORS | Works in production, explicit | Requires backend changes |
| Vite Proxy | No backend changes, dev only | Doesn't work in production |

**Recommendation:** Use backend CORS configuration. It's explicit and works in all environments.

---

## Implementation Reference

Files in this project:

| File | Purpose |
|------|---------|
| `src/types/api.ts` | TypeScript types matching backend DTOs |
| `src/api/client.ts` | Fetch wrapper with error handling |
| `src/api/tasks.ts` | Task API functions + `taskKeys` factory |
| `src/api/projects.ts` | Project API functions + `projectKeys` factory |

---

## Key Takeaways

1. **Wrap fetch** in a client layer for consistency
2. **Use generics** for type-safe responses
3. **Custom errors** make error handling predictable
4. **Query key factories** ensure consistent caching
5. **CORS** must be configured when frontend/backend are on different origins
6. **Backend CORS** is preferred over Vite proxy for production compatibility

---

## Next: Route Loaders

Loading data before routes render.
