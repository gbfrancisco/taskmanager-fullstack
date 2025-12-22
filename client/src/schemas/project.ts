/**
 * Project Validation Schema
 *
 * This file defines the Zod schema for project form validation.
 *
 * Unlike tasks, projects use a single schema for both create and edit
 * because there are no conditional validation rules (like the future date
 * requirement for tasks).
 *
 * VALIDATION RULES:
 * - name: Required, 1-100 characters
 * - description: Optional, max 500 characters
 * - status: Required (always has a default from the dropdown)
 */

import { z } from 'zod';

// =============================================================================
// PROJECT STATUS VALUES
// =============================================================================

/**
 * Valid project status values as a Zod enum.
 *
 * This matches our ProjectStatus type from api.ts.
 */
const projectStatusValues = [
  'PLANNING',
  'ACTIVE',
  'ON_HOLD',
  'COMPLETED',
  'CANCELLED'
] as const;

// =============================================================================
// PROJECT SCHEMA
// =============================================================================

/**
 * Schema for creating and editing projects.
 *
 * Since there are no differences between create and edit validation,
 * we use a single schema for both operations.
 */
export const projectSchema = z.object({
  /**
   * Name - Required, 1-100 characters
   *
   * .min(1) provides a better error message than the default "Required"
   * .max(100) prevents overly long project names
   */
  name: z
    .string()
    .min(1, 'Project name is required')
    .max(100, 'Project name must be 100 characters or less'),

  /**
   * Description - Optional, max 500 characters
   *
   * Empty string → undefined conversion is handled in the form's
   * onSubmit handler. This keeps the schema type compatible with
   * React Hook Form's defaultValues.
   */
  description: z
    .string()
    .max(500, 'Description must be 500 characters or less')
    .optional(),

  /**
   * Status - Required enum value
   *
   * The form always provides a default value, so this will always be valid.
   * We include it in the schema for type safety.
   */
  status: z.enum(projectStatusValues)
});

// =============================================================================
// TYPE INFERENCE
// =============================================================================

/**
 * TypeScript type inferred from the schema.
 *
 * The resulting type is:
 * {
 *   name: string
 *   description?: string | undefined
 *   status: 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED'
 * }
 */
export type ProjectFormData = z.infer<typeof projectSchema>;
