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
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createTask,
  updateTask,
  assignTaskToProject,
  removeTaskFromProject,
  taskKeys
} from '../api/tasks';
import { fetchProjectsByUserId, projectKeys } from '../api/projects';
import {
  taskCreateSchema,
  taskEditSchema,
  type TaskFormData
} from '../schemas/task';
import type { Task, TaskCreateInput } from '../types/api';

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

/**
 * Temporary hardcoded user ID.
 * In a real app, this would come from authentication context.
 * TODO: Replace with actual authenticated user when auth is implemented.
 */
const TEMP_USER_ID = 1;

// =============================================================================
// COMPONENT
// =============================================================================

export function TaskForm({ task, onSuccess, onCancel }: TaskFormProps) {
  // Determine if we're creating or editing
  const isEditing = !!task;

  // Track original projectId to detect changes in edit mode
  const originalProjectId = task?.projectId ?? null;

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
      projectId: task?.projectId ?? null,
      dueDate: task?.dueDate ? task.dueDate.split('T')[0] : '',
      dueTime: task?.dueDate
        ? (task.dueDate.split('T')[1]?.slice(0, 5) ?? '')
        : '',
      includeTime: task?.dueDate
        ? task.dueDate.split('T')[1] !== '00:00:00'
        : false
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
   * Fetch projects belonging to the current user for the dropdown.
   *
   * We filter by userId to only show projects the user owns.
   * This is more scalable and secure than fetching all projects.
   */
  const { data: projects } = useQuery({
    queryKey: projectKeys.listByUser(TEMP_USER_ID),
    queryFn: () => fetchProjectsByUserId(TEMP_USER_ID)
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
    mutationFn: (input: {
      id: number;
      data: Parameters<typeof updateTask>[1];
    }) => updateTask(input.id, input.data)
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
    mutationFn: async ({
      taskId,
      newProjectId
    }: {
      taskId: number;
      newProjectId: number | null;
    }) => {
      if (newProjectId === null) {
        return removeTaskFromProject(taskId);
      } else {
        return assignTaskToProject(taskId, newProjectId);
      }
    }
  });

  // Combine for easier access in the UI
  const mutation = isEditing ? updateMutation : createMutation;
  const isPending =
    isSubmitting || mutation.isPending || projectAssignmentMutation.isPending;

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
      const timeValue =
        data.includeTime && data.dueTime ? data.dueTime : '00:00';
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
        const projectChanged = data.projectId !== originalProjectId;
        if (projectChanged) {
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
      const input: TaskCreateInput = {
        title: data.title,
        description: data.description,
        status: data.status,
        dueDate: formattedDueDate,
        appUserId: TEMP_USER_ID,
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
    if (!watchIncludeTime) {
      setValue('dueTime', '');
    }
  }, [watchIncludeTime, setValue]);

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* Title Field - Required */}
      <div>
        <label
          htmlFor="title"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Title <span className="text-red-500">*</span>
        </label>
        {/*
          register('title') returns:
          - name: 'title'
          - ref: for DOM access
          - onChange: updates form state
          - onBlur: triggers validation
        */}
        <input
          type="text"
          id="title"
          {...register('title')}
          className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
            errors.title ? 'border-red-500' : 'border-gray-300'
          }`}
          placeholder="Enter task title"
        />
        {/* Error message - shown only when there's an error */}
        {errors.title && (
          <p className="text-red-600 text-sm mt-1">{errors.title.message}</p>
        )}
      </div>

      {/* Description Field - Optional */}
      <div>
        <label
          htmlFor="description"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Description
        </label>
        <textarea
          id="description"
          {...register('description')}
          rows={3}
          className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
            errors.description ? 'border-red-500' : 'border-gray-300'
          }`}
          placeholder="Enter task description (optional)"
        />
        {errors.description && (
          <p className="text-red-600 text-sm mt-1">
            {errors.description.message}
          </p>
        )}
      </div>

      {/* Status Dropdown */}
      <div>
        <label
          htmlFor="status"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Status
        </label>
        <select
          id="status"
          {...register('status')}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          {TASK_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {/* Project Dropdown - Optional */}
      <div>
        <label
          htmlFor="projectId"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Project
        </label>
        {/*
          For select with numbers, we need special handling:
          - register returns onChange that passes string
          - We need to convert to number for our schema
          - Empty string means null (no project)
        */}
        <select
          id="projectId"
          {...register('projectId', {
            setValueAs: (v) => (v === '' ? null : parseInt(v, 10))
          })}
          defaultValue={task?.projectId ?? ''}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="">No project</option>
          {projects?.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <p className="text-xs text-gray-500 mt-1">
          {isEditing
            ? 'Move task to a different project'
            : 'Assign this task to a project (optional)'}
        </p>
      </div>

      {/* Due Date & Time - Optional */}
      <div>
        <label
          htmlFor="dueDate"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Due Date
        </label>
        <div className="flex gap-2">
          <input
            type="date"
            id="dueDate"
            {...register('dueDate')}
            className={`flex-1 px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.dueDate ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {watchDueDate && (
            <input
              type="time"
              id="dueTime"
              {...register('dueTime')}
              disabled={!watchIncludeTime}
              className="w-32 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:text-gray-400"
            />
          )}
        </div>
        {errors.dueDate && (
          <p className="text-red-600 text-sm mt-1">{errors.dueDate.message}</p>
        )}
        {watchDueDate && (
          <label className="flex items-center gap-2 mt-2 text-sm text-gray-600">
            <input
              type="checkbox"
              {...register('includeTime')}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            Include specific time
            {!watchIncludeTime && (
              <span className="text-gray-400">(defaults to 00:00)</span>
            )}
          </label>
        )}
      </div>

      {/* Server Error Message - show errors from mutations */}
      {(mutation.isError || projectAssignmentMutation.isError) && (
        <div className="bg-red-50 border border-red-200 rounded-md p-3">
          <p className="text-red-800 text-sm">
            {mutation.error instanceof Error
              ? mutation.error.message
              : projectAssignmentMutation.error instanceof Error
                ? projectAssignmentMutation.error.message
                : 'An error occurred. Please try again.'}
          </p>
        </div>
      )}

      {/* Form Actions */}
      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          disabled={isPending}
          className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? 'Saving...' : isEditing ? 'Update Task' : 'Create Task'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
