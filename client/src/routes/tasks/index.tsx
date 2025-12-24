/**
 * Tasks List Route - /tasks
 *
 * This file demonstrates TanStack Query integration:
 * - useQuery hook for data fetching
 * - useMutation for creating tasks (via TaskForm component)
 * - Loading and error states
 * - Query keys for caching
 *
 * TanStack Query handles:
 * - Automatic caching and background refetching
 * - Deduplication of requests
 * - Loading/error state management
 * - Retry on failure
 * - Cache invalidation after mutations
 */

import { useState } from 'react';
import { createFileRoute, Link } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { fetchTasks, taskKeys } from '@/api/tasks';
import { TaskForm } from '@/components/TaskForm';
import type { Task, TaskStatus } from '@/types/api';

export const Route = createFileRoute('/tasks/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData({
      queryKey: taskKeys.list(),
      queryFn: fetchTasks
    }),
  component: TasksPage
});

/**
 * TasksPage Component
 *
 * Displays a list of tasks fetched from the backend API.
 * Includes a form to create new tasks.
 */
function TasksPage() {
  /**
   * State to toggle the create form visibility.
   *
   * When showCreateForm is true, we display the TaskForm component.
   * After successful creation, we hide the form.
   */
  const [showCreateForm, setShowCreateForm] = useState(false);

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
    error
  } = useQuery({
    queryKey: taskKeys.list(),
    queryFn: fetchTasks
  });

  // Loading state - show skeleton or spinner
  if (isPending) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <h1 className="text-display text-4xl text-ink mb-6">Tasks</h1>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {/* Skeleton loading cards */}
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="bg-paper p-4 border-comic shadow-comic-sm animate-pulse"
            >
              <div className="h-5 bg-paper-dark w-2/3 mb-2"></div>
              <div className="h-4 bg-paper-dark w-full mb-3"></div>
              <div className="h-4 bg-paper-dark w-1/2"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Error state - show error message
  if (isError) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <h1 className="text-display text-4xl text-ink mb-6">Tasks</h1>
        <div className="bg-danger-bg border-comic p-4">
          <p className="text-danger font-medium">Failed to load tasks</p>
          <p className="text-danger text-sm mt-1">
            {error instanceof Error ? error.message : 'Unknown error occurred'}
          </p>
        </div>
      </div>
    );
  }

  // Success state - render the task list
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Header with title and create button */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-display text-4xl text-ink">Tasks</h1>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="bg-amber-vivid text-ink px-6 py-3 border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
        >
          {showCreateForm ? 'Cancel' : '+ New Task'}
        </button>
      </div>
      {/* Create Task Form - shown when showCreateForm is true */}
      {showCreateForm ? (
        <div className="bg-paper border-comic shadow-comic-soft-lg p-6 max-w-3xl mx-auto">
          <h2 className="text-display text-xl text-ink mb-4">
            Create New Task
          </h2>
          <TaskForm
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      ) : tasks.length === 0 ? (
        // Empty state
        <div className="bg-paper-dark border-comic p-6 text-center">
          <p className="text-ink-soft">No tasks yet. Create your first task!</p>
        </div>
      ) : (
        // Task grid
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} />
          ))}
        </div>
      )}
    </div>
  );
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
      className="block bg-paper p-4 border-comic shadow-comic-soft-interactive"
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="font-medium text-ink">{task.title}</p>
          {task.description && (
            <p className="text-sm text-ink-soft mt-1 line-clamp-2">
              {task.description}
            </p>
          )}
        </div>
        <StatusBadge status={task.status} />
      </div>

      {/* Due date if present */}
      {task.dueDate && (
        <p className="text-xs text-ink-light mt-2">
          Due: {formatDate(task.dueDate)}
        </p>
      )}
    </Link>
  );
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
    TODO: 'bg-status-todo',
    IN_PROGRESS: 'bg-status-progress',
    COMPLETED: 'bg-status-complete',
    CANCELLED: 'bg-status-cancelled'
  };

  const labels: Record<TaskStatus, string> = {
    TODO: 'To Do',
    IN_PROGRESS: 'In Progress',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled'
  };

  return (
    <span
      className={`px-2 py-1 text-xs text-ink border-2 border-ink shadow-comic-sm ${styles[status]}`}
    >
      {labels[status]}
    </span>
  );
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Format an ISO date string for display
 *
 * Shows time only if it's not midnight (00:00).
 */
function formatDate(isoString: string): string {
  const date = new Date(isoString);

  const dateStr = date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });

  // Check if time is midnight (meaning no specific time was set)
  const hours = date.getHours();
  const minutes = date.getMinutes();
  if (hours === 0 && minutes === 0) {
    return dateStr;
  }

  // Include time if it was explicitly set
  const timeStr = date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit'
  });

  return `${dateStr}, ${timeStr}`;
}
