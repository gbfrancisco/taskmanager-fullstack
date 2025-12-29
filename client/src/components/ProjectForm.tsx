/**
 * ProjectForm Component - Create and edit projects with validation
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
 *
 * 2. zodResolver - Bridges React Hook Form and Zod
 *    - Validates form data against Zod schema
 *    - Maps Zod errors to React Hook Form's error format
 *
 * 3. mode: 'onBlur' - When to validate
 *    - 'onBlur': Validate when user leaves a field (our choice)
 *    - 'onChange': Validate on every keystroke (too aggressive)
 *    - 'onSubmit': Only validate on form submission (too late)
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

/**
 * ProjectFormProps
 *
 * @param project - Existing project data for edit mode. If undefined, form is in create mode.
 * @param onSuccess - Callback when form is successfully submitted.
 * @param onCancel - Callback when user cancels the form.
 */
interface ProjectFormProps {
  project?: Project;
  onSuccess?: () => void;
  onCancel?: () => void;
}

// =============================================================================
// CONSTANTS
// =============================================================================

/**
 * Available project statuses for the dropdown.
 * Matches the ProjectStatus type from our API types.
 */
const PROJECT_STATUSES = [
  { value: 'PLANNING', label: 'Planning Phase' },
  { value: 'ACTIVE', label: 'Active Operation' },
  { value: 'ON_HOLD', label: 'On Hold / Frozen' },
  { value: 'COMPLETED', label: 'Mission Accomplished' },
  { value: 'CANCELLED', label: 'Scrubbed' }
] as const;

// =============================================================================
// COMPONENT
// =============================================================================

export function ProjectForm({
  project,
  onSuccess,
  onCancel
}: ProjectFormProps) {
  // Determine if we're creating or editing
  const isEditing = !!project;

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
    mutationFn: createProject,
    onSuccess: () => {
      // Invalidate the projects list so it refetches with the new project
      queryClient.invalidateQueries({ queryKey: projectKeys.lists() });
      onSuccess?.();
    }
  });

  // ---------------------------------------------------------------------------
  // UPDATE MUTATION
  // ---------------------------------------------------------------------------

  /**
   * Update mutation - similar pattern, different API function.
   *
   * We also invalidate the detail query for the specific project being edited.
   */
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

  // Combine for easier access in the UI
  const mutation = isEditing ? updateMutation : createMutation;
  const isPending = isSubmitting || mutation.isPending;

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  /**
   * Handle form submission.
   *
   * This function is wrapped by React Hook Form's handleSubmit(),
   * which validates the form before calling this function.
   * If validation fails, this function is NOT called.
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
        status: data.status
      };
      createMutation.mutate(input);
    }
  }

  // ---------------------------------------------------------------------------
  // STYLES
  // ---------------------------------------------------------------------------

  /**
   * Shared styles for form elements.
   * Extracted to constants for consistency and easy modification.
   */
  const labelStyle = "block text-sm font-bold uppercase tracking-wide text-ink mb-1";
  const inputStyle = "w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-4 focus:ring-amber-vivid/50 transition-all";
  const errorStyle = "text-danger text-sm mt-1 font-bold";

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">

      {/* Name and Status - Side by side on larger screens */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-start">

        {/* Name Field - Required (Spans 2 columns) */}
        <div className="md:col-span-2">
          <label htmlFor="name" className={labelStyle}>
            Campaign Codename <span className="text-danger">*</span>
          </label>
          <input
            type="text"
            id="name"
            autoComplete="off"
            {...register('name')}
            className={`${inputStyle} ${errors.name ? 'border-danger' : ''}`}
            placeholder="E.g. Operation Skyfall"
          />
          {errors.name && (
            <p className={errorStyle}>{errors.name.message}</p>
          )}
        </div>

        {/* Status Field (Spans 1 column) */}
        <div className="md:col-span-1">
          <label htmlFor="status" className={labelStyle}>
            Operational Status
          </label>
          <div className="bg-halftone border-2 border-dashed border-ink p-1">
            <select
              id="status"
              {...register('status')}
              className="w-full px-3 py-2 bg-paper border-2 border-ink focus:outline-none focus:ring-2 focus:ring-amber-vivid cursor-pointer font-bold uppercase text-sm"
            >
              {PROJECT_STATUSES.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Description Field - Optional */}
      <div>
        <label htmlFor="description" className={labelStyle}>
          Mission Synopsis
        </label>
        <div className="relative">
          <textarea
            id="description"
            {...register('description')}
            rows={4}
            className={`${inputStyle} ${errors.description ? 'border-danger' : ''} leading-relaxed resize-none`}
            placeholder="Brief operational overview..."
          />
          {/* Subtle watermark inside the textarea container */}
          <div className="absolute bottom-3 right-3 pointer-events-none opacity-10">
            <span className="font-black text-2xl text-ink uppercase">Confidential</span>
          </div>
        </div>
        {errors.description && (
          <p className={errorStyle}>
            {errors.description.message}
          </p>
        )}
      </div>

      {/* Server Error Message - show errors from mutations */}
      {mutation.isError && (
        <div className="bg-danger-bg border-comic p-3">
          <p className="text-danger font-bold">
            {mutation.error instanceof Error
              ? mutation.error.message
              : 'Protocol Failure: Submission rejected.'}
          </p>
        </div>
      )}

      {/* Form Actions */}
      <div className="flex gap-4 pt-4 border-t-4 border-ink">
        <button
          type="submit"
          disabled={isPending}
          className="flex-1 bg-amber-vivid text-ink border-comic shadow-[4px_4px_0_black] py-4 px-6 text-xl font-display uppercase tracking-widest hover:-translate-y-1 hover:shadow-[6px_6px_0_black] active:translate-y-0 active:shadow-[2px_2px_0_black] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending
            ? 'Processing...'
            : isEditing
              ? 'Update Protocols'
              : 'Launch Campaign'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="px-6 py-4 bg-paper text-ink border-comic shadow-[4px_4px_0_black] text-xl font-display uppercase hover:-translate-y-1 hover:shadow-[6px_6px_0_black] hover:bg-paper-dark active:translate-y-0 active:shadow-[2px_2px_0_black] transition-all disabled:opacity-50"
          >
            Abort
          </button>
        )}
      </div>
    </form>
  );
}
