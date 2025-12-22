# 04 - TypeScript with React

Props typing, generics, and type inference in React components.

## Table of Contents
- [Why TypeScript?](#why-typescript)
- [Typing Props](#typing-props)
- [Typing State](#typing-state)
- [Typing Events](#typing-events)
- [Generics in Components](#generics-in-components)
- [Type Inference](#type-inference)
- [Common Patterns](#common-patterns) (includes `unknown` vs `any`)
- [Utility Types](#utility-types)
- [Implementation Reference](#implementation-reference)

---

## Why TypeScript?

TypeScript adds static type checking to JavaScript, catching errors at compile time rather than runtime.

### Benefits for React Development

1. **Catch errors early** - Typos, wrong prop types, missing properties caught before running
2. **Better IDE support** - Autocomplete, inline docs, refactoring tools
3. **Self-documenting code** - Types serve as documentation
4. **Safer refactoring** - Change a type, see all affected code immediately
5. **Better collaboration** - Types clarify component contracts

### Example: Catching Errors

```tsx
// Without TypeScript - fails silently at runtime
<UserCard naem="John" />  // typo in prop name

// With TypeScript - caught at compile time
<UserCard naem="John" />
//        ~~~~ Error: 'naem' does not exist. Did you mean 'name'?
```

---

## Typing Props

### Interface vs Type

Both work for props. Use interfaces for objects, types for unions/primitives:

```tsx
// Interface - preferred for component props
interface UserCardProps {
  name: string
  email: string
  isActive?: boolean  // Optional prop
}

// Type - good for unions
type Status = 'pending' | 'success' | 'error'
type ButtonVariant = 'primary' | 'secondary' | 'danger'
```

### Required vs Optional Props

```tsx
interface CardProps {
  title: string        // Required - must be provided
  subtitle?: string    // Optional - may be undefined
}

function Card({ title, subtitle }: CardProps) {
  return (
    <div>
      <h2>{title}</h2>
      {subtitle && <p>{subtitle}</p>}  {/* Check before using optional */}
    </div>
  )
}
```

### Children Prop

```tsx
import type { ReactNode } from 'react'

interface ContainerProps {
  children: ReactNode  // Any valid React child
  className?: string
}

function Container({ children, className }: ContainerProps) {
  return <div className={className}>{children}</div>
}
```

### Destructuring with Types

```tsx
// Inline type (simple components)
function Badge({ label, color }: { label: string; color: string }) {
  return <span style={{ color }}>{label}</span>
}

// Separate interface (complex components)
interface TaskCardProps {
  task: Task
  onEdit?: (id: number) => void
  onDelete?: (id: number) => void
}

function TaskCard({ task, onEdit, onDelete }: TaskCardProps) {
  // ...
}
```

---

## Typing State

### useState with Types

TypeScript usually infers state types, but sometimes you need to be explicit:

```tsx
// Inferred as string
const [name, setName] = useState('John')

// Inferred as string | undefined (wrong if you want null)
const [name, setName] = useState()

// Explicit type for nullable state
const [user, setUser] = useState<User | null>(null)

// Explicit type for arrays
const [tasks, setTasks] = useState<Task[]>([])
```

### Complex State Objects

```tsx
interface FormState {
  title: string
  description: string
  status: TaskStatus
  errors: Record<string, string>
}

const [form, setForm] = useState<FormState>({
  title: '',
  description: '',
  status: 'TODO',
  errors: {},
})
```

### Union Types for State

```tsx
type LoadingState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: User[] }
  | { status: 'error'; error: string }

const [state, setState] = useState<LoadingState>({ status: 'idle' })

// TypeScript knows which properties exist based on status
if (state.status === 'success') {
  console.log(state.data)  // ✅ data exists here
}
if (state.status === 'error') {
  console.log(state.error)  // ✅ error exists here
}
```

---

## Typing Events

### Event Handler Types

```tsx
// Click events
function handleClick(event: React.MouseEvent<HTMLButtonElement>) {
  console.log(event.currentTarget.name)
}

// Change events
function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
  console.log(event.target.value)
}

// Form events
function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault()
  // ...
}
```

### Inline Event Handlers

When passing inline handlers, TypeScript infers types automatically:

```tsx
<input
  type="text"
  onChange={(e) => setName(e.target.value)}  // e is inferred
/>

<button onClick={(e) => console.log(e.clientX)}>  // e is inferred
  Click me
</button>
```

### Keyboard Events

```tsx
function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
  if (event.key === 'Enter') {
    submitForm()
  }
  if (event.key === 'Escape') {
    cancel()
  }
}
```

---

## Generics in Components

### Generic Functions

Generics let you write reusable code that works with multiple types:

```tsx
// Generic fetch wrapper - T is the return type
async function get<T>(url: string): Promise<T> {
  const response = await fetch(url)
  return response.json() as T
}

// Usage - specify what type you expect
const user = await get<User>('/api/users/1')
const tasks = await get<Task[]>('/api/tasks')
```

### Generic Components

```tsx
interface ListProps<T> {
  items: T[]
  renderItem: (item: T) => ReactNode
  keyExtractor: (item: T) => string | number
}

function List<T>({ items, renderItem, keyExtractor }: ListProps<T>) {
  return (
    <ul>
      {items.map((item) => (
        <li key={keyExtractor(item)}>{renderItem(item)}</li>
      ))}
    </ul>
  )
}

// Usage
<List
  items={tasks}
  renderItem={(task) => <span>{task.title}</span>}
  keyExtractor={(task) => task.id}
/>
```

### Constrained Generics

Limit what types can be used:

```tsx
// T must have an 'id' property
interface HasId {
  id: number | string
}

function findById<T extends HasId>(items: T[], id: T['id']): T | undefined {
  return items.find((item) => item.id === id)
}
```

---

## Type Inference

### When to Let TypeScript Infer

Let TypeScript infer when the type is obvious:

```tsx
// ✅ Good - inferred as string
const name = 'John'

// ❌ Unnecessary - redundant type annotation
const name: string = 'John'

// ✅ Good - inferred from return value
const tasks = await fetchTasks()  // If fetchTasks returns Promise<Task[]>

// ✅ Good - inferred from initial value
const [count, setCount] = useState(0)  // Inferred as number
```

### When to Be Explicit

Be explicit when TypeScript can't infer or infers wrong:

```tsx
// ✅ Explicit - initial value doesn't show full type
const [user, setUser] = useState<User | null>(null)

// ✅ Explicit - empty array doesn't reveal element type
const [items, setItems] = useState<Task[]>([])

// ✅ Explicit - function parameters need types
function handleTask(task: Task) { ... }
```

### `as const` Assertion

Makes values readonly and literal types:

```tsx
// Without as const - type is string[]
const colors = ['red', 'blue', 'green']

// With as const - type is readonly ['red', 'blue', 'green']
const colors = ['red', 'blue', 'green'] as const

// Useful for query keys - preserves exact types
export const taskKeys = {
  all: ['tasks'] as const,                    // readonly ['tasks']
  detail: (id: number) => ['tasks', id] as const,  // readonly ['tasks', number]
}
```

---

## Common Patterns

### String Literal Union Types

Better than enums for simple cases:

```tsx
// String literal union - works great with TypeScript
type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

// Usage
const status: TaskStatus = 'TODO'  // ✅
const status: TaskStatus = 'INVALID'  // ❌ Error
```

### Discriminated Unions

Objects with a common property that determines the shape:

```tsx
type ApiResponse<T> =
  | { success: true; data: T }
  | { success: false; error: string }

function handleResponse<T>(response: ApiResponse<T>) {
  if (response.success) {
    // TypeScript knows response.data exists here
    console.log(response.data)
  } else {
    // TypeScript knows response.error exists here
    console.log(response.error)
  }
}
```

### Type Narrowing

TypeScript narrows types based on checks:

```tsx
function TaskCard({ task }: { task: Task }) {
  // task.description is string | null

  if (task.description) {
    // Here TypeScript knows it's string (not null)
    return <p>{task.description.toUpperCase()}</p>
  }

  return <p>No description</p>
}

// With useQuery
const { data, isPending, isError, error } = useQuery({...})

if (isPending) {
  return <Loading />
}

if (isError) {
  return <Error message={error.message} />  // error is defined here
}

// data is defined here - TypeScript knows isPending and isError are false
return <TaskList tasks={data} />
```

### unknown vs any

Both represent "any value", but with different safety guarantees:

```tsx
// ❌ any - disables type checking entirely
function processAny(data: any) {
  console.log(data.foo.bar.baz)  // No error - crashes at runtime if wrong
  data.someMethod()               // No error - might not exist
}

// ✅ unknown - must narrow before using
function processUnknown(data: unknown) {
  console.log(data.foo)  // ❌ Error: 'data' is of type 'unknown'

  // Must check the type first
  if (typeof data === 'object' && data !== null) {
    console.log(data)  // ✅ Now TypeScript knows it's an object
  }
}
```

**When to use each:**

| Type | Use Case | Safety |
|------|----------|--------|
| `any` | Migrating JS code, temporary escape hatch | ❌ None |
| `unknown` | Accepting external/untrusted data | ✅ Must validate |

**Real example from our API client:**

```tsx
// src/api/client.ts
export async function post<T>(endpoint: string, data: unknown): Promise<T> {
  // We accept any data shape - we just serialize it
  // We don't access data's properties, so unknown is perfect
  body: JSON.stringify(data)  // JSON.stringify accepts unknown
}

// Usage - any object works
await post<Task>('/api/tasks', { title: 'New task', status: 'TODO' })
await post<User>('/api/users', { username: 'john', email: 'john@example.com' })
```

**Why `unknown` over `any` here:**
- We don't need to access the data's properties (just serialize)
- If we accidentally tried to access `data.title`, TypeScript would error
- Prevents bugs where we might misuse the parameter

**Type narrowing with unknown:**

```tsx
function handleApiResponse(response: unknown) {
  // Check if it's an object with expected shape
  if (
    typeof response === 'object' &&
    response !== null &&
    'data' in response
  ) {
    // TypeScript now knows response has a 'data' property
    console.log(response.data)
  }
}

// Or use a type guard
function isTask(value: unknown): value is Task {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    'title' in value &&
    'status' in value
  )
}

function processTask(input: unknown) {
  if (isTask(input)) {
    // TypeScript knows input is Task here
    console.log(input.title)
  }
}
```

**Rule of thumb:** Use `unknown` instead of `any` when you're accepting data from external sources (API responses, user input, JSON parsing) and need to validate it before use.

### Exhaustive Checks

Ensure all cases are handled:

```tsx
type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

function getStatusColor(status: TaskStatus): string {
  switch (status) {
    case 'TODO':
      return 'gray'
    case 'IN_PROGRESS':
      return 'blue'
    case 'COMPLETED':
      return 'green'
    case 'CANCELLED':
      return 'red'
    default:
      // This ensures all cases are handled
      // If you add a new status, TypeScript will error here
      const _exhaustive: never = status
      return _exhaustive
  }
}
```

---

## Utility Types

### Record<Keys, Type>

Create an object type with specific keys:

```tsx
// Map status to colors
const statusColors: Record<TaskStatus, string> = {
  TODO: 'gray',
  IN_PROGRESS: 'blue',
  COMPLETED: 'green',
  CANCELLED: 'red',
}

// Map status to display labels
const statusLabels: Record<TaskStatus, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}

// Usage
function StatusBadge({ status }: { status: TaskStatus }) {
  return (
    <span className={statusColors[status]}>
      {statusLabels[status]}
    </span>
  )
}
```

### Partial<Type>

Make all properties optional:

```tsx
interface Task {
  id: number
  title: string
  description: string
  status: TaskStatus
}

// All fields optional - good for update inputs
type TaskUpdateInput = Partial<Task>

// Same as:
interface TaskUpdateInput {
  id?: number
  title?: string
  description?: string
  status?: TaskStatus
}
```

### Pick<Type, Keys> and Omit<Type, Keys>

Select or exclude specific properties:

```tsx
interface User {
  id: number
  username: string
  email: string
  password: string
  createdAt: string
}

// Only include specific fields
type UserPreview = Pick<User, 'id' | 'username'>
// { id: number; username: string }

// Exclude specific fields
type UserResponse = Omit<User, 'password'>
// { id: number; username: string; email: string; createdAt: string }
```

### React.ComponentProps

Extract props from a component:

```tsx
import type { ComponentProps } from 'react'

// Get props of a native element
type ButtonProps = ComponentProps<'button'>

// Extend native element props
interface CustomButtonProps extends ComponentProps<'button'> {
  variant: 'primary' | 'secondary'
  isLoading?: boolean
}
```

---

## Where to Put Types

### The Two Main Approaches

**1. Colocated Types** - Next to the code that uses them
```
src/
├── components/
│   └── TaskCard/
│       ├── TaskCard.tsx      # Component
│       └── types.ts          # Types only used by TaskCard
├── api/
│   └── tasks.ts              # API functions + their types
```

**2. Shared Types Folder** - Centralized location
```
src/
├── types/
│   ├── api.ts                # Types matching backend DTOs
│   ├── forms.ts              # Form-related types
│   └── index.ts              # Barrel export (optional)
```

### When to Use Each

| Scenario | Where to Put Types |
|----------|-------------------|
| Used by one component only | Colocate in same file or `types.ts` next to it |
| Used by 2-3 related files | Colocate in parent folder |
| Used across the app | `src/types/` folder |
| Matches backend DTOs | `src/types/api.ts` |
| Component props | Same file as component |

### Best Practices

**1. Start colocated, extract when needed**

```tsx
// ✅ Start with types in the same file
interface TaskCardProps {
  task: Task
  onEdit: () => void
}

function TaskCard({ task, onEdit }: TaskCardProps) { ... }
```

Only move to a separate file when:
- The file gets too long
- Types are needed elsewhere

**2. API types in one place**

Types that match your backend should live together:

```
src/types/api.ts
├── User, UserCreateInput, UserUpdateInput
├── Task, TaskCreateInput, TaskUpdateInput
├── Project, ProjectCreateInput, ProjectUpdateInput
└── TaskStatus, ProjectStatus (enums)
```

This makes it easy to update when the backend changes.

**3. Export types with `type` keyword**

```tsx
// ✅ Explicit type export - clear intent, better tree-shaking
export type { Task, TaskStatus }

// ✅ Also good for inline export
export interface TaskCardProps { ... }

// ❌ Avoid mixing value and type exports ambiguously
export { Task }  // Is this a type or a class?
```

**4. Avoid barrel exports for types (controversial)**

```tsx
// src/types/index.ts
export * from './api'
export * from './forms'
```

Pros:
- Clean imports: `import { Task } from '@/types'`

Cons:
- Can slow down IDE/TypeScript in large projects
- Harder to find where a type is defined

**Recommendation:** For small-medium projects, barrel exports are fine. For large projects, import directly from the source file.

**5. Don't over-organize**

```
// ❌ Too granular
src/types/
├── task/
│   ├── status.ts
│   ├── create.ts
│   └── response.ts

// ✅ Simple and scannable
src/types/
├── api.ts        # All API-related types
└── forms.ts      # All form-related types
```

### This Project's Convention

```
src/
├── types/
│   └── api.ts              # All backend DTO types (Task, Project, User, enums)
├── api/
│   ├── client.ts           # ApiClientError class (colocated)
│   ├── tasks.ts             # taskKeys (colocated - only used with task queries)
│   └── projects.ts          # projectKeys (colocated)
├── components/
│   └── Header.tsx           # Props defined inline (simple component)
└── routes/
    └── tasks.tsx            # Helper component props inline (TaskCard, StatusBadge)
```

**Rules we follow:**
1. Backend DTOs → `src/types/api.ts`
2. Component props → same file (unless complex)
3. API-specific types → colocate in `src/api/`
4. When in doubt, colocate first

---

## Implementation Reference

TypeScript patterns used in this project:

### Type Definitions (`src/types/api.ts`)
- String literal unions for enums (`TaskStatus`, `ProjectStatus`)
- Interfaces for domain models (`Task`, `Project`, `User`)
- Separate types for create/update inputs

### Generic API Client (`src/api/client.ts`)
- Generic fetch functions: `get<T>()`, `post<T>()`, `put<T>()`
- Custom error class with typed properties

### Query Keys (`src/api/tasks.ts`, `src/api/projects.ts`)
- `as const` assertions for type-safe keys
- Factory functions returning readonly arrays

### Components
- `Record<Status, string>` for status → color/label mappings
- Type narrowing with `isPending`/`isError` checks
- Props interfaces for all components

---

## Key Takeaways

1. **Use interfaces for props**, types for unions
2. **Let TypeScript infer** when types are obvious
3. **Be explicit** with nullable state and empty arrays
4. **Use `as const`** for literal types and query keys
5. **Use `Record<K,V>`** for object mappings
6. **Narrow types** with conditional checks
7. **Use generics** for reusable functions and components

---

## Next: API Client Setup

Patterns for building a type-safe API client.
