/**
 * Projects List Route - /projects
 *
 * This file demonstrates TanStack Query integration:
 * - useQuery hook for data fetching
 * - useMutation for creating projects (via ProjectForm component)
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
import { fetchProjects, projectKeys } from '@/api/projects';
import { ProjectForm } from '@/components/ProjectForm';
import { ProjectCard } from '@/components/ProjectCard';
import { CardGridSkeleton } from '@/components/CardGridSkeleton';
import { PageErrorState } from '@/components/PageErrorState';
import { RouteErrorComponent } from '@/components/RouteErrorComponent';

export const Route = createFileRoute('/projects/')({
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
      queryKey: projectKeys.list(),
      queryFn: fetchProjects
    }),
  component: ProjectsPage,
  errorComponent: RouteErrorComponent
});

// =============================================================================
// PROJECTS PAGE COMPONENT
// =============================================================================

/**
 * ProjectsPage Component
 *
 * Displays a list of projects fetched from the backend API.
 * Includes a form to create new projects.
 */
function ProjectsPage() {
  /**
   * State to toggle the create form visibility.
   *
   * When showCreateForm is true, we display the ProjectForm component.
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
   *
   * NOTE: We use projectKeys.list() instead of a raw array ['projects'].
   * This factory pattern ensures consistent keys across the app.
   */
  const {
    data: projects,
    isPending,
    isError,
    error
  } = useQuery({
    queryKey: projectKeys.list(),
    queryFn: fetchProjects
  });

  // Loading state - show skeleton
  if (isPending) return <CardGridSkeleton />;

  // Error state - show error message
  if (isError) {
    return (
      <PageErrorState
        title="CONNECTION SEVERED"
        error={error}
        defaultMessage="Unable to retrieve campaign logs."
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
          <div className="comic-caption text-xs mb-2 bg-ink border-none">
            Vol. 1 // Global Operations
          </div>
          <h1 className="text-display text-5xl text-ink uppercase leading-none">
            Your <span className="text-amber-vivid bg-ink px-2 skew-x-[-6deg] inline-block">Projects</span>
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
          {showCreateForm ? 'Cancel' : '+ New Project'}
        </button>
      </div>

      {/* Create Project Form - shown when showCreateForm is true */}
      {showCreateForm && (
        <div className="mb-10 bg-paper border-comic-heavy shadow-comic-soft-lg p-6 relative overflow-hidden">
          <h2 className="text-display text-2xl text-ink mb-6 border-b-4 border-amber-vivid inline-block">
            Create Project
          </h2>
          <ProjectForm
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Empty state - no projects yet */}
      {!isPending && projects.length === 0 && !showCreateForm && (
        <div className="bg-halftone border-comic p-12 text-center flex flex-col items-center">
          <div className="w-20 h-24 border-4 border-ink bg-paper mb-4 shadow-comic-soft rotate-3 flex items-center justify-center">
            <span className="text-4xl text-ink-light">?</span>
          </div>
          <h3 className="text-display text-2xl mb-2">The Archives Are Empty</h3>
          <p className="text-ink-soft max-w-md">
            No active campaigns found in the database. Start a new story arc to begin tracking objectives.
          </p>
        </div>
      )}

      {/* Project grid */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {projects.map((project) => (
          <ProjectCard key={project.id} project={project} />
        ))}
      </div>
    </div>
  );
}
