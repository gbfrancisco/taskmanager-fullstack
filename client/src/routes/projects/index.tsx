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
      <div className="p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">Projects</h1>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 animate-pulse"
            >
              <div className="h-5 bg-gray-200 rounded w-2/3 mb-2"></div>
              <div className="h-4 bg-gray-100 rounded w-full mb-3"></div>
              <div className="h-4 bg-gray-100 rounded w-1/2"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // Error state
  if (isError) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">Projects</h1>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800 font-medium">Failed to load projects</p>
          <p className="text-red-600 text-sm mt-1">
            {error instanceof Error ? error.message : 'Unknown error occurred'}
          </p>
        </div>
      </div>
    );
  }

  // Success state
  return (
    <div className="p-6">
      {/* Header with title and create button */}
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold text-gray-800">Projects</h1>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          {showCreateForm ? 'Cancel' : '+ New Project'}
        </button>
      </div>

      {/* Create Project Form */}
      {showCreateForm && (
        <div className="bg-white border border-gray-200 rounded-lg p-4 mb-6 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-800 mb-3">
            Create New Project
          </h2>
          <ProjectForm
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {projects.length === 0 ? (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
          <p className="text-gray-600">
            No projects yet. Create your first project!
          </p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
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
      className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
    >
      <h3 className="font-semibold text-gray-800 mb-2">{project.name}</h3>

      {project.description ? (
        <p className="text-sm text-gray-500 mb-3 line-clamp-2">
          {project.description}
        </p>
      ) : (
        <p className="text-sm text-gray-400 italic mb-3">No description</p>
      )}

      <div className="flex justify-between items-center">
        <span className="text-xs text-gray-400">ID: {project.id}</span>
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
    PLANNING: 'bg-yellow-100 text-yellow-800',
    ACTIVE: 'bg-green-100 text-green-800',
    ON_HOLD: 'bg-orange-100 text-orange-800',
    COMPLETED: 'bg-blue-100 text-blue-800',
    CANCELLED: 'bg-red-100 text-red-800'
  };

  const labels: Record<ProjectStatus, string> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On Hold',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled'
  };

  return (
    <span className={`px-2 py-1 text-xs rounded ${styles[status]}`}>
      {labels[status]}
    </span>
  );
}
