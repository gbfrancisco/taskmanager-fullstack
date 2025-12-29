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
import { createFileRoute, Link, redirect } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { fetchProjects, projectKeys } from '@/api/projects';
import { ProjectForm } from '@/components/ProjectForm';
import { RouteErrorComponent } from '@/components/RouteErrorComponent';
import type { Project, ProjectStatus } from '@/types/api';

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
  if (isPending) return <ProjectsLoadingSkeleton />;

  // Error state - show error message
  if (isError) return <ProjectsErrorState error={error} />;

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
            Campaign <span className="text-paper bg-ink px-2 skew-x-[-6deg] inline-block">Log</span>
          </h1>
        </div>

        {/* Create button with press animation */}
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className={`
            group relative px-6 py-3 border-comic text-display uppercase tracking-wider font-bold transition-all
            ${!showCreateForm
              // Default state: popped up with shadow, hover lifts higher
              ? 'bg-amber-vivid text-ink shadow-[4px_4px_0_black] hover:-translate-y-1 hover:shadow-[6px_6px_0_black] active:translate-y-0 active:shadow-[2px_2px_0_black]'
              // Active (cancel) state: pressed down, no shadow, dashed border
              : 'bg-paper text-ink shadow-none translate-y-1 border-dashed'}
          `}
        >
          {showCreateForm ? 'Close Log' : '+ Start Campaign'}
        </button>
      </div>

      {/* Create Project Form - shown when showCreateForm is true */}
      {showCreateForm && (
        <div className="mb-10 bg-paper border-comic-heavy shadow-comic-lg p-6 relative overflow-hidden">
          <h2 className="text-display text-2xl text-ink mb-6 border-b-4 border-amber-vivid inline-block">
            Initialize Campaign
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
          <div className="w-20 h-24 border-4 border-ink bg-paper mb-4 shadow-comic rotate-3 flex items-center justify-center">
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

// =============================================================================
// PROJECT CARD COMPONENT
// =============================================================================

/**
 * ProjectCard - Displays a single project in the list
 *
 * Features a visual "spine" on the left side that changes color based on status.
 * Links to the project detail page.
 */
function ProjectCard({ project }: { project: Project }) {
  // Determine "Spine" color based on status
  const statusColors: Record<string, string> = {
    PLANNING: 'bg-status-planning',
    ACTIVE: 'bg-status-active',
    ON_HOLD: 'bg-status-on-hold',
    COMPLETED: 'bg-status-completed'
  };

  const spineColor = statusColors[project.status] || 'bg-ink';

  return (
    <Link
      to="/projects/$projectId"
      params={{ projectId: String(project.id) }}
      className="group block bg-paper border-comic shadow-[4px_4px_0_black] hover:-translate-y-1 hover:shadow-[6px_6px_0_black] transition-all h-full relative overflow-hidden"
    >
      {/* Visual Spine (Left side strip) */}
      <div className={`absolute left-0 top-0 bottom-0 w-3 border-r-2 border-ink ${spineColor}`} />

      <div className="p-5 pl-8 flex flex-col h-full">
        {/* ID and Status row */}
        <div className="flex justify-between items-start mb-2">
          <span className="font-mono text-[10px] font-bold uppercase text-ink-light tracking-widest">
            Arc #{project.id.toString().padStart(3, '0')}
          </span>
          <ProjectStatusStamp status={project.status} />
        </div>

        {/* Title */}
        <h3 className="text-display text-2xl leading-tight mb-2 group-hover:text-amber-dark transition-colors">
          {project.name}
        </h3>

        {/* Description (truncated) */}
        <div className="flex-1 mb-6">
          {project.description ? (
            <p className="text-sm text-ink-soft line-clamp-3">
              {project.description}
            </p>
          ) : (
            <p className="text-sm text-ink-light italic">No synopsis available.</p>
          )}
        </div>

        {/* Footer - Task count */}
        <div className="border-t-2 border-dashed border-ink/20 pt-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase text-ink-light">Objectives:</span>
            <span className="bg-ink text-paper text-xs font-mono px-1.5 py-0.5 rounded-sm">
              {project.taskCount || 0}
            </span>
          </div>

          <span className="text-xs font-bold text-amber-vivid group-hover:translate-x-1 transition-transform">
            Open File &rarr;
          </span>
        </div>
      </div>
    </Link>
  );
}

// =============================================================================
// STATUS STAMP COMPONENT
// =============================================================================

/**
 * ProjectStatusStamp - Displays the project status as a rotated stamp/badge
 *
 * Each status has a different color and slight rotation for a "stamped" effect.
 */
function ProjectStatusStamp({ status }: { status: ProjectStatus }) {
  const labels: Record<ProjectStatus, string> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On Hold',
    COMPLETED: 'Done',
    CANCELLED: 'Cancelled'
  };

  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'text-ink-light border-ink-light',
    ACTIVE: 'text-success border-success',
    ON_HOLD: 'text-status-on-hold border-status-on-hold',
    COMPLETED: 'text-status-progress border-status-progress',
    CANCELLED: 'text-danger border-danger'
  };

  return (
    <span className={`
      text-[10px] font-black uppercase tracking-wider border-2 px-1 rotate-[-2deg]
      ${styles[status] || 'text-ink border-ink'}
    `}>
      {labels[status] || status}
    </span>
  );
}

// =============================================================================
// LOADING SKELETON
// =============================================================================

/**
 * ProjectsLoadingSkeleton - Placeholder UI while data is loading
 *
 * Shows animated pulse effect to indicate loading state.
 */
function ProjectsLoadingSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="h-16 w-64 bg-paper-dark mb-8 animate-pulse" />
      <div className="grid gap-6 md:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-48 bg-paper border-comic opacity-50 animate-pulse" />
        ))}
      </div>
    </div>
  );
}

// =============================================================================
// ERROR STATE
// =============================================================================

/**
 * ProjectsErrorState - Displayed when project fetching fails
 *
 * Shows error message in a danger-styled container.
 */
function ProjectsErrorState({ error }: { error: unknown }) {
  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="bg-danger-bg border-comic-heavy p-6 shadow-comic">
        <h1 className="text-display text-2xl text-danger mb-2">CONNECTION SEVERED</h1>
        <p className="font-mono text-sm">
          {error instanceof Error ? error.message : 'Unable to retrieve campaign logs.'}
        </p>
      </div>
    </div>
  );
}
