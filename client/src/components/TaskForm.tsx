/**
 * TaskForm Component - Create and edit tasks with validation
 *
 * This component demonstrates:
 * - React Hook Form for form state management
 * - Zod for schema-based validation
 * - Integration with TanStack Query mutations
 *
 * KEY CONCEPTS:
 *
 * 1. useForm() - React Hook Form's main hook
 *    - register: Connects inputs to form state (replaces value/onChange)
 *    - handleSubmit: Wraps your submit handler with validation
 *    - formState: Contains errors, isSubmitting, isDirty, etc.
 *    - watch: Subscribe to field value changes
 *    - setValue: Programmatically set field values
 *
 * 2. zodResolver - Bridges React Hook Form and Zod
 *    - Validates form data against Zod schema
 *    - Maps Zod errors to React Hook Form's error format
 *
 * 3. mode: 'onBlur' - When to validate
 *    - 'onBlur': Validate when user leaves a field (our choice)
 *    - 'onChange': Validate on every keystroke (too aggressive)
 *    - 'onSubmit': Only validate on form submission (too late)
 *
 * VALIDATION FLOW:
 * 1. User types in a field
 * 2. User leaves the field (blur event)
 * 3. React Hook Form runs Zod validation for that field
 * 4. If invalid, error appears below the field
 * 5. User fixes the error
 * 6. On next blur or submit, error clears
 */

import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createTask,
  updateTask,
  assignTaskToProject,
  removeTaskFromProject,
  taskKeys
} from '@/api/tasks';
import { fetchProjects, projectKeys } from '@/api/projects';
import {
  taskCreateSchema,
  taskEditSchema,
  type TaskFormData
} from '@/schemas/task';
import type { Task, TaskCreateInput } from '@/types/api';

// =============================================================================
// COMPONENT PROPS
// =============================================================================

interface TaskFormProps {
  /**
   * Existing task data for edit mode.
   * If undefined, the form is in "create" mode.
   */
  task?: Task;

  /**
   * Callback when form is successfully submitted.
   * Use this to close modals, navigate away, etc.
   */
  onSuccess?: () => void;

  /**
   * Callback when user cancels the form.
   */
  onCancel?: () => void;
}

// =============================================================================
// CONSTANTS
// =============================================================================

/**
 * Available task statuses for the dropdown.
 * Matches the TaskStatus type from our API types.
 */
const TASK_STATUSES = [
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' }
] as const;

// =============================================================================
// COMPONENT
// =============================================================================

export function TaskForm({ task, onSuccess, onCancel }: TaskFormProps) {
  // Determine if we're creating or editing
  const isEditing = !!task;

  // Track original projectId to detect changes in edit mode
  const originalProjectId = task?.project?.id ?? null;

  // ---------------------------------------------------------------------------
  // REACT HOOK FORM SETUP
  // ---------------------------------------------------------------------------

  /**
   * useForm - The core hook from React Hook Form
   *
   * Configuration options:
   * - resolver: zodResolver connects our Zod schema
   * - mode: 'onBlur' validates when user leaves a field
   * - defaultValues: Pre-populate form with existing data (for edit) or defaults
   *
   * Returns:
   * - register: Function to connect inputs to form state
   * - handleSubmit: Wrapper that validates before calling your submit handler
   * - watch: Subscribe to field value changes
   * - setValue: Programmatically update field values
   * - formState: Object containing errors, isSubmitting, isDirty, etc.
   */
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    control,
    formState: { errors, isSubmitting }
  } = useForm<TaskFormData>({
    // Use different schema for create vs edit (future date validation differs)
    resolver: zodResolver(isEditing ? taskEditSchema : taskCreateSchema),
    // Validate on blur - balances UX and immediate feedback
    mode: 'onBlur',
    // Pre-populate with existing data or defaults
    defaultValues: {
      title: task?.title ?? '',
      description: task?.description ?? '',
      status: task?.status ?? 'TODO',
      projectId: task?.project?.id ?? null,
      dueDate: task?.dueDate ? task.dueDate.split('T')[0] : '',
      dueTime: task?.dueDate ? (task.dueDate.split('T')[1]?.slice(0, 5) ?? '') : '',
      includeTime: task?.dueDate ? task.dueDate.split('T')[1] !== '00:00:00' : false
    }
  });

  /**
   * watch() - Subscribe to field value changes
   *
   * We watch dueDate and includeTime to conditionally show/hide the time input.
   * Unlike useState, this doesn't cause re-renders on every keystroke.
   */
  const watchDueDate = watch('dueDate');
  const watchIncludeTime = watch('includeTime');

  // ---------------------------------------------------------------------------
  // FETCH PROJECTS FOR DROPDOWN
  // ---------------------------------------------------------------------------

  /**
   * Fetch projects for the dropdown.
   *
   * The backend automatically returns only projects belonging to the
   * authenticated user (extracted from JWT token).
   */
  const { data: projects } = useQuery({
    queryKey: projectKeys.list(),
    queryFn: fetchProjects
  });

  // ---------------------------------------------------------------------------
  // QUERY CLIENT
  // ---------------------------------------------------------------------------

  /**
   * useQueryClient gives us access to the query cache.
   *
   * We need this to invalidate queries after a mutation succeeds.
   * Invalidation marks cached data as stale and triggers a refetch.
   */
  const queryClient = useQueryClient();

  // ---------------------------------------------------------------------------
  // CREATE MUTATION
  // ---------------------------------------------------------------------------

  /**
   * useMutation - The hook for create/update/delete operations
   *
   * Unlike useQuery (which runs automatically), mutations run when you call
   * mutation.mutate(data). This is perfect for form submissions.
   */
  const createMutation = useMutation({
    mutationFn: createTask,
    onSuccess: () => {
      // Invalidate the tasks list so it refetches with the new task
      queryClient.invalidateQueries({ queryKey: taskKeys.lists() });
      onSuccess?.();
    }
  });

  // ---------------------------------------------------------------------------
  // UPDATE MUTATION
  // ---------------------------------------------------------------------------

  /**
   * Update mutation - similar pattern, different API function.
   *
   * Note: We don't call onSuccess here - onSubmit coordinates
   * both the update and project assignment before calling onSuccess.
   */
  const updateMutation = useMutation({
    mutationFn: (input: { id: number; data: Parameters<typeof updateTask>[1] }) =>
      updateTask(input.id, input.data)
  });

  // ---------------------------------------------------------------------------
  // PROJECT ASSIGNMENT MUTATION
  // ---------------------------------------------------------------------------

  /**
   * Project assignment uses separate endpoints from updateTask.
   *
   * The backend has dedicated endpoints for project assignment:
   * - PUT /api/tasks/{taskId}/project/{projectId} - assign to project
   * - DELETE /api/tasks/{taskId}/project - remove from project
   *
   * We only call this when the project actually changes.
   */
  const projectAssignmentMutation = useMutation({
    mutationFn: async ({ taskId, newProjectId }: { taskId: number; newProjectId: number | null }) => {
      if (newProjectId === null) return removeTaskFromProject(taskId);
      return assignTaskToProject(taskId, newProjectId);
    }
  });

  // Combine for easier access in the UI
  const mutation = isEditing ? updateMutation : createMutation;
  const isPending = isSubmitting || mutation.isPending || projectAssignmentMutation.isPending;

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  /**
   * Handle form submission.
   *
   * This function is wrapped by React Hook Form's handleSubmit(),
   * which validates the form before calling this function.
   * If validation fails, this function is NOT called.
   *
   * The 'data' parameter is the validated form data (typed by Zod schema).
   */
  async function onSubmit(data: TaskFormData) {
    /**
     * Build the due date/time string for the backend.
     *
     * Backend expects LocalDateTime format: "2026-01-01T14:30:00"
     * - If no date is set, dueDate is undefined
     * - If date is set but no time toggle, default to 00:00:00
     * - If date and time are both set, use the specified time
     */
    let formattedDueDate: string | undefined;
    if (data.dueDate) {
      const timeValue = data.includeTime && data.dueTime ? data.dueTime : '00:00';
      formattedDueDate = `${data.dueDate}T${timeValue}:00`;
    }

    if (isEditing && task) {
      try {
        // Update task fields
        await updateMutation.mutateAsync({
          id: task.id,
          data: {
            title: data.title,
            description: data.description,
            status: data.status,
            dueDate: formattedDueDate
          }
        });

        // If project changed, call the separate project assignment endpoint
        if (data.projectId !== originalProjectId) {
          await projectAssignmentMutation.mutateAsync({
            taskId: task.id,
            newProjectId: data.projectId ?? null
          });
        }

        // Invalidate queries after all mutations succeed
        queryClient.invalidateQueries({ queryKey: taskKeys.lists() });
        queryClient.invalidateQueries({ queryKey: taskKeys.detail(task.id) });

        onSuccess?.();
      } catch {
        // Errors are handled by mutation.isError - no need to do anything here
      }
    } else {
      // Create new task - projectId is included in the create payload
      // Note: appUserId is not needed - backend extracts user from JWT token
      const input: TaskCreateInput = {
        title: data.title,
        description: data.description,
        status: data.status,
        dueDate: formattedDueDate,
        projectId: data.projectId ?? undefined
      };
      createMutation.mutate(input);
    }
  }

  // ---------------------------------------------------------------------------
  // SIDE EFFECTS
  // ---------------------------------------------------------------------------

  /**
   * Clear time when includeTime is toggled off.
   *
   * useEffect is needed because we can't do side effects inside render.
   */
  useEffect(() => {
    if (!watchIncludeTime) setValue('dueTime', '');
  }, [watchIncludeTime, setValue]);

  // ---------------------------------------------------------------------------
  // STYLES
  // ---------------------------------------------------------------------------

  /**
   * Shared styles for form elements.
   * Extracted to constants for consistency and easy modification.
   */
  const labelStyle = 'block text-sm font-bold uppercase tracking-wide text-ink mb-1';
  const inputStyle = 'w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-4 focus:ring-amber-vivid/50 transition-all';
  const errorStyle = 'text-danger text-sm mt-1 font-bold';

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

      {/* Title Field - Required */}
      <div>
        <label htmlFor="title" className={labelStyle}>
          Task Title <span className="text-danger">*</span>
        </label>
        <input
          type="text"
          id="title"
          {...register('title')}
          className={`${inputStyle} ${errors.title ? 'border-danger' : ''}`}
          placeholder="E.g. Secure the payload"
        />
        {errors.title && <p className={errorStyle}>{errors.title.message}</p>}
      </div>

      {/* Description Field - Optional */}
      <div>
        <label htmlFor="description" className={labelStyle}>
          Task Details
        </label>
        <textarea
          id="description"
          {...register('description')}
          rows={3}
          className={`${inputStyle} ${errors.description ? 'border-danger' : ''}`}
          placeholder="Additional context..."
        />
        {errors.description && <p className={errorStyle}>{errors.description.message}</p>}
      </div>

      {/* Status and Project - Side by side on larger screens */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Status Dropdown */}
        <div>
          <label htmlFor="status" className={labelStyle}>
            Current Status
          </label>
          <select id="status" {...register('status')} className={inputStyle}>
            {TASK_STATUSES.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>

        {/* Project Dropdown - Optional */}
        <div>
          <label htmlFor="projectId" className={labelStyle}>
            Assign Project
          </label>
          <Controller
            name="projectId"
            control={control}
            render={({ field }) => (
              <select
                id="projectId"
                value={field.value ?? ''}
                onChange={(e) => {
                  const val = e.target.value;
                  field.onChange(val === '' ? null : parseInt(val, 10));
                }}
                onBlur={field.onBlur}
                className={inputStyle}
              >
                <option value="">-- Independent Op --</option>
                {projects?.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            )}
          />
        </div>
      </div>

      {/* Due Date & Time - Optional, with dashed border container */}
      <div className={`p-4 border-2 border-dashed ${errors.dueDate ? 'border-danger bg-danger-bg/20' : 'border-ink bg-halftone'}`}>
        <label className={labelStyle}>Timeline Requirements</label>
        <div className="flex gap-2 flex-wrap">
          <input
            type="date"
            {...register('dueDate')}
            className={`flex-1 ${inputStyle} ${errors.dueDate ? 'border-danger text-danger' : ''}`}
          />
          {watchDueDate && (
            <div className="flex items-center gap-2 bg-paper border-comic px-3">
              <input
                type="checkbox"
                {...register('includeTime')}
                className="w-5 h-5 accent-amber-vivid"
              />
              <span className="text-sm font-bold">Set Time</span>
            </div>
          )}
          {watchIncludeTime && watchDueDate && (
            <input
              type="time"
              {...register('dueTime')}
              className={`w-32 ${inputStyle}`}
            />
          )}
        </div>
        {errors.dueDate && <p className={errorStyle}>{errors.dueDate.message}</p>}
      </div>

      {/* Server Error Message - show errors from mutations */}
      {(mutation.isError || projectAssignmentMutation.isError) && (
        <div className="bg-danger-bg border-comic p-3">
          <p className="text-danger font-bold">ERROR: Submission Failed</p>
          <p className="text-danger text-sm mt-1">
            {mutation.error instanceof Error
              ? mutation.error.message
              : projectAssignmentMutation.error instanceof Error
                ? projectAssignmentMutation.error.message
                : 'An error occurred. Please try again.'}
          </p>
        </div>
      )}

      {/* Form Actions */}
      <div className="flex gap-4 pt-4 border-t-4 border-ink">
        <button
          type="submit"
          disabled={isPending}
          className="flex-1 bg-amber-vivid text-ink border-comic shadow-comic-interactive py-4 px-6 text-xl font-display uppercase tracking-widest disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? 'Processing...' : isEditing ? 'Update Task' : 'Create Task'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="px-6 py-4 bg-paper text-ink border-comic shadow-comic-interactive text-xl font-display uppercase"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
