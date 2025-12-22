/**
 * Project Detail Route - /projects/:projectId
 *
 * This file demonstrates:
 * - Fetching a single project by ID
 * - Fetching related data (tasks in this project)
 * - Multiple useQuery calls in one component
 * - useMutation for delete operations
 * - Edit mode with inline form
 *
 * PARALLEL QUERIES:
 * We fetch the project and its tasks simultaneously.
 * TanStack Query makes this easy - just call useQuery twice!
 */

import { useState } from 'react';
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchProjectById,
  deleteProject,
  projectKeys
} from '../../api/projects';
import { fetchTasksByProjectId, taskKeys } from '../../api/tasks';
import { ProjectForm } from '../../components/ProjectForm';
import type { ProjectStatus, Task, TaskStatus } from '../../types/api';

export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage
});

function ProjectDetailPage() {
  const { projectId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const id = parseInt(projectId, 10);

  // State for edit mode and delete confirmation
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Fetch project details
  const {
    data: project,
    isPending: isProjectPending,
    isError: isProjectError,
    error: projectError
  } = useQuery({
    queryKey: projectKeys.detail(id),
    queryFn: () => fetchProjectById(id),
    enabled: !isNaN(id)
  });

  /**
   * Fetch tasks for this project
   *
   * This query runs in parallel with the project query.
   * Each has its own loading/error state.
   *
   * NOTE: We use a separate query key that includes projectId,
   * so tasks are cached per-project.
   */
  const {
    data: tasks,
    isPending: isTasksPending,
    isError: isTasksError
  } = useQuery({
    queryKey: taskKeys.listByProject(id),
    queryFn: () => fetchTasksByProjectId(id),
    enabled: !isNaN(id)
  });

  /**
   * Delete mutation
   */
  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.lists() });
      navigate({ to: '/projects' });
    }
  });

  function handleDelete() {
    deleteMutation.mutate(id);
  }

  // Invalid ID
  if (isNaN(id)) {
    return (
      <div className="p-6">
        <BackLink />
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mt-4">
          <p className="text-red-800 font-medium">Invalid project ID</p>
          <p className="text-red-600 text-sm mt-1">
            "{projectId}" is not a valid project ID
          </p>
        </div>
      </div>
    );
  }

  // Loading state
  if (isProjectPending) {
    return (
      <div className="p-6">
        <BackLink />
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 mt-4 animate-pulse">
          <div className="h-7 bg-gray-200 rounded w-1/2 mb-4"></div>
          <div className="h-4 bg-gray-100 rounded w-3/4 mb-2"></div>
          <div className="h-4 bg-gray-100 rounded w-1/2"></div>
        </div>
      </div>
    );
  }

  // Error state
  if (isProjectError) {
    return (
      <div className="p-6">
        <BackLink />
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mt-4">
          <p className="text-red-800 font-medium">Failed to load project</p>
          <p className="text-red-600 text-sm mt-1">
            {projectError instanceof Error
              ? projectError.message
              : 'Unknown error occurred'}
          </p>
        </div>
      </div>
    );
  }

  // Success state
  return (
    <div className="p-6">
      <BackLink />

      {/* Project details card */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 mt-4 mb-6">
        {isEditing ? (
          // Edit mode
          <>
            <h2 className="text-lg font-semibold text-gray-800 mb-4">
              Edit Project
            </h2>
            <ProjectForm
              project={project}
              onSuccess={() => setIsEditing(false)}
              onCancel={() => setIsEditing(false)}
            />
          </>
        ) : (
          // View mode
          <>
            <div className="flex items-start justify-between mb-4">
              <h1 className="text-2xl font-bold text-gray-800">
                {project.name}
              </h1>
              <ProjectStatusBadge status={project.status} />
            </div>

            {project.description ? (
              <p className="text-gray-600 mb-6">{project.description}</p>
            ) : (
              <p className="text-gray-400 italic mb-6">No description</p>
            )}

            <div className="border-t border-gray-100 pt-4 space-y-2">
              <MetadataRow label="Project ID" value={String(project.id)} />
              <MetadataRow label="Owner ID" value={String(project.appUserId)} />
            </div>

            {/* Action Buttons */}
            <div className="border-t border-gray-100 pt-4 mt-4 flex gap-3">
              <button
                onClick={() => setIsEditing(true)}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              >
                Edit Project
              </button>
              <button
                onClick={() => setShowDeleteConfirm(true)}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
              >
                Delete Project
              </button>
            </div>

            {/* Delete Confirmation */}
            {showDeleteConfirm && (
              <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md">
                <p className="text-red-800 font-medium">
                  Are you sure you want to delete this project?
                </p>
                <p className="text-red-600 text-sm mt-1">
                  This will also affect any tasks assigned to this project.
                </p>
                <div className="mt-3 flex gap-3">
                  <button
                    onClick={handleDelete}
                    disabled={deleteMutation.isPending}
                    className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50"
                  >
                    {deleteMutation.isPending ? 'Deleting...' : 'Yes, Delete'}
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirm(false)}
                    disabled={deleteMutation.isPending}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                </div>
                {deleteMutation.isError && (
                  <p className="text-red-600 text-sm mt-2">
                    {deleteMutation.error instanceof Error
                      ? deleteMutation.error.message
                      : 'Failed to delete project'}
                  </p>
                )}
              </div>
            )}
          </>
        )}
      </div>

      {/* Project tasks section */}
      <div>
        <h2 className="text-lg font-semibold text-gray-800 mb-3">
          Project Tasks
        </h2>

        {isTasksPending ? (
          <div className="space-y-2">
            {[1, 2].map((i) => (
              <div
                key={i}
                className="bg-gray-50 p-3 rounded border border-gray-200 animate-pulse"
              >
                <div className="h-4 bg-gray-200 rounded w-1/3"></div>
              </div>
            ))}
          </div>
        ) : isTasksError ? (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <p className="text-red-600 text-sm">Failed to load tasks</p>
          </div>
        ) : tasks && tasks.length > 0 ? (
          <div className="space-y-2">
            {tasks.map((task) => (
              <TaskRow key={task.id} task={task} />
            ))}
          </div>
        ) : (
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 text-center">
            <p className="text-gray-500 text-sm">
              No tasks in this project yet
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

// =============================================================================
// HELPER COMPONENTS
// =============================================================================

function BackLink() {
  return (
    <Link
      to="/projects"
      className="text-blue-600 hover:text-blue-800 text-sm flex items-center gap-1"
    >
      <span>←</span>
      <span>Back to projects</span>
    </Link>
  );
}

function ProjectStatusBadge({ status }: { status: ProjectStatus }) {
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

function TaskStatusBadge({ status }: { status: TaskStatus }) {
  const styles: Record<TaskStatus, string> = {
    TODO: 'bg-gray-100 text-gray-800',
    IN_PROGRESS: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800'
  };

  return (
    <span className={`px-2 py-0.5 text-xs rounded ${styles[status]}`}>
      {status.replace('_', ' ')}
    </span>
  );
}

function TaskRow({ task }: { task: Task }) {
  return (
    <Link
      to="/tasks/$taskId"
      params={{ taskId: String(task.id) }}
      className="block bg-gray-50 p-3 rounded border border-gray-200 hover:bg-gray-100 transition-colors"
    >
      <div className="flex items-center justify-between">
        <span className="text-sm text-gray-700">{task.title}</span>
        <TaskStatusBadge status={task.status} />
      </div>
    </Link>
  );
}

function MetadataRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center text-sm">
      <span className="text-gray-500 w-24">{label}:</span>
      <span className="text-gray-800 font-mono">{value}</span>
    </div>
  );
}
