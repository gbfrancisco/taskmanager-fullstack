/**
 * TaskForm Component - Create and edit tasks
 *
 * This component demonstrates the useMutation hook from TanStack Query.
 *
 * KEY CONCEPTS:
 * - useMutation for POST/PUT/DELETE operations
 * - Cache invalidation after successful mutations
 * - Controlled form inputs in React
 * - Handling loading and error states during submission
 *
 * MUTATION FLOW:
 * 1. User fills out form and submits
 * 2. mutation.mutate() is called with form data
 * 3. TanStack Query calls our API function (createTask or updateTask)
 * 4. On success: invalidate related queries to refetch fresh data
 * 5. On error: error is available via mutation.error
 */

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createTask,
  updateTask,
  assignTaskToProject,
  removeTaskFromProject,
  taskKeys,
} from '../api/tasks'
import { fetchProjectsByUserId, projectKeys } from '../api/projects'
import type { Task, TaskCreateInput, TaskStatus } from '../types/api'

// =============================================================================
// COMPONENT PROPS
// =============================================================================

interface TaskFormProps {
  /**
   * Existing task data for edit mode.
   * If undefined, the form is in "create" mode.
   */
  task?: Task

  /**
   * Callback when form is successfully submitted.
   * Use this to close modals, navigate away, etc.
   */
  onSuccess?: () => void

  /**
   * Callback when user cancels the form.
   */
  onCancel?: () => void
}

// =============================================================================
// CONSTANTS
// =============================================================================

/**
 * Available task statuses for the dropdown.
 * Matches the TaskStatus type from our API types.
 */
const TASK_STATUSES: { value: TaskStatus; label: string }[] = [
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

/**
 * Temporary hardcoded user ID.
 * In a real app, this would come from authentication context.
 * TODO: Replace with actual authenticated user when auth is implemented.
 */
const TEMP_USER_ID = 1

// =============================================================================
// COMPONENT
// =============================================================================

export function TaskForm({ task, onSuccess, onCancel }: TaskFormProps) {
  // Determine if we're creating or editing
  const isEditing = !!task

  // Track original projectId to detect changes in edit mode
  const originalProjectId = task?.projectId ?? null

  // ---------------------------------------------------------------------------
  // FORM STATE
  // ---------------------------------------------------------------------------

  /**
   * Controlled form state.
   *
   * We initialize with existing task data (for edit) or defaults (for create).
   * Each input updates this state via onChange handlers.
   */
  const [title, setTitle] = useState(task?.title ?? '')
  const [description, setDescription] = useState(task?.description ?? '')
  const [status, setStatus] = useState<TaskStatus>(task?.status ?? 'TODO')
  const [projectId, setProjectId] = useState<number | null>(task?.projectId ?? null)

  /**
   * Due date/time state.
   *
   * We split date and time into separate inputs for better UX.
   * The toggle controls whether to include a specific time.
   * When off, we default to 00:00:00.
   */
  const [dueDate, setDueDate] = useState(() => {
    if (!task?.dueDate) return ''
    return task.dueDate.split('T')[0]
  })
  const [dueTime, setDueTime] = useState(() => {
    if (!task?.dueDate) return ''
    const timePart = task.dueDate.split('T')[1]
    // Extract HH:mm from HH:mm:ss
    return timePart ? timePart.slice(0, 5) : ''
  })
  const [includeTime, setIncludeTime] = useState(() => {
    if (!task?.dueDate) return false
    const timePart = task.dueDate.split('T')[1]
    // Check if time is not midnight (00:00:00)
    return timePart ? timePart !== '00:00:00' : false
  })

  // ---------------------------------------------------------------------------
  // FETCH PROJECTS FOR DROPDOWN
  // ---------------------------------------------------------------------------

  /**
   * Fetch projects belonging to the current user for the dropdown.
   *
   * We filter by userId to only show projects the user owns.
   * This is more scalable and secure than fetching all projects.
   */
  const { data: projects } = useQuery({
    queryKey: projectKeys.listByUser(TEMP_USER_ID),
    queryFn: () => fetchProjectsByUserId(TEMP_USER_ID),
  })

  // ---------------------------------------------------------------------------
  // QUERY CLIENT
  // ---------------------------------------------------------------------------

  /**
   * useQueryClient gives us access to the query cache.
   *
   * We need this to invalidate queries after a mutation succeeds.
   * Invalidation marks cached data as stale and triggers a refetch.
   */
  const queryClient = useQueryClient()

  // ---------------------------------------------------------------------------
  // CREATE MUTATION
  // ---------------------------------------------------------------------------

  /**
   * useMutation - The hook for create/update/delete operations
   *
   * Unlike useQuery (which runs automatically), mutations run when you call
   * mutation.mutate(data). This is perfect for form submissions.
   *
   * Key properties:
   * - mutationFn: The async function to call (our API function)
   * - onSuccess: Callback when mutation succeeds
   * - onError: Callback when mutation fails (optional)
   *
   * Returns an object with:
   * - mutate: Function to trigger the mutation
   * - isPending: True while mutation is in progress
   * - isError: True if mutation failed
   * - error: The error object if failed
   */
  const createMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => {
      // Invalidate the tasks list so it refetches with the new task
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
      onSuccess?.()
    },
  })

  // ---------------------------------------------------------------------------
  // UPDATE MUTATION
  // ---------------------------------------------------------------------------

  /**
   * Update mutation - similar pattern, different API function.
   *
   * Note: We don't call onSuccess here - handleSubmit coordinates
   * both the update and project assignment before calling onSuccess.
   */
  const updateMutation = useMutation({
    mutationFn: (input: { id: number; data: Parameters<typeof updateTask>[1] }) =>
      updateTask(input.id, input.data),
  })

  // ---------------------------------------------------------------------------
  // PROJECT ASSIGNMENT MUTATION
  // ---------------------------------------------------------------------------

  /**
   * Project assignment uses separate endpoints from updateTask.
   *
   * The backend has dedicated endpoints for project assignment:
   * - PUT /api/tasks/{taskId}/project/{projectId} - assign to project
   * - DELETE /api/tasks/{taskId}/project - remove from project
   *
   * We only call this when the project actually changes.
   */
  const projectAssignmentMutation = useMutation({
    mutationFn: async ({
      taskId,
      newProjectId,
    }: {
      taskId: number
      newProjectId: number | null
    }) => {
      if (newProjectId === null) {
        return removeTaskFromProject(taskId)
      } else {
        return assignTaskToProject(taskId, newProjectId)
      }
    },
  })

  // Combine for easier access in the UI
  const mutation = isEditing ? updateMutation : createMutation
  const isPending =
    mutation.isPending || projectAssignmentMutation.isPending

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  /**
   * Handle form submission.
   *
   * For edit mode, we need to coordinate two potential mutations:
   * 1. updateTask - updates title, description, status, dueDate
   * 2. assignTaskToProject/removeTaskFromProject - if project changed
   *
   * We use mutateAsync to await completion before calling onSuccess.
   */
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    /**
     * Build the due date/time string for the backend.
     *
     * Backend expects LocalDateTime format: "2026-01-01T14:30:00"
     * - If no date is set, dueDate is undefined
     * - If date is set but no time toggle, default to 00:00:00
     * - If date and time are both set, use the specified time
     */
    let formattedDueDate: string | undefined
    if (dueDate) {
      const timeValue = includeTime && dueTime ? dueTime : '00:00'
      formattedDueDate = `${dueDate}T${timeValue}:00`
    }

    if (isEditing && task) {
      try {
        // Update task fields
        await updateMutation.mutateAsync({
          id: task.id,
          data: {
            title,
            description: description || undefined,
            status,
            dueDate: formattedDueDate,
          },
        })

        // If project changed, call the separate project assignment endpoint
        const projectChanged = projectId !== originalProjectId
        if (projectChanged) {
          await projectAssignmentMutation.mutateAsync({
            taskId: task.id,
            newProjectId: projectId,
          })
        }

        // Invalidate queries after all mutations succeed
        queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
        queryClient.invalidateQueries({ queryKey: taskKeys.detail(task.id) })

        onSuccess?.()
      } catch {
        // Errors are handled by mutation.isError - no need to do anything here
      }
    } else {
      // Create new task - projectId is included in the create payload
      const input: TaskCreateInput = {
        title,
        description: description || undefined,
        status,
        dueDate: formattedDueDate,
        appUserId: TEMP_USER_ID,
        projectId: projectId ?? undefined,
      }
      createMutation.mutate(input)
    }
  }

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Title Field - Required */}
      <div>
        <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
          Title <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="Enter task title"
        />
      </div>

      {/* Description Field - Optional */}
      <div>
        <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
          Description
        </label>
        <textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="Enter task description (optional)"
        />
      </div>

      {/* Status Dropdown */}
      <div>
        <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
          Status
        </label>
        <select
          id="status"
          value={status}
          onChange={(e) => setStatus(e.target.value as TaskStatus)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          {TASK_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {/* Project Dropdown - Optional */}
      <div>
        <label htmlFor="projectId" className="block text-sm font-medium text-gray-700 mb-1">
          Project
        </label>
        <select
          id="projectId"
          value={projectId ?? ''}
          onChange={(e) => setProjectId(e.target.value ? parseInt(e.target.value, 10) : null)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="">No project</option>
          {projects?.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <p className="text-xs text-gray-500 mt-1">
          {isEditing ? 'Move task to a different project' : 'Assign this task to a project (optional)'}
        </p>
      </div>

      {/* Due Date & Time - Optional */}
      <div>
        <label htmlFor="dueDate" className="block text-sm font-medium text-gray-700 mb-1">
          Due Date
        </label>
        <div className="flex gap-2">
          <input
            type="date"
            id="dueDate"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
          {dueDate && (
            <input
              type="time"
              id="dueTime"
              value={dueTime}
              onChange={(e) => {
                setDueTime(e.target.value)
                if (e.target.value) setIncludeTime(true)
              }}
              disabled={!includeTime}
              className="w-32 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:text-gray-400"
            />
          )}
        </div>
        {dueDate && (
          <label className="flex items-center gap-2 mt-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={includeTime}
              onChange={(e) => {
                setIncludeTime(e.target.checked)
                if (!e.target.checked) setDueTime('')
              }}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            Include specific time
            {!includeTime && <span className="text-gray-400">(defaults to 00:00)</span>}
          </label>
        )}
      </div>

      {/* Error Message - show errors from either mutation */}
      {(mutation.isError || projectAssignmentMutation.isError) && (
        <div className="bg-red-50 border border-red-200 rounded-md p-3">
          <p className="text-red-800 text-sm">
            {mutation.error instanceof Error
              ? mutation.error.message
              : projectAssignmentMutation.error instanceof Error
                ? projectAssignmentMutation.error.message
                : 'An error occurred. Please try again.'}
          </p>
        </div>
      )}

      {/* Form Actions */}
      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          disabled={isPending}
          className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? 'Saving...' : isEditing ? 'Update Task' : 'Create Task'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
