# 09 - Forms in React

Controlled inputs and form state management.

## Table of Contents
- [Controlled vs Uncontrolled](#controlled-vs-uncontrolled)
- [useState for Forms](#usestate-for-forms)
- [Form Submission](#form-submission)
- [Input Types](#input-types)
- [Select and Dropdown](#select-and-dropdown)
- [Reusable Form Components](#reusable-form-components)
- [Implementation Reference](#implementation-reference)

---

## Controlled vs Uncontrolled

React offers two approaches for handling form inputs:

### Controlled Components (What We Use)

The React state is the "single source of truth":

```tsx
function ControlledInput() {
  const [name, setName] = useState('')

  return (
    <input
      type="text"
      value={name}                          // Value comes from state
      onChange={(e) => setName(e.target.value)}  // State updates on change
    />
  )
}
```

**How it works:**
1. User types a character
2. `onChange` fires, updating state
3. React re-renders with new value
4. Input displays the updated value

**Benefits:**
- Full control over input value
- Can validate/transform on every keystroke
- Easy to reset or programmatically change

### Uncontrolled Components

The DOM maintains its own state:

```tsx
function UncontrolledInput() {
  const inputRef = useRef<HTMLInputElement>(null)

  function handleSubmit() {
    console.log(inputRef.current?.value)  // Read value directly from DOM
  }

  return <input type="text" ref={inputRef} />
}
```

**When to use uncontrolled:**
- File inputs (must be uncontrolled)
- Integration with non-React libraries
- Simple forms where you only need values on submit

**Our approach:** We use controlled components for all forms - it's more React-idiomatic and gives us full control.

---

## useState for Forms

### Single Field

```tsx
const [title, setTitle] = useState('')

<input value={title} onChange={(e) => setTitle(e.target.value)} />
```

### Multiple Fields (Separate State)

For forms with a few fields, separate useState calls are cleaner:

```tsx
const [title, setTitle] = useState('')
const [description, setDescription] = useState('')
const [status, setStatus] = useState<TaskStatus>('TODO')
```

This is what we use in TaskForm and ProjectForm - it's explicit and easy to understand.

### Multiple Fields (Object State)

For complex forms, you can use a single object:

```tsx
interface FormState {
  title: string
  description: string
  status: TaskStatus
}

const [form, setForm] = useState<FormState>({
  title: '',
  description: '',
  status: 'TODO',
})

// Update a single field
function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
  setForm(prev => ({ ...prev, [key]: value }))
}

<input
  value={form.title}
  onChange={(e) => updateField('title', e.target.value)}
/>
```

---

## Form Submission

### Preventing Default Behavior

HTML forms submit via page navigation by default. We prevent this:

```tsx
function handleSubmit(e: React.FormEvent) {
  e.preventDefault()  // Stop the page from reloading

  // Handle submission (call API, etc.)
}

<form onSubmit={handleSubmit}>
  {/* inputs */}
  <button type="submit">Submit</button>
</form>
```

### Collecting Form Data

With controlled inputs, data is already in state:

```tsx
function handleSubmit(e: React.FormEvent) {
  e.preventDefault()

  const taskData = {
    title,        // From useState
    description,  // From useState
    status,       // From useState
  }

  // Use the data...
  createTask(taskData)
}
```

---

## Input Types

### Text Input

```tsx
<input
  type="text"
  id="title"
  value={title}
  onChange={(e) => setTitle(e.target.value)}
  required
  placeholder="Enter title"
  className="..."
/>
```

### Textarea

```tsx
<textarea
  id="description"
  value={description}
  onChange={(e) => setDescription(e.target.value)}
  rows={3}
  placeholder="Enter description (optional)"
/>
```

### Date Input

HTML date inputs return `YYYY-MM-DD` format:

```tsx
const [dueDate, setDueDate] = useState('')

<input
  type="date"
  value={dueDate}
  onChange={(e) => setDueDate(e.target.value)}
/>
```

### Date + Time with Toggle

When the backend expects `LocalDateTime` (e.g., `2024-12-31T14:30:00`), you need both date and time. A good UX pattern is to show a time input with a toggle:

```tsx
const [dueDate, setDueDate] = useState('')
const [dueTime, setDueTime] = useState('')
const [includeTime, setIncludeTime] = useState(false)

// Date input
<input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />

// Time input (only shown when date is set)
{dueDate && (
  <input
    type="time"
    value={dueTime}
    onChange={(e) => setDueTime(e.target.value)}
    disabled={!includeTime}
  />
)}

// Toggle for including specific time
{dueDate && (
  <label>
    <input
      type="checkbox"
      checked={includeTime}
      onChange={(e) => setIncludeTime(e.target.checked)}
    />
    Include specific time (defaults to 00:00)
  </label>
)}
```

**On submission**, combine date and time:

```tsx
let formattedDueDate: string | undefined
if (dueDate) {
  const timeValue = includeTime && dueTime ? dueTime : '00:00'
  formattedDueDate = `${dueDate}T${timeValue}:00`  // "2024-12-31T14:30:00"
}
```

This gives users flexibility:
- **Toggle off**: Defaults to midnight (`00:00:00`)
- **Toggle on**: User can specify exact time

---

## Select and Dropdown

### Basic Select with Enum Options

```tsx
const TASK_STATUSES: { value: TaskStatus; label: string }[] = [
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const [status, setStatus] = useState<TaskStatus>('TODO')

<select
  value={status}
  onChange={(e) => setStatus(e.target.value as TaskStatus)}
>
  {TASK_STATUSES.map((s) => (
    <option key={s.value} value={s.value}>
      {s.label}
    </option>
  ))}
</select>
```

**Pattern:** Define status options as a constant array with `value` and `label`. This makes it easy to:
- Render the dropdown
- Display human-readable labels
- Map between API values and UI labels

---

## Reusable Form Components

### Create vs Edit Mode

One component handles both modes based on props:

```tsx
interface TaskFormProps {
  task?: Task         // If provided, we're in edit mode
  onSuccess?: () => void
  onCancel?: () => void
}

function TaskForm({ task, onSuccess, onCancel }: TaskFormProps) {
  const isEditing = !!task

  // Initialize with existing data (edit) or defaults (create)
  const [title, setTitle] = useState(task?.title ?? '')
  const [status, setStatus] = useState(task?.status ?? 'TODO')

  // Use different mutations for create vs update
  const mutation = isEditing ? updateMutation : createMutation

  return (
    <form onSubmit={handleSubmit}>
      {/* ... */}
      <button type="submit">
        {isEditing ? 'Update Task' : 'Create Task'}
      </button>
    </form>
  )
}
```

### Callback Props

Forms take callbacks to communicate with parents:

```tsx
// Parent component
const [showForm, setShowForm] = useState(false)

{showForm && (
  <TaskForm
    onSuccess={() => setShowForm(false)}  // Close form on success
    onCancel={() => setShowForm(false)}   // Close form on cancel
  />
)}
```

---

## Implementation Reference

Files implementing these patterns:

| File | Description |
|------|-------------|
| `src/components/TaskForm.tsx` | Create/edit task form |
| `src/components/ProjectForm.tsx` | Create/edit project form |
| `src/routes/tasks.tsx` | Tasks list with create form |
| `src/routes/tasks/$taskId.tsx` | Task detail with edit form |
| `src/routes/projects.tsx` | Projects list with create form |
| `src/routes/projects/$projectId.tsx` | Project detail with edit form |

---

## Key Takeaways

1. **Use controlled components** - React state is the source of truth
2. **Separate useState** for simple forms, object state for complex ones
3. **Always preventDefault** on form submission
4. **Define status options** as constant arrays with value/label pairs
5. **One component for create/edit** - use props to determine mode
6. **Date inputs need formatting** - append time for LocalDateTime

---

## Next: Mutations

Using useMutation to submit form data to the API.
