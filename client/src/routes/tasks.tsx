/**
 * Tasks List Route - /tasks
 *
 * This file demonstrates TanStack Query integration:
 * - useQuery hook for data fetching
 * - Loading and error states
 * - Query keys for caching
 *
 * TanStack Query handles:
 * - Automatic caching and background refetching
 * - Deduplication of requests
 * - Loading/error state management
 * - Retry on failure
 */

import { createFileRoute, Link } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchTasks, taskKeys } from '../api/tasks'
import type { Task, TaskStatus } from '../types/api'

export const Route = createFileRoute('/tasks')({
  component: TasksPage,
})

/**
 * TasksPage Component
 *
 * Displays a list of tasks fetched from the backend API.
 */
function TasksPage() {
  /**
   * useQuery - The core hook for fetching data
   *
   * Takes a configuration object with:
   * - queryKey: Unique identifier for caching (array format)
   * - queryFn: Function that returns a Promise with the data
   *
   * Returns an object with:
   * - data: The fetched data (undefined while loading)
   * - isPending: True during initial load
   * - isError: True if the query failed
   * - error: The error object if isError is true
   * - isSuccess: True if the query succeeded
   *
   * NOTE: We use taskKeys.list() instead of a raw array ['tasks'].
   * This factory pattern ensures consistent keys across the app.
   */
  const {
    data: tasks,
    isPending,
    isError,
    error,
  } = useQuery({
    queryKey: taskKeys.list(),
    queryFn: fetchTasks,
  })

  // Loading state - show skeleton or spinner
  if (isPending) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">Tasks</h1>
        <div className="space-y-3">
          {/* Skeleton loading cards */}
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 animate-pulse"
            >
              <div className="h-5 bg-gray-200 rounded w-1/3 mb-2"></div>
              <div className="h-4 bg-gray-100 rounded w-1/4"></div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  // Error state - show error message
  if (isError) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">Tasks</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 font-medium">Failed to load tasks</p>
          <p className="text-red-600 text-sm mt-1">
            {error instanceof Error ? error.message : 'Unknown error occurred'}
          </p>
        </div>
      </div>
    )
  }

  // Success state - render the task list
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-800 mb-4">Tasks</h1>

      {tasks.length === 0 ? (
        // Empty state
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
          <p className="text-gray-600">No tasks yet. Create your first task!</p>
        </div>
      ) : (
        // Task list
        <div className="space-y-3">
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} />
          ))}
        </div>
      )}
    </div>
  )
}

// =============================================================================
// TASK CARD COMPONENT
// =============================================================================

/**
 * TaskCard - Displays a single task in the list
 *
 * Extracted as a separate component for readability.
 * Links to the task detail page.
 */
function TaskCard({ task }: { task: Task }) {
  return (
    <Link
      to="/tasks/$taskId"
      params={{ taskId: String(task.id) }}
      className="block bg-white p-4 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="font-medium text-gray-800">{task.title}</p>
          {task.description && (
            <p className="text-sm text-gray-500 mt-1 line-clamp-2">
              {task.description}
            </p>
          )}
        </div>
        <StatusBadge status={task.status} />
      </div>

      {/* Due date if present */}
      {task.dueDate && (
        <p className="text-xs text-gray-400 mt-2">
          Due: {formatDate(task.dueDate)}
        </p>
      )}
    </Link>
  )
}

// =============================================================================
// STATUS BADGE COMPONENT
// =============================================================================

/**
 * StatusBadge - Displays the task status with appropriate styling
 *
 * Each status has a different color to make it easy to scan.
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

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Format an ISO date string for display
 *
 * @param isoString - ISO date string from the API
 * @returns Formatted date string
 */
function formatDate(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}
