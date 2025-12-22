/**
 * ProjectForm Component - Create and edit projects
 *
 * Similar pattern to TaskForm - demonstrates useMutation for projects.
 *
 * KEY CONCEPTS:
 * - useMutation for create/update operations
 * - Cache invalidation after successful mutations
 * - Controlled form inputs
 * - Reusable form component for both create and edit modes
 */

import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createProject, updateProject, projectKeys } from '../api/projects'
import type { Project, ProjectCreateInput, ProjectStatus } from '../types/api'

// =============================================================================
// COMPONENT PROPS
// =============================================================================

interface ProjectFormProps {
  /**
   * Existing project data for edit mode.
   * If undefined, the form is in "create" mode.
   */
  project?: Project

  /**
   * Callback when form is successfully submitted.
   */
  onSuccess?: () => void

  /**
   * Callback when user cancels the form.
   */
  onCancel?: () => void
}

// =============================================================================
// CONSTANTS
// =============================================================================

/**
 * Available project statuses for the dropdown.
 */
const PROJECT_STATUSES: { value: ProjectStatus; label: string }[] = [
  { value: 'PLANNING', label: 'Planning' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_HOLD', label: 'On Hold' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

/**
 * Temporary hardcoded user ID.
 * TODO: Replace with actual authenticated user when auth is implemented.
 */
const TEMP_USER_ID = 1

// =============================================================================
// COMPONENT
// =============================================================================

export function ProjectForm({ project, onSuccess, onCancel }: ProjectFormProps) {
  const isEditing = !!project

  // ---------------------------------------------------------------------------
  // FORM STATE
  // ---------------------------------------------------------------------------

  const [name, setName] = useState(project?.name ?? '')
  const [description, setDescription] = useState(project?.description ?? '')
  const [status, setStatus] = useState<ProjectStatus>(project?.status ?? 'PLANNING')

  // ---------------------------------------------------------------------------
  // QUERY CLIENT & MUTATIONS
  // ---------------------------------------------------------------------------

  const queryClient = useQueryClient()

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.lists() })
      onSuccess?.()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (input: { id: number; data: Parameters<typeof updateProject>[1] }) =>
      updateProject(input.id, input.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.lists() })
      if (project) {
        queryClient.invalidateQueries({ queryKey: projectKeys.detail(project.id) })
      }
      onSuccess?.()
    },
  })

  const mutation = isEditing ? updateMutation : createMutation
  const isPending = mutation.isPending

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    if (isEditing && project) {
      updateMutation.mutate({
        id: project.id,
        data: {
          name,
          description: description || undefined,
          status,
        },
      })
    } else {
      const input: ProjectCreateInput = {
        name,
        description: description || undefined,
        status,
        appUserId: TEMP_USER_ID,
      }
      createMutation.mutate(input)
    }
  }

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Name Field - Required */}
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
          Project Name <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="Enter project name"
        />
      </div>

      {/* Description Field - Optional */}
      <div>
        <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
          Description
        </label>
        <textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="Enter project description (optional)"
        />
      </div>

      {/* Status Dropdown */}
      <div>
        <label htmlFor="status" className="block text-sm font-medium text-gray-700 mb-1">
          Status
        </label>
        <select
          id="status"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProjectStatus)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          {PROJECT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {/* Error Message */}
      {mutation.isError && (
        <div className="bg-red-50 border border-red-200 rounded-md p-3">
          <p className="text-red-800 text-sm">
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
          className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? 'Saving...' : isEditing ? 'Update Project' : 'Create Project'}
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
  )
}
