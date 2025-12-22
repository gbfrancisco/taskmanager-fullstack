/**
 * Task Detail Route - /tasks/:taskId
 *
 * This file demonstrates:
 * - Using route parameters with TanStack Query
 * - Fetching a single resource by ID
 * - Combining useParams with useQuery
 *
 * IMPORTANT: Route params are always strings!
 * We need to parse them to numbers for API calls.
 */

import { createFileRoute, Link } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchTaskById, taskKeys } from '../../api/tasks'
import type { TaskStatus } from '../../types/api'

export const Route = createFileRoute('/tasks/$taskId')({
  component: TaskDetailPage,
})

function TaskDetailPage() {
  // Get the taskId from the URL
  const { taskId } = Route.useParams()

  /**
   * Parse the taskId to a number
   *
   * Route params are always strings, but our API expects numbers.
   * parseInt converts "123" → 123
   *
   * NOTE: We could add validation here to handle invalid IDs,
   * but for now we'll let the API return a 404.
   */
  const id = parseInt(taskId, 10)

  /**
   * Fetch the task data
   *
   * The query key includes the task ID, so each task has its own
   * cached entry. If you navigate away and back, the cached data
   * is shown immediately while a background refetch happens.
   */
  const { data: task, isPending, isError, error } = useQuery({
    queryKey: taskKeys.detail(id),
    queryFn: () => fetchTaskById(id),
    // Only run the query if we have a valid ID
    enabled: !isNaN(id),
  })

  // Invalid ID handling
  if (isNaN(id)) {
    return (
      <div className="p-6">
        <BackLink />
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mt-4">
          <p className="text-red-800 font-medium">Invalid task ID</p>
          <p className="text-red-600 text-sm mt-1">
            "{taskId}" is not a valid task ID
          </p>
        </div>
      </div>
    )
  }

  // Loading state
  if (isPending) {
    return (
      <div className="p-6">
        <BackLink />
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 mt-4 animate-pulse">
          <div className="h-7 bg-gray-200 rounded w-1/2 mb-4"></div>
          <div className="h-4 bg-gray-100 rounded w-3/4 mb-2"></div>
          <div className="h-4 bg-gray-100 rounded w-1/2"></div>
        </div>
      </div>
    )
  }

  // Error state
  if (isError) {
    return (
      <div className="p-6">
        <BackLink />
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mt-4">
          <p className="text-red-800 font-medium">Failed to load task</p>
          <p className="text-red-600 text-sm mt-1">
            {error instanceof Error ? error.message : 'Unknown error occurred'}
          </p>
        </div>
      </div>
    )
  }

  // Success state - render task details
  return (
    <div className="p-6">
      <BackLink />

      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 mt-4">
        {/* Header with title and status */}
        <div className="flex items-start justify-between mb-4">
          <h1 className="text-2xl font-bold text-gray-800">{task.title}</h1>
          <StatusBadge status={task.status} />
        </div>

        {/* Description */}
        {task.description ? (
          <p className="text-gray-600 mb-6">{task.description}</p>
        ) : (
          <p className="text-gray-400 italic mb-6">No description</p>
        )}

        {/* Metadata */}
        <div className="border-t border-gray-100 pt-4 space-y-2">
          <MetadataRow label="Task ID" value={String(task.id)} />
          <MetadataRow label="User ID" value={String(task.appUserId)} />
          {task.projectId && (
            <MetadataRow label="Project ID" value={String(task.projectId)} />
          )}
          {task.dueDate && (
            <MetadataRow label="Due Date" value={formatDate(task.dueDate)} />
          )}
        </div>
      </div>
    </div>
  )
}

// =============================================================================
// HELPER COMPONENTS
// =============================================================================

/**
 * BackLink - Navigate back to the tasks list
 */
function BackLink() {
  return (
    <Link
      to="/tasks"
      className="text-blue-600 hover:text-blue-800 text-sm flex items-center gap-1"
    >
      <span>←</span>
      <span>Back to tasks</span>
    </Link>
  )
}

/**
 * StatusBadge - Same component as in tasks.tsx
 *
 * In a real app, this would be in a shared components folder.
 * We duplicate it here for learning clarity.
 */
function StatusBadge({ status }: { status: TaskStatus }) {
  const styles: Record<TaskStatus, string> = {
    TODO: 'bg-gray-100 text-gray-800',
    IN_PROGRESS: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  }

  const labels: Record<TaskStatus, string> = {
    TODO: 'To Do',
    IN_PROGRESS: 'In Progress',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
  }

  return (
    <span className={`px-2 py-1 text-xs rounded ${styles[status]}`}>
      {labels[status]}
    </span>
  )
}

/**
 * MetadataRow - Displays a label/value pair
 */
function MetadataRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center text-sm">
      <span className="text-gray-500 w-24">{label}:</span>
      <span className="text-gray-800 font-mono">{value}</span>
    </div>
  )
}

/**
 * Format an ISO date string for display
 */
function formatDate(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}
