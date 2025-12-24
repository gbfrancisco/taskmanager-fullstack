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
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="max-w-3xl mx-auto">
          <BackLink />
          <div className="bg-danger-bg border-comic p-4 mt-4">
            <p className="text-danger font-medium">Invalid project ID</p>
            <p className="text-danger text-sm mt-1">
              "{projectId}" is not a valid project ID
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Loading state
  if (isProjectPending) {
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
  if (isProjectError) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="max-w-3xl mx-auto">
          <BackLink />
          <div className="bg-danger-bg border-comic p-4 mt-4">
            <p className="text-danger font-medium">Failed to load project</p>
            <p className="text-danger text-sm mt-1">
              {projectError instanceof Error
                ? projectError.message
                : 'Unknown error occurred'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Success state
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="max-w-3xl mx-auto">
        <BackLink />

        {/* Project details card */}
        <div className="bg-paper p-6 border-comic shadow-comic-soft-lg mt-4 mb-6">
        {isEditing ? (
          // Edit mode
          <>
            <h2 className="text-display text-xl text-ink mb-4">
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
              <h1 className="text-display text-3xl text-ink">
                {project.name}
              </h1>
              <ProjectStatusBadge status={project.status} />
            </div>

            {project.description ? (
              <p className="text-ink-soft mb-6">{project.description}</p>
            ) : (
              <p className="text-ink-light italic mb-6">No description</p>
            )}

            <div className="border-t-2 border-ink pt-4 space-y-2">
              <MetadataRow label="Project ID" value={String(project.id)} />
              <MetadataRow label="Owner" value={project.appUser.username} />
            </div>

            {/* Action Buttons */}
            <div className="border-t-2 border-ink pt-4 mt-4 flex gap-3">
              <button
                onClick={() => setIsEditing(true)}
                className="px-6 py-3 bg-amber-vivid text-ink border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
              >
                Edit Project
              </button>
              <button
                onClick={() => setShowDeleteConfirm(true)}
                className="px-6 py-3 bg-danger text-paper border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
              >
                Delete Project
              </button>
            </div>

            {/* Delete Confirmation */}
            {showDeleteConfirm && (
              <div className="mt-4 p-4 bg-danger-bg border-comic">
                <p className="text-ink font-medium">
                  Are you sure you want to delete this project?
                </p>
                {project.taskCount > 0 ? (
                  <p className="text-danger text-sm mt-1">
                    This project has {project.taskCount} task
                    {project.taskCount !== 1 ? 's' : ''}. Deleting it will
                    permanently remove all associated tasks.
                  </p>
                ) : (
                  <p className="text-danger text-sm mt-1">
                    This action cannot be undone.
                  </p>
                )}
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
        <h2 className="text-display text-2xl text-ink mb-4">
          Project Tasks
        </h2>

        {isTasksPending ? (
          <div className="space-y-3">
            {[1, 2].map((i) => (
              <div
                key={i}
                className="bg-paper p-3 border-comic shadow-comic-sm animate-pulse"
              >
                <div className="h-4 bg-paper-dark w-1/3"></div>
              </div>
            ))}
          </div>
        ) : isTasksError ? (
          <div className="bg-danger-bg border-comic p-3">
            <p className="text-danger text-sm">Failed to load tasks</p>
          </div>
        ) : tasks && tasks.length > 0 ? (
          <div className="space-y-3">
            {tasks.map((task) => (
              <TaskRow key={task.id} task={task} />
            ))}
          </div>
        ) : (
          <div className="bg-paper-dark border-comic p-4 text-center">
            <p className="text-ink-soft text-sm">
              No tasks in this project yet
            </p>
          </div>
        )}
        </div>
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
      className="text-amber-dark text-display hover:text-amber-vivid flex items-center gap-1"
    >
      <span>←</span>
      <span>Back to projects</span>
    </Link>
  );
}

function ProjectStatusBadge({ status }: { status: ProjectStatus }) {
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

function TaskStatusBadge({ status }: { status: TaskStatus }) {
  const styles: Record<TaskStatus, string> = {
    TODO: 'bg-status-todo',
    IN_PROGRESS: 'bg-status-progress',
    COMPLETED: 'bg-status-complete',
    CANCELLED: 'bg-status-cancelled'
  };

  return (
    <span
      className={`px-2 py-0.5 text-xs text-ink border-2 border-ink shadow-comic-sm ${styles[status]}`}
    >
      {status.replace('_', ' ')}
    </span>
  );
}

function TaskRow({ task }: { task: Task }) {
  return (
    <Link
      to="/tasks/$taskId"
      params={{ taskId: String(task.id) }}
      className="block bg-paper p-3 border-comic shadow-comic-soft-interactive"
    >
      <div className="flex items-center justify-between">
        <span className="text-sm text-ink">{task.title}</span>
        <TaskStatusBadge status={task.status} />
      </div>
    </Link>
  );
}

function MetadataRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center text-sm">
      <span className="text-ink-light w-24">{label}:</span>
      <span className="text-ink font-mono">{value}</span>
    </div>
  );
}
