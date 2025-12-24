/**
 * Projects List Route - /projects
 *
 * Same pattern as tasks.tsx:
 * - useQuery for data fetching
 * - useMutation for creating projects (via ProjectForm)
 * - Loading, error, and success states
 * - Query key factory for consistent caching
 */

import { useState } from 'react';
import { createFileRoute, Link } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { fetchProjects, projectKeys } from '../../api/projects';
import { ProjectForm } from '../../components/ProjectForm';
import type { Project, ProjectStatus } from '../../types/api';

export const Route = createFileRoute('/projects/')({
  loader: ({ context: { queryClient } }) =>
    queryClient.ensureQueryData({
      queryKey: projectKeys.list(),
      queryFn: fetchProjects
    }),
  component: ProjectsPage
});

function ProjectsPage() {
  const [showCreateForm, setShowCreateForm] = useState(false);

  const {
    data: projects,
    isPending,
    isError,
    error
  } = useQuery({
    queryKey: projectKeys.list(),
    queryFn: fetchProjects
  });

  // Loading state
  if (isPending) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <h1 className="text-display text-4xl text-ink mb-6">Projects</h1>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
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

  // Error state
  if (isError) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <h1 className="text-display text-4xl text-ink mb-6">Projects</h1>
        <div className="bg-danger-bg border-comic p-4">
          <p className="text-danger font-medium">Failed to load projects</p>
          <p className="text-danger text-sm mt-1">
            {error instanceof Error ? error.message : 'Unknown error occurred'}
          </p>
        </div>
      </div>
    );
  }

  // Success state
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Header with title and create button */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-display text-4xl text-ink">Projects</h1>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="bg-amber-vivid text-ink px-6 py-3 border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
        >
          {showCreateForm ? 'Cancel' : '+ New Project'}
        </button>
      </div>

      {/* Create Project Form */}
      {showCreateForm ? (
        <div className="bg-paper border-comic shadow-comic-soft-lg p-6 max-w-3xl mx-auto">
          <h2 className="text-display text-xl text-ink mb-4">
            Create New Project
          </h2>
          <ProjectForm
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      ) : projects.length === 0 ? (
        <div className="bg-paper-dark border-comic p-6 text-center">
          <p className="text-ink-soft">
            No projects yet. Create your first project!
          </p>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      )}
    </div>
  );
}

// =============================================================================
// PROJECT CARD COMPONENT
// =============================================================================

function ProjectCard({ project }: { project: Project }) {
  return (
    <Link
      to="/projects/$projectId"
      params={{ projectId: String(project.id) }}
      className="bg-paper p-4 border-comic shadow-comic-soft-interactive"
    >
      <h3 className="text-display text-xl text-ink mb-2">{project.name}</h3>

      {project.description ? (
        <p className="text-sm text-ink-soft mb-3 line-clamp-2">
          {project.description}
        </p>
      ) : (
        <p className="text-sm text-ink-light italic mb-3">No description</p>
      )}

      <div className="flex justify-between items-center">
        <span className="text-xs text-ink-light">
          {project.taskCount !== undefined && project.taskCount > 0
            ? `${project.taskCount} task${project.taskCount !== 1 ? 's' : ''}`
            : 'No tasks'}
        </span>
        <StatusBadge status={project.status} />
      </div>
    </Link>
  );
}

// =============================================================================
// STATUS BADGE COMPONENT
// =============================================================================

function StatusBadge({ status }: { status: ProjectStatus }) {
  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'bg-status-planning',
    ACTIVE: 'bg-status-active',
    ON_HOLD: 'bg-status-on-hold',
    COMPLETED: 'bg-status-completed',
    CANCELLED: 'bg-status-cancelled'
  };

  const labels: Record<ProjectStatus, string> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On Hold',
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
