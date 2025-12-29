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
import { createFileRoute, Link, redirect, useNavigate } from '@tanstack/react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchTaskById, deleteTask, taskKeys } from '@/api/tasks';
import { TaskForm } from '@/components/TaskForm';
import { RouteErrorComponent } from '@/components/RouteErrorComponent';
import { formatDate } from '@/utils/dateUtils';
import type { TaskStatus } from '@/types/api';

export const Route = createFileRoute('/tasks/$taskId')({
  // Route guard: redirect to login if not authenticated
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname }
      });
    }
  },
  component: TaskDetailPage,
  errorComponent: RouteErrorComponent
});

// =============================================================================
// TASK DETAIL PAGE COMPONENT
// =============================================================================

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
  const { data: task, isPending, isError, error } = useQuery({
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

  // ---------------------------------------------------------------------------
  // CONDITIONAL RENDERS
  // ---------------------------------------------------------------------------

  // Invalid ID handling
  if (isNaN(id)) {
    return <ErrorDisplay title="Invalid ID" message="Target not found." />;
  }

  // Loading state
  if (isPending) {
    return <LoadingDisplay />;
  }

  // Error state
  if (isError) {
    return (
      <ErrorDisplay
        title="Intel Unavailable"
        message={error instanceof Error ? error.message : 'Unknown error'}
      />
    );
  }

  // ---------------------------------------------------------------------------
  // RENDER - Success state
  // ---------------------------------------------------------------------------

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8">
      {/* Breadcrumb / Back link */}
      <div className="mb-6">
        <Link to="/tasks" className="font-bold text-sm uppercase tracking-wider hover:underline decoration-2">
          &larr; Return to Board
        </Link>
      </div>

      {/* Main Dossier Container */}
      <div className="bg-paper border-comic-heavy shadow-comic-lg relative">

        {/* Confidential stamp */}
        <div className="absolute -top-3 -right-3 bg-amber-vivid border-comic px-4 py-1 rotate-2 shadow-sm z-10">
          <span className="text-xs font-black uppercase tracking-widest">CONFIDENTIAL</span>
        </div>

        <div className="p-8">
          {isEditing ? (
            // Edit mode - show the form
            <div>
              <h2 className="text-display text-2xl mb-6 flex items-center gap-2">
                <span className="text-amber-vivid">✎</span> Edit Intel
              </h2>
              <TaskForm
                task={task}
                onSuccess={() => setIsEditing(false)}
                onCancel={() => setIsEditing(false)}
              />
            </div>
          ) : (
            // View mode - show task details
            <>
              {/* Header with title and status */}
              <div className="flex flex-col md:flex-row justify-between items-start gap-4 border-b-4 border-ink pb-6 mb-6">
                <div>
                  <div className="font-mono text-xs text-ink-light mb-1">CASE FILE #{task.id}</div>
                  <h1 className="text-display text-4xl md:text-5xl text-ink leading-tight">
                    {task.title}
                  </h1>
                </div>
                <StatusStampLarge status={task.status} />
              </div>

              <div className="grid md:grid-cols-3 gap-8">
                {/* Left Column: Description and Actions */}
                <div className="md:col-span-2 space-y-6">
                  {/* Description */}
                  <div>
                    <h3 className="font-black uppercase text-sm mb-2 bg-ink text-paper inline-block px-2">
                      Description
                    </h3>
                    <div className="text-lg leading-relaxed border-l-2 border-ink-light pl-4">
                      {task.description || (
                        <span className="italic text-ink-light">No description provided.</span>
                      )}
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex flex-wrap gap-4 pt-4">
                    <button
                      onClick={() => setIsEditing(true)}
                      className="bg-amber-light hover:bg-amber-vivid text-ink border-comic px-6 py-2 font-bold uppercase shadow-[4px_4px_0_black] hover:shadow-[2px_2px_0_black] hover:translate-x-[2px] hover:translate-y-[2px] transition-all"
                    >
                      Update Intel
                    </button>
                    <button
                      onClick={() => setShowDeleteConfirm(true)}
                      className="bg-paper hover:bg-danger-bg text-danger border-comic px-6 py-2 font-bold uppercase shadow-[4px_4px_0_black] hover:shadow-[2px_2px_0_black] hover:translate-x-[2px] hover:translate-y-[2px] transition-all"
                    >
                      Burn File
                    </button>
                  </div>

                  {/* Delete Confirmation Dialog */}
                  {showDeleteConfirm && (
                    <div className="mt-4 p-4 bg-danger-bg border-comic-heavy">
                      <p className="font-bold text-danger mb-2">⚠ WARNING: IRREVERSIBLE ACTION</p>
                      <p className="text-sm mb-4">Are you sure you want to delete this task?</p>
                      <div className="flex gap-3">
                        <button
                          onClick={() => deleteMutation.mutate(id)}
                          disabled={deleteMutation.isPending}
                          className="bg-danger text-paper border-2 border-ink px-4 py-1 font-bold shadow-comic-sm hover:translate-y-0.5 hover:shadow-none disabled:opacity-50"
                        >
                          {deleteMutation.isPending ? 'Deleting...' : 'CONFIRM DELETION'}
                        </button>
                        <button
                          onClick={() => setShowDeleteConfirm(false)}
                          disabled={deleteMutation.isPending}
                          className="bg-paper text-ink border-2 border-ink px-4 py-1 font-bold shadow-comic-sm hover:translate-y-0.5 hover:shadow-none"
                        >
                          CANCEL
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
                </div>

                {/* Right Column: Metadata Sidebar */}
                <div className="bg-halftone p-4 border-comic h-fit">
                  <h3 className="font-display text-xl border-b-2 border-ink mb-4">Meta Data</h3>
                  <dl className="space-y-4 font-mono text-sm">
                    <MetaItem label="Operative" value={task.appUser.username} />
                    <MetaItem
                      label="Project"
                      value={task.project ? task.project.name : 'N/A'}
                      link={task.project ? `/projects/${task.project.id}` : undefined}
                    />
                    <MetaItem
                      label="Deadline"
                      value={task.dueDate ? formatDate(task.dueDate) : 'Indefinite'}
                      highlight={!!task.dueDate}
                    />
                    <MetaItem label="Created" value={formatDate(task.createdTimestamp)} />
                  </dl>
                </div>
              </div>
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
 * MetaItem - Displays a label/value pair in the metadata sidebar
 */
interface MetaItemProps {
  label: string;
  value: string;
  link?: string;
  highlight?: boolean;
}

function MetaItem({ label, value, link, highlight }: MetaItemProps) {
  return (
    <div>
      <dt className="text-xs text-ink-light uppercase font-bold">{label}</dt>
      <dd className={`font-bold ${highlight ? 'text-amber-dark' : 'text-ink'}`}>
        {link ? (
          <Link to={link} className="underline decoration-wavy hover:text-amber-vivid">
            {value}
          </Link>
        ) : (
          value
        )}
      </dd>
    </div>
  );
}

/**
 * StatusStampLarge - Large rotated status badge
 *
 * Uses a double border for a "stamped" effect.
 */
function StatusStampLarge({ status }: { status: TaskStatus }) {
  const colors: Record<TaskStatus, string> = {
    TODO: 'text-ink-light border-ink-light',
    IN_PROGRESS: 'text-status-progress border-status-progress',
    COMPLETED: 'text-success border-success',
    CANCELLED: 'text-danger border-danger'
  };

  return (
    <div className={`border-4 border-double p-2 rotate-[-5deg] opacity-90 ${colors[status]}`}>
      <span className="text-xl font-black uppercase tracking-widest">
        {status.replace('_', ' ')}
      </span>
    </div>
  );
}

/**
 * LoadingDisplay - Skeleton placeholder while loading
 */
function LoadingDisplay() {
  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="bg-paper border-comic p-12 animate-pulse flex flex-col items-center">
        <div className="w-16 h-16 bg-paper-dark rounded-full mb-4" />
        <div className="h-4 w-1/2 bg-paper-dark mb-2" />
        <div className="h-4 w-1/3 bg-paper-dark" />
      </div>
    </div>
  );
}

/**
 * ErrorDisplay - Shows error message with link back to safety
 */
function ErrorDisplay({ title, message }: { title: string; message: string }) {
  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="bg-danger-bg border-comic p-6">
        <h2 className="text-display text-2xl text-danger">{title}</h2>
        <p>{message}</p>
        <Link to="/tasks" className="underline mt-4 inline-block">
          Return to safety
        </Link>
      </div>
    </div>
  );
}
