# 12 - Form Validation

Client-side validation with React Hook Form and Zod.

## Table of Contents
- [Why Client-Side Validation?](#why-client-side-validation)
- [The Modern Stack: React Hook Form + Zod](#the-modern-stack-react-hook-form--zod)
- [Zod Schema Basics](#zod-schema-basics)
- [React Hook Form Basics](#react-hook-form-basics)
- [Connecting RHF and Zod](#connecting-rhf-and-zod)
- [Validation Timing](#validation-timing)
- [Displaying Validation Errors](#displaying-validation-errors)
- [Conditional Validation](#conditional-validation)
- [Server-Side Validation Errors](#server-side-validation-errors)
- [Implementation Reference](#implementation-reference)

---

## Why Client-Side Validation?

Client-side validation provides immediate feedback to users before data reaches the server.

### Benefits

1. **Better UX** - Users see errors immediately, not after a round-trip to the server
2. **Reduced server load** - Invalid requests never reach your backend
3. **Faster feedback** - No network delay for validation errors

### Important Caveat

Client-side validation is **NOT a security measure**. Users can bypass it by:
- Disabling JavaScript
- Modifying the DOM
- Sending requests directly to the API

**Always validate on the server too.** Client-side validation is for UX; server-side validation is for security.

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────────┐
│ User Input                                                       │
│     ↓                                                            │
│ ┌───────────────────────────────────────────────────────────┐   │
│ │ Client-Side Validation (UX)                                │   │
│ │ • Immediate feedback                                       │   │
│ │ • Prevents obvious mistakes                                │   │
│ │ • Can be bypassed                                          │   │
│ └───────────────────────────────────────────────────────────┘   │
│     ↓                                                            │
│ ┌───────────────────────────────────────────────────────────┐   │
│ │ Server-Side Validation (Security)                          │   │
│ │ • Cannot be bypassed                                       │   │
│ │ • Enforces business rules                                  │   │
│ │ • Final authority                                          │   │
│ └───────────────────────────────────────────────────────────┘   │
│     ↓                                                            │
│ Database                                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## The Modern Stack: React Hook Form + Zod

### Why This Combination?

| Library | Role | Why We Use It |
|---------|------|---------------|
| **React Hook Form** | Form state management | Handles inputs, errors, submission, touched state |
| **Zod** | Schema validation | Type-safe validation rules with TypeScript inference |
| **@hookform/resolvers** | Bridge | Connects RHF to Zod (and other validation libraries) |

### Installation

```bash
npm install react-hook-form zod @hookform/resolvers
```

### How They Work Together

```
┌──────────────────────────────────────────────────────────────────┐
│ Zod Schema                                                        │
│ ┌────────────────────────────────────────────────────────────┐   │
│ │ Defines validation rules                                    │   │
│ │ Provides TypeScript types via z.infer<>                     │   │
│ └────────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│ @hookform/resolvers (zodResolver)                                │
│ ┌────────────────────────────────────────────────────────────┐   │
│ │ Runs Zod validation                                         │   │
│ │ Maps Zod errors to RHF error format                         │   │
│ └────────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│ React Hook Form (useForm)                                        │
│ ┌────────────────────────────────────────────────────────────┐   │
│ │ Manages form state                                          │   │
│ │ Provides register(), handleSubmit(), errors                 │   │
│ │ Tracks touched/dirty state                                  │   │
│ └────────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│ Your Form Component                                               │
│ ┌────────────────────────────────────────────────────────────┐   │
│ │ Uses register() on inputs                                   │   │
│ │ Displays errors.fieldName.message                           │   │
│ │ Calls handleSubmit(onSubmit)                                │   │
│ └────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Zod Schema Basics

Zod is a TypeScript-first schema validation library.

### Defining a Schema

```tsx
import { z } from 'zod'

const userSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Invalid email address'),
  age: z.number().min(18, 'Must be 18 or older').optional(),
})
```

### Common Validators

| Validator | Description | Example |
|-----------|-------------|---------|
| `.string()` | Must be a string | `z.string()` |
| `.number()` | Must be a number | `z.number()` |
| `.boolean()` | Must be boolean | `z.boolean()` |
| `.min(n)` | Minimum length/value | `z.string().min(1)` |
| `.max(n)` | Maximum length/value | `z.string().max(100)` |
| `.email()` | Valid email format | `z.string().email()` |
| `.optional()` | Field can be undefined | `z.string().optional()` |
| `.nullable()` | Field can be null | `z.number().nullable()` |
| `.enum([])` | Must be one of values | `z.enum(['A', 'B', 'C'])` |

### Custom Error Messages

Every validator accepts a custom error message:

```tsx
z.string()
  .min(1, 'Title is required')           // Custom message for min
  .max(100, 'Title is too long')         // Custom message for max
```

### Type Inference

Zod can infer TypeScript types from schemas:

```tsx
const taskSchema = z.object({
  title: z.string(),
  status: z.enum(['TODO', 'IN_PROGRESS', 'COMPLETED']),
})

// Inferred type:
// { title: string; status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED' }
type Task = z.infer<typeof taskSchema>
```

This eliminates the need to define types separately from validation rules.

---

## React Hook Form Basics

React Hook Form (RHF) manages form state with minimal re-renders.

### The useForm Hook

```tsx
import { useForm } from 'react-hook-form'

function MyForm() {
  const {
    register,      // Connects inputs to form state
    handleSubmit,  // Wraps submit handler with validation
    watch,         // Subscribe to field changes
    setValue,      // Programmatically set values
    formState: {
      errors,      // Validation errors by field name
      isSubmitting, // True during async submission
      isDirty,     // True if form has been modified
      isValid,     // True if form passes validation
    },
  } = useForm()
}
```

### Registering Inputs

```tsx
<input {...register('email')} />

// register('email') returns:
// {
//   name: 'email',
//   ref: (ref) => { ... },
//   onChange: (e) => { ... },
//   onBlur: (e) => { ... },
// }
```

The spread operator (`...`) passes all these props to the input.

### Handling Submission

```tsx
function MyForm() {
  const { register, handleSubmit } = useForm()

  function onSubmit(data) {
    // Called ONLY if validation passes
    console.log(data)
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email')} />
      <button type="submit">Submit</button>
    </form>
  )
}
```

`handleSubmit()` validates the form before calling your `onSubmit` function.

---

## Connecting RHF and Zod

The `zodResolver` bridges React Hook Form and Zod.

### Basic Setup

```tsx
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

// 1. Define the schema
const schema = z.object({
  title: z.string().min(1, 'Title is required'),
  description: z.string().optional(),
})

// 2. Infer the type
type FormData = z.infer<typeof schema>

// 3. Use in component
function TaskForm() {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),  // Connect Zod
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('title')} />
      {errors.title && <p>{errors.title.message}</p>}
    </form>
  )
}
```

### Default Values

Pre-populate the form (useful for edit mode):

```tsx
const { register } = useForm<FormData>({
  resolver: zodResolver(schema),
  defaultValues: {
    title: existingTask?.title ?? '',
    description: existingTask?.description ?? '',
  },
})
```

---

## Validation Timing

When should validation run? React Hook Form provides several modes.

### mode Options

| Mode | When Validation Runs | UX Impact |
|------|---------------------|-----------|
| `'onSubmit'` | Only on form submit | Errors appear late |
| `'onBlur'` | When user leaves a field | **Recommended** - natural checkpoint |
| `'onChange'` | On every keystroke | Too aggressive, can be distracting |
| `'onTouched'` | On blur, then on change | Good balance |
| `'all'` | On blur AND change | Most responsive |

### Our Choice: onBlur

```tsx
useForm({
  resolver: zodResolver(schema),
  mode: 'onBlur',  // Validate when user leaves field
})
```

**Why onBlur?**
- Validates at a natural checkpoint (user finished typing)
- Not as aggressive as onChange (no errors while still typing)
- Provides feedback before user tries to submit

### The Pattern: Lazy Validation with Eager Re-validation

Once a field shows an error:
1. User sees the error on blur
2. User starts fixing the error
3. Error clears on next blur (or on change if using `mode: 'onTouched'`)

---

## Displaying Validation Errors

### Error Message Pattern

```tsx
<div>
  <label htmlFor="title">Title</label>
  <input
    id="title"
    {...register('title')}
    className={errors.title ? 'border-red-500' : 'border-gray-300'}
  />
  {errors.title && (
    <p className="text-red-600 text-sm mt-1">{errors.title.message}</p>
  )}
</div>
```

### Visual Indicators

1. **Red border** on the input when invalid
2. **Error message** below the field
3. **Red asterisk** on required field labels (always visible)

```
┌─────────────────────────────────────────────┐
│ Title *                                     │  ← Label with required indicator
│ ┌─────────────────────────────────────────┐ │
│ │                                         │ │  ← Red border when invalid
│ └─────────────────────────────────────────┘ │
│ Title is required                           │  ← Error message
└─────────────────────────────────────────────┘
```

### Conditional Styling

```tsx
<input
  {...register('title')}
  className={`base-styles ${errors.title ? 'border-red-500' : 'border-gray-300'}`}
/>
```

---

## Conditional Validation

Sometimes validation rules differ based on context (e.g., create vs edit).

### Pattern: Multiple Schemas

```tsx
// Create schema - future date required
const taskCreateSchema = z.object({
  title: z.string().min(1),
  dueDate: z.string().optional(),
}).refine(
  (data) => {
    if (data.dueDate) {
      return new Date(data.dueDate) > new Date()
    }
    return true
  },
  { message: 'Due date must be in the future', path: ['dueDate'] }
)

// Edit schema - any date allowed
const taskEditSchema = z.object({
  title: z.string().min(1),
  dueDate: z.string().optional(),
})
```

### Using the Right Schema

```tsx
function TaskForm({ task }: { task?: Task }) {
  const isEditing = !!task

  const { register } = useForm({
    resolver: zodResolver(isEditing ? taskEditSchema : taskCreateSchema),
  })
}
```

### The refine() Method

For validation that depends on multiple fields:

```tsx
z.object({
  password: z.string().min(8),
  confirmPassword: z.string(),
}).refine(
  (data) => data.password === data.confirmPassword,
  {
    message: "Passwords don't match",
    path: ['confirmPassword'],  // Which field shows the error
  }
)
```

---

## Server-Side Validation Errors

The server may return validation errors we didn't catch client-side.

### Current Approach

We display server errors in a general error banner:

```tsx
{mutation.isError && (
  <div className="bg-red-50 border border-red-200 rounded-md p-3">
    <p className="text-red-800 text-sm">
      {mutation.error instanceof Error
        ? mutation.error.message
        : 'An error occurred. Please try again.'}
    </p>
  </div>
)}
```

### Future Enhancement: Field-Level Server Errors

If the server returns field-specific errors:

```json
{
  "errors": {
    "email": "Email already exists",
    "username": "Username is taken"
  }
}
```

You could map them to form fields using `setError`:

```tsx
const { setError } = useForm()

// After catching a server error
if (serverErrors) {
  Object.entries(serverErrors).forEach(([field, message]) => {
    setError(field, { message })
  })
}
```

This is beyond our current scope but good to know for more complex apps.

---

## Implementation Reference

### Files Created

| File | Purpose |
|------|---------|
| `src/schemas/task.ts` | Zod schemas for task validation |
| `src/schemas/project.ts` | Zod schema for project validation |

### Files Modified

| File | Changes |
|------|---------|
| `src/components/TaskForm.tsx` | useForm + zodResolver integration |
| `src/components/ProjectForm.tsx` | useForm + zodResolver integration |

### Validation Rules

**TaskForm:**
- title: Required, 1-100 characters
- description: Optional, max 500 characters
- dueDate: Future date required (create mode only)

**ProjectForm:**
- name: Required, 1-100 characters
- description: Optional, max 500 characters

---

## Key Takeaways

1. **React Hook Form** manages form state; **Zod** defines validation rules
2. **zodResolver** bridges the two libraries
3. **mode: 'onBlur'** provides balanced validation timing
4. **z.infer<typeof schema>** gives you TypeScript types from schemas
5. **Multiple schemas** handle conditional validation (create vs edit)
6. **refine()** enables cross-field validation
7. **Client validation is for UX**, not security - always validate on server too

---

## Next: Styling with Tailwind

Responsive design and component styling patterns.
