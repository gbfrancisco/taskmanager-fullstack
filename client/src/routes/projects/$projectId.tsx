/**
 * Project Detail Route - /projects/:projectId
 *
 * This file demonstrates:
 * - Using route parameters with TanStack Query
 * - Fetching a single resource by ID
 * - Parallel queries (project + tasks)
 * - useMutation for delete operations
 * - Edit mode with inline form
 *
 * IMPORTANT: Route params are always strings!
 * We need to parse them to numbers for API calls.
 */

import { useState } from 'react';
import { createFileRoute, Link, redirect, useNavigate } from '@tanstack/react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchProjectById,
  deleteProject,
  projectKeys
} from '@/api/projects';
import { fetchTasksByProjectId, taskKeys } from '@/api/tasks';
import { ProjectForm } from '@/components/ProjectForm';
import { RouteErrorComponent } from '@/components/RouteErrorComponent';
import type { Task, ProjectStatus } from '@/types/api';
import { formatDate } from '@/utils/dateUtils';

export const Route = createFileRoute('/projects/$projectId')({
  // Route guard: redirect to login if not authenticated
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname }
      });
    }
  },
  component: ProjectDetailPage,
  errorComponent: RouteErrorComponent
});

// =============================================================================
// PROJECT DETAIL PAGE COMPONENT
// =============================================================================

function ProjectDetailPage() {
  // Get the projectId from the URL
  const { projectId } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  /**
   * Parse the projectId to a number
   *
   * Route params are always strings, but our API expects numbers.
   * parseInt converts "123" → 123
   */
  const id = parseInt(projectId, 10);

  // State for edit mode and delete confirmation
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // ---------------------------------------------------------------------------
  // QUERIES - Parallel fetching
  // ---------------------------------------------------------------------------

  /**
   * Fetch project data
   *
   * The query key includes the project ID, so each project has its own
   * cached entry. If you navigate away and back, the cached data
   * is shown immediately while a background refetch happens.
   */
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
   * This runs in parallel with the project query above.
   * Each query is independent and has its own loading/error states.
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

  // ---------------------------------------------------------------------------
  // DELETE MUTATION
  // ---------------------------------------------------------------------------

  /**
   * Delete mutation
   *
   * After successfully deleting, we:
   * 1. Invalidate the projects list (so it refetches without this project)
   * 2. Navigate back to the projects list
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

  // ---------------------------------------------------------------------------
  // CONDITIONAL RENDERS
  // ---------------------------------------------------------------------------

  // Invalid ID handling
  if (isNaN(id)) {
    return <ErrorDisplay title="Invalid ID" message="Campaign ID not recognized." />;
  }

  // Loading state
  if (isProjectPending) {
    return <LoadingDisplay />;
  }

  // Error state
  if (isProjectError) {
    return (
      <ErrorDisplay
        title="Access Denied"
        message={projectError instanceof Error ? projectError.message : 'Unknown error'}
      />
    );
  }

  // ---------------------------------------------------------------------------
  // RENDER - Success state
  // ---------------------------------------------------------------------------

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">

      {/* Breadcrumb / Back link */}
      <div className="mb-6">
        <Link to="/projects" className="group flex items-center gap-2 font-bold text-sm uppercase tracking-wider text-ink hover:text-amber-dark">
          <span className="bg-ink text-paper px-1 group-hover:-translate-x-1 transition-transform">&larr;</span>
          <span>Return to Projects</span>
        </Link>
      </div>

      <div className="grid lg:grid-cols-12 gap-8">

        {/* Left Column: Case File (Span 8) */}
        <div className="lg:col-span-8">
          <div className="bg-paper border-comic-heavy shadow-comic-soft-lg relative min-h-[400px]">

            {/* Visual Tab at top (Only visible in View Mode) */}
            {!isEditing && (
              <div className="absolute -top-4 left-0 bg-ink text-paper px-6 py-1 skew-x-[-10deg] border-b-0">
                <span className="font-mono font-bold text-xs uppercase tracking-widest skew-x-[10deg]">
                  Case File #{project.id}
                </span>
              </div>
            )}

            <div className="p-8 pt-10">
              {isEditing ? (
                // Edit mode - show the form
                <div className="relative bg-paper-dark border-2 border-dashed border-ink p-6 shadow-inner">

                  {/* Background Watermark */}
                  <div className="absolute inset-0 flex items-center justify-center opacity-[0.03] pointer-events-none overflow-hidden select-none">
                    <span className="text-8xl font-black -rotate-12">EDITING</span>
                  </div>

                  {/* Header Strip */}
                  <div className="relative z-10 bg-amber-vivid border-comic px-4 py-2 mb-8 inline-block -rotate-1 shadow-comic-sm">
                    <h2 className="text-display text-lg uppercase tracking-widest text-ink font-black flex items-center gap-2">
                      <span>⚠</span> Protocol Amendment
                    </h2>
                  </div>

                  <ProjectForm
                    project={project}
                    onSuccess={() => setIsEditing(false)}
                    onCancel={() => setIsEditing(false)}
                  />
                </div>
              ) : (
                // View mode - show project details
                <>
                  {/* Header with title and status */}
                  <div className="flex justify-between items-start border-b-4 border-ink pb-4 mb-6">
                    <h1 className="text-display text-5xl md:text-6xl text-ink uppercase leading-[0.9]">
                      {project.name}
                    </h1>
                    <ProjectStatusStamp status={project.status} />
                  </div>

                  {/* Description */}
                  <div className="mb-8">
                    <h3 className="font-bold text-xs uppercase bg-amber-vivid text-ink inline-block px-2 mb-2">Synopsis</h3>
                    <p className="text-lg leading-relaxed text-ink-soft">
                      {project.description || <span className="italic text-ink-light">No mission briefing available.</span>}
                    </p>
                  </div>

                  {/* Metadata Grid */}
                  <div className="bg-halftone border-comic p-4 grid grid-cols-2 gap-4 mb-6">
                    <MetaItem label="Commander" value={project.appUser.username} />
                    <MetaItem label="Initiated" value={formatDate(project.createdTimestamp)} />
                    <MetaItem label="Last Update" value={formatDate(project.updatedTimestamp)} />
                    <MetaItem label="Directives" value={`${project.taskCount} Active`} highlight />
                  </div>

                  {/* Action Buttons */}
                  <div className="flex flex-wrap gap-4 pt-4 border-t-2 border-dashed border-ink/30">
                    <button
                      onClick={() => setIsEditing(true)}
                      className="bg-paper text-ink border-comic px-6 py-3 font-display uppercase tracking-widest shadow-comic-soft-interactive"
                    >
                      Update
                    </button>
                    <button
                      onClick={() => setShowDeleteConfirm(true)}
                      className="bg-paper text-danger border-comic px-6 py-3 font-display uppercase tracking-widest shadow-comic-soft-interactive"
                    >
                      Delete
                    </button>
                  </div>

                  {/* Delete Confirmation Dialog */}
                  {showDeleteConfirm && (
                    <div className="mt-6 p-4 bg-danger-bg border-comic-heavy">
                      <p className="text-danger font-black uppercase text-lg mb-2">⚠ Warning: Destructive Action</p>
                      <p className="text-sm font-medium mb-4">
                        {project.taskCount > 0
                          ? `This campaign contains ${project.taskCount} active mission(s). Deleting it will wipe all associated data.`
                          : "Are you sure you want to close this file permanently?"}
                      </p>
                      <div className="flex gap-3">
                        <button
                          onClick={handleDelete}
                          disabled={deleteMutation.isPending}
                          className="bg-danger text-paper border-comic px-4 py-2 font-bold shadow-comic-soft-interactive disabled:opacity-50"
                        >
                          {deleteMutation.isPending ? 'WIPING DATA...' : 'CONFIRM DELETE'}
                        </button>
                        <button
                          onClick={() => setShowDeleteConfirm(false)}
                          className="bg-paper text-ink border-comic px-4 py-2 font-bold shadow-comic-soft-interactive"
                        >
                          CANCEL
                        </button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Objectives List (Span 4) */}
        <div className="lg:col-span-4">
          <div className="sticky top-6">
            <h2 className="text-display text-2xl mb-4 bg-ink text-amber-vivid inline-block px-3 py-1 -rotate-1 shadow-comic-sm">
              Campaign Objectives
            </h2>

            <div className="space-y-3">
              {isTasksPending ? (
                <div className="animate-pulse space-y-3">
                  <div className="h-12 bg-paper-dark border-comic"></div>
                  <div className="h-12 bg-paper-dark border-comic"></div>
                </div>
              ) : isTasksError ? (
                <div className="bg-danger-bg border-comic p-3 font-bold text-danger text-sm">
                  Failed to load tactical data.
                </div>
              ) : tasks && tasks.length > 0 ? (
                tasks.map((task) => (
                  <TaskStrip key={task.id} task={task} />
                ))
              ) : (
                <div className="bg-paper-dark border-comic p-6 text-center border-dashed">
                  <p className="text-ink-soft text-sm font-medium">
                    No active objectives.<br/>Add tasks to this campaign.
                  </p>
                </div>
              )}
            </div>

            {/* Quick Link to Add Task */}
            <div className="mt-4 text-center">
              <Link to="/tasks" className="text-xs font-bold uppercase underline hover:text-amber-vivid">
                + Manage Tasks at Task Board
              </Link>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}

// =============================================================================
// HELPER COMPONENTS
// =============================================================================

/**
 * TaskStrip - Compact task item for the sidebar
 *
 * Shows task title with checkbox visual indicating completion status.
 */
function TaskStrip({ task }: { task: Task }) {
  const isComplete = task.status === 'COMPLETED';
  const isCancelled = task.status === 'CANCELLED';

  return (
    <Link
      to="/tasks/$taskId"
      params={{ taskId: String(task.id) }}
      className={`
        group block bg-paper border-comic p-3 shadow-comic-soft-interactive
        ${isComplete ? 'opacity-70' : ''}
      `}
    >
      <div className="flex items-center gap-3">
        {/* Checkbox Visual */}
        <div className={`
          w-5 h-5 border-2 border-ink flex items-center justify-center
          ${isComplete ? 'bg-success' : isCancelled ? 'bg-danger' : 'bg-paper'}
        `}>
          {isComplete && <span className="text-paper text-xs font-black">✓</span>}
          {isCancelled && <span className="text-paper text-xs font-black">✕</span>}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className={`text-sm font-bold truncate ${isComplete || isCancelled ? 'line-through text-ink-light' : 'text-ink'}`}>
            {task.title}
          </div>
        </div>

        {/* Mini Badge */}
        <span className="text-[10px] font-mono border border-ink px-1 bg-paper-dark">
          #{task.id}
        </span>
      </div>
    </Link>
  );
}

/**
 * ProjectStatusStamp - Large rotated status badge
 *
 * Uses a double border for a "stamped" effect.
 */
function ProjectStatusStamp({ status }: { status: ProjectStatus }) {
  const styles: Record<ProjectStatus, string> = {
    PLANNING: 'text-ink-light border-ink-light rotate-[-10deg]',
    ACTIVE: 'text-success border-success rotate-[5deg]',
    ON_HOLD: 'text-status-on-hold border-status-on-hold rotate-[-5deg]',
    COMPLETED: 'text-status-progress border-status-progress rotate-[-10deg]',
    CANCELLED: 'text-danger border-danger rotate-[-10deg]'
  };

  return (
    <div className={`
      border-4 border-double px-2 py-1 uppercase font-black tracking-widest text-lg opacity-80
      ${styles[status] || 'text-ink border-ink'}
    `}>
      {status.replace('_', ' ')}
    </div>
  );
}

/**
 * MetaItem - Displays a label/value pair in the metadata grid
 */
interface MetaItemProps {
  label: string;
  value: string;
  highlight?: boolean;
}

function MetaItem({ label, value, highlight }: MetaItemProps) {
  return (
    <div>
      <div className="text-[10px] uppercase font-bold text-ink-light">{label}</div>
      <div className={`font-mono text-sm font-bold truncate ${highlight ? 'text-amber-dark' : 'text-ink'}`}>
        {value}
      </div>
    </div>
  );
}

/**
 * LoadingDisplay - Skeleton placeholder while loading
 */
function LoadingDisplay() {
  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="h-8 w-32 bg-paper-dark mb-6 animate-pulse"></div>
      <div className="grid lg:grid-cols-12 gap-8">
        <div className="lg:col-span-8 h-96 bg-paper border-comic animate-pulse"></div>
        <div className="lg:col-span-4 h-64 bg-paper border-comic animate-pulse"></div>
      </div>
    </div>
  );
}

/**
 * ErrorDisplay - Shows error message with link back to safety
 */
function ErrorDisplay({ title, message }: { title: string; message: string }) {
  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="bg-danger-bg border-comic p-6">
        <h2 className="text-display text-2xl text-danger">{title}</h2>
        <p>{message}</p>
        <Link to="/projects" className="underline mt-4 inline-block">
          Return to Projects
        </Link>
      </div>
    </div>
  );
}
