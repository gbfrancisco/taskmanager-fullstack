/**
 * ProjectForm Component - Create and edit projects with validation
 *
 * This component demonstrates the same React Hook Form + Zod pattern as TaskForm,
 * but with a simpler schema (no conditional validation like future date).
 *
 * KEY DIFFERENCES FROM TASKFORM:
 * - Single schema for both create and edit (no conditional rules)
 * - Fewer fields (name, description, status)
 * - No complex state dependencies (like dueDate/dueTime)
 *
 * SAME PATTERNS:
 * - useForm with zodResolver
 * - mode: 'onBlur' for validation timing
 * - register() for connecting inputs
 * - errors.fieldName?.message for error display
 * - Red border styling on invalid fields
 */

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createProject, updateProject, projectKeys } from '@/api/projects';
import { projectSchema, type ProjectFormData } from '@/schemas/project';
import type { Project, ProjectCreateInput } from '@/types/api';

// =============================================================================
// COMPONENT PROPS
// =============================================================================

interface ProjectFormProps {
  /**
   * Existing project data for edit mode.
   * If undefined, the form is in "create" mode.
   */
  project?: Project;

  /**
   * Callback when form is successfully submitted.
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
 * Available project statuses for the dropdown.
 */
const PROJECT_STATUSES = [
  { value: 'PLANNING', label: 'Planning' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_HOLD', label: 'On Hold' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' }
] as const;

/**
 * Temporary hardcoded user ID.
 * TODO: Replace with actual authenticated user when auth is implemented.
 */
const TEMP_USER_ID = 1;

// =============================================================================
// COMPONENT
// =============================================================================

export function ProjectForm({
  project,
  onSuccess,
  onCancel
}: ProjectFormProps) {
  const isEditing = !!project;

  // ---------------------------------------------------------------------------
  // REACT HOOK FORM SETUP
  // ---------------------------------------------------------------------------

  /**
   * useForm configuration for project form.
   *
   * Unlike TaskForm, we use a single schema (projectSchema) for both
   * create and edit because there are no conditional validation rules.
   */
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<ProjectFormData>({
    resolver: zodResolver(projectSchema),
    mode: 'onBlur',
    defaultValues: {
      name: project?.name ?? '',
      description: project?.description ?? '',
      status: project?.status ?? 'PLANNING'
    }
  });

  // ---------------------------------------------------------------------------
  // QUERY CLIENT & MUTATIONS
  // ---------------------------------------------------------------------------

  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.lists() });
      onSuccess?.();
    }
  });

  const updateMutation = useMutation({
    mutationFn: (input: {
      id: number;
      data: Parameters<typeof updateProject>[1];
    }) => updateProject(input.id, input.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.lists() });
      if (project) {
        queryClient.invalidateQueries({
          queryKey: projectKeys.detail(project.id)
        });
      }
      onSuccess?.();
    }
  });

  const mutation = isEditing ? updateMutation : createMutation;
  const isPending = isSubmitting || mutation.isPending;

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  /**
   * Handle form submission.
   *
   * Called by React Hook Form's handleSubmit() after validation passes.
   * The 'data' parameter is the validated form data typed by our Zod schema.
   */
  function onSubmit(data: ProjectFormData) {
    if (isEditing && project) {
      updateMutation.mutate({
        id: project.id,
        data: {
          name: data.name,
          description: data.description,
          status: data.status
        }
      });
    } else {
      const input: ProjectCreateInput = {
        name: data.name,
        description: data.description,
        status: data.status,
        appUserId: TEMP_USER_ID
      };
      createMutation.mutate(input);
    }
  }

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* Name Field - Required */}
      <div>
        <label
          htmlFor="name"
          className="block text-display text-ink mb-1"
        >
          Project Name <span className="text-danger">*</span>
        </label>
        <input
          type="text"
          id="name"
          autoComplete="off"
          {...register('name')}
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.name ? 'border-danger' : ''
          }`}
          placeholder="Enter project name"
        />
        {errors.name && (
          <p className="text-danger text-sm mt-1">{errors.name.message}</p>
        )}
      </div>

      {/* Description Field - Optional */}
      <div>
        <label
          htmlFor="description"
          className="block text-display text-ink mb-1"
        >
          Description
        </label>
        <textarea
          id="description"
          {...register('description')}
          rows={3}
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.description ? 'border-danger' : ''
          }`}
          placeholder="Enter project description (optional)"
        />
        {errors.description && (
          <p className="text-danger text-sm mt-1">
            {errors.description.message}
          </p>
        )}
      </div>

      {/* Status Dropdown */}
      <div>
        <label
          htmlFor="status"
          className="block text-display text-ink mb-1"
        >
          Status
        </label>
        <select
          id="status"
          {...register('status')}
          className="w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2"
        >
          {PROJECT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {/* Server Error Message - show errors from mutations */}
      {mutation.isError && (
        <div className="bg-danger-bg border-comic p-4">
          <p className="text-danger text-sm font-medium">
            {mutation.error instanceof Error
              ? mutation.error.message
              : 'An error occurred. Please try again.'}
          </p>
        </div>
      )}

      {/* Form Actions */}
      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          disabled={isPending}
          className="flex-1 bg-amber-vivid text-ink border-comic shadow-comic py-3 px-6 text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending
            ? 'Saving...'
            : isEditing
              ? 'Update Project'
              : 'Create Project'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="px-6 py-3 bg-paper text-ink border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2 disabled:opacity-50"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
