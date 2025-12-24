/**
 * Task Detail Route - /tasks/:taskId
 *
 * This file demonstrates:
 * - Using route parameters with TanStack Query
 * - Fetching a single resource by ID
 * - Combining useParams with useQuery
 * - useMutation for delete operations
 * - Edit mode with inline form
 *
 * IMPORTANT: Route params are always strings!
 * We need to parse them to numbers for API calls.
 */

import { useState } from 'react';
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchTaskById, deleteTask, taskKeys } from '../../api/tasks';
import { TaskForm } from '../../components/TaskForm';
import { MetadataList, MetadataItem } from '../../components/Metadata';
import type { TaskStatus } from '../../types/api';

export const Route = createFileRoute('/tasks/$taskId')({
  component: TaskDetailPage
});

function TaskDetailPage() {
  // Get the taskId from the URL
  const { taskId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // State for edit mode and delete confirmation
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  /**
   * Parse the taskId to a number
   *
   * Route params are always strings, but our API expects numbers.
   * parseInt converts "123" → 123
   *
   * NOTE: We could add validation here to handle invalid IDs,
   * but for now we'll let the API return a 404.
   */
  const id = parseInt(taskId, 10);

  /**
   * Fetch the task data
   *
   * The query key includes the task ID, so each task has its own
   * cached entry. If you navigate away and back, the cached data
   * is shown immediately while a background refetch happens.
   */
  const {
    data: task,
    isPending,
    isError,
    error
  } = useQuery({
    queryKey: taskKeys.detail(id),
    queryFn: () => fetchTaskById(id),
    // Only run the query if we have a valid ID
    enabled: !isNaN(id)
  });

  /**
   * Delete mutation
   *
   * After successfully deleting, we:
   * 1. Invalidate the tasks list (so it refetches without this task)
   * 2. Navigate back to the tasks list
   */
  const deleteMutation = useMutation({
    mutationFn: deleteTask,
    onSuccess: () => {
      // Invalidate the tasks list
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() });
      // Navigate back to the list
      navigate({ to: '/tasks' });
    }
  });

  /**
   * Handle delete with confirmation
   */
  function handleDelete() {
    deleteMutation.mutate(id);
  }

  // Invalid ID handling
  if (isNaN(id)) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="max-w-3xl mx-auto">
          <BackLink />
        <div className="bg-danger-bg border-comic p-4 mt-4">
          <p className="text-danger font-medium">Invalid task ID</p>
          <p className="text-danger text-sm mt-1">
            "{taskId}" is not a valid task ID
          </p>
        </div>
        </div>
      </div>
    );
  }

  // Loading state
  if (isPending) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="max-w-3xl mx-auto">
          <BackLink />
          <div className="bg-paper p-6 border-comic shadow-comic-soft-lg mt-4 animate-pulse">
            <div className="h-7 bg-paper-dark w-1/2 mb-4"></div>
            <div className="h-4 bg-paper-dark w-3/4 mb-2"></div>
            <div className="h-4 bg-paper-dark w-1/2"></div>
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (isError) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="max-w-3xl mx-auto">
          <BackLink />
          <div className="bg-danger-bg border-comic p-4 mt-4">
            <p className="text-danger font-medium">Failed to load task</p>
            <p className="text-danger text-sm mt-1">
              {error instanceof Error ? error.message : 'Unknown error occurred'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Success state - render task details or edit form
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="max-w-3xl mx-auto">
        <BackLink />

        <div className="bg-paper p-6 border-comic shadow-comic-soft-lg mt-4">
        {isEditing ? (
          // Edit mode - show the form
          <>
            <h2 className="text-display text-xl text-ink mb-4">
              Edit Task
            </h2>
            <TaskForm
              task={task}
              onSuccess={() => setIsEditing(false)}
              onCancel={() => setIsEditing(false)}
            />
          </>
        ) : (
          // View mode - show task details
          <>
            {/* Header with title and status */}
            <div className="flex items-start justify-between mb-4">
              <h1 className="text-display text-3xl text-ink">{task.title}</h1>
              <StatusBadge status={task.status} />
            </div>

            {/* Description */}
            {task.description ? (
              <p className="text-ink-soft mb-6">{task.description}</p>
            ) : (
              <p className="text-ink-light italic mb-6">No description</p>
            )}

            {/* Metadata - using MetadataList component */}
            <MetadataList className="border-t-2 border-ink pt-4 gap-x-8">
              <MetadataItem label="Task ID">
                <span className="font-mono">{task.id}</span>
              </MetadataItem>

              <MetadataItem label="Owner">
                <span className="font-mono">{task.appUser.username}</span>
              </MetadataItem>

              {task.project && (
                <MetadataItem label="Project">
                  <Link
                    to="/projects/$projectId"
                    params={{ projectId: String(task.project.id) }}
                    className="text-amber-dark hover:text-amber-vivid font-medium"
                  >
                    {task.project.name}
                  </Link>
                </MetadataItem>
              )}

              {task.dueDate && (
                <MetadataItem label="Due Date">
                  <span className="font-mono">{formatDate(task.dueDate)}</span>
                </MetadataItem>
              )}
            </MetadataList>

            {/* Action Buttons */}
            <div className="border-t-2 border-ink pt-4 mt-4 flex gap-3">
              <button
                onClick={() => setIsEditing(true)}
                className="px-6 py-3 bg-amber-vivid text-ink border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
              >
                Edit Task
              </button>
              <button
                onClick={() => setShowDeleteConfirm(true)}
                className="px-6 py-3 bg-danger text-paper border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
              >
                Delete Task
              </button>
            </div>

            {/* Delete Confirmation Dialog */}
            {showDeleteConfirm && (
              <div className="mt-4 p-4 bg-danger-bg border-comic">
                <p className="text-ink font-medium">
                  Are you sure you want to delete this task?
                </p>
                <p className="text-danger text-sm mt-1">
                  This action cannot be undone.
                </p>
                <div className="mt-3 flex gap-3">
                  <button
                    onClick={handleDelete}
                    disabled={deleteMutation.isPending}
                    className="px-6 py-3 bg-danger text-paper border-comic shadow-comic text-display tracking-wide shadow-comic-interactive disabled:opacity-50"
                  >
                    {deleteMutation.isPending ? 'Deleting...' : 'Yes, Delete'}
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirm(false)}
                    disabled={deleteMutation.isPending}
                    className="px-6 py-3 bg-paper text-ink border-comic shadow-comic text-display tracking-wide shadow-comic-interactive"
                  >
                    Cancel
                  </button>
                </div>
                {deleteMutation.isError && (
                  <p className="text-danger text-sm mt-2">
                    {deleteMutation.error instanceof Error
                      ? deleteMutation.error.message
                      : 'Failed to delete task'}
                  </p>
                )}
              </div>
            )}
          </>
        )}
        </div>
      </div>
    </div>
  );
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
      className="text-amber-dark text-display hover:text-amber-vivid flex items-center gap-1"
    >
      <span>←</span>
      <span>Back to tasks</span>
    </Link>
  );
}

/**
 * StatusBadge - Same component as in tasks.tsx
 *
 * In a real app, this would be in a shared components folder.
 * We duplicate it here for learning clarity.
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

/**
 * Format an ISO date string for display
 *
 * Shows time only if it's not midnight (00:00).
 * This matches our form behavior where users can optionally include time.
 */
function formatDate(isoString: string): string {
  const date = new Date(isoString);

  const dateStr = date.toLocaleDateString('en-US', {
    weekday: 'short',
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

  return `${dateStr} at ${timeStr}`;
}
