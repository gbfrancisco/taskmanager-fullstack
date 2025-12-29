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
import { createFileRoute, redirect } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { fetchTasks, taskKeys } from '@/api/tasks';
import { TaskForm } from '@/components/TaskForm';
import { TaskCard } from '@/components/TaskCard';
import { CardGridSkeleton } from '@/components/CardGridSkeleton';
import { PageErrorState } from '@/components/PageErrorState';
import { RouteErrorComponent } from '@/components/RouteErrorComponent';

export const Route = createFileRoute('/tasks/')({
  // Route guard: redirect to login if not authenticated
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname }
      });
    }
  },
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData({
      queryKey: taskKeys.list(),
      queryFn: fetchTasks
    }),
  component: TasksPage,
  errorComponent: RouteErrorComponent
});

// =============================================================================
// TASKS PAGE COMPONENT
// =============================================================================

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

  // Loading state - show skeleton
  if (isPending) return <CardGridSkeleton />;

  // Error state - show error message
  if (isError) {
    return (
      <PageErrorState
        title="MISSION FAILURE"
        error={error}
        defaultMessage="Unknown tactical error."
      />
    );
  }

  // ---------------------------------------------------------------------------
  // RENDER - Success state
  // ---------------------------------------------------------------------------

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Header with title and create button */}
      <div className="flex flex-col md:flex-row items-start md:items-end justify-between mb-8 gap-4">
        <div>
          <div className="comic-caption text-xs mb-2">Sector 7 // Objectives</div>
          <h1 className="text-display text-5xl text-ink uppercase leading-none">
            Your <span className="text-amber-vivid bg-ink px-2 skew-x-[-6deg] inline-block">Tasks</span>
          </h1>
        </div>

        {/* Create button with press animation */}
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className={`
            group relative px-6 py-3 border-comic text-display uppercase tracking-wider font-bold
            ${!showCreateForm
              // Default state: interactive shadow (press down on hover)
              ? 'bg-amber-vivid text-ink shadow-comic-soft-interactive'
              // Active (cancel) state: pressed down, no shadow, dashed border
              : 'bg-paper text-ink shadow-none translate-y-1 border-dashed transition-all'}
          `}
        >
          {showCreateForm ? 'Cancel' : '+ New Task'}
        </button>
      </div>

      {/* Create Task Form - shown when showCreateForm is true */}
      {showCreateForm && (
        <div className="mb-10 bg-paper border-comic-heavy shadow-comic-soft-lg p-6 relative overflow-hidden">
          {/* Decorative watermark */}
          <div className="absolute top-0 right-0 p-2 opacity-10 pointer-events-none">
            <span className="text-9xl font-black text-ink">NEW</span>
          </div>
          <h2 className="text-display text-2xl text-ink mb-6 border-b-4 border-amber-vivid inline-block">
            Mission Briefing
          </h2>
          <TaskForm
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Empty state - no tasks yet */}
      {!isPending && tasks.length === 0 && !showCreateForm && (
        <div className="bg-halftone border-comic p-12 text-center">
          <div className="inline-block border-4 border-ink rounded-full p-6 mb-4 bg-paper shadow-comic-soft">
            <span className="text-4xl">💤</span>
          </div>
          <h3 className="text-display text-2xl mb-2">All Quiet on the Front</h3>
          <p className="text-ink-soft">No active missions. Take a break or create a new objective.</p>
        </div>
      )}

      {/* Task grid */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {tasks.map((task) => (
          <TaskCard key={task.id} task={task} />
        ))}
      </div>
    </div>
  );
}
