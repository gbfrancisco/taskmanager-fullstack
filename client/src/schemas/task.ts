/**
 * Task Validation Schemas
 *
 * This file defines Zod schemas for task form validation.
 *
 * WHY ZOD?
 * Zod is a TypeScript-first schema validation library that:
 * - Provides type inference (no need to define types separately)
 * - Works seamlessly with React Hook Form via @hookform/resolvers
 * - Offers a fluent, chainable API for defining validation rules
 *
 * KEY CONCEPTS:
 *
 * 1. z.object() - Defines an object schema with typed properties
 * 2. z.string() - Validates string values
 * 3. .min(n) / .max(n) - Length constraints
 * 4. .optional() - Makes a field optional (can be undefined)
 * 5. .nullable() - Allows null values
 * 6. .refine() - Custom validation logic with access to all form data
 * 7. z.infer<typeof schema> - Extracts TypeScript type from schema
 *
 * TWO SCHEMAS PATTERN:
 * We define separate schemas for create vs edit because:
 * - Create: Due date must be in the future (you shouldn't create overdue tasks)
 * - Edit: Any date allowed (user might be editing an existing overdue task)
 */

import { z } from 'zod'

// =============================================================================
// TASK STATUS VALUES
// =============================================================================

/**
 * Valid task status values as a Zod enum.
 *
 * z.enum() creates a schema that only accepts these exact string values.
 * This matches our TaskStatus type from api.ts.
 */
const taskStatusValues = [
  'TODO',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
] as const

// =============================================================================
// BASE SCHEMA (SHARED FIELDS)
// =============================================================================

/**
 * Base schema with fields common to both create and edit.
 *
 * We extract this to avoid duplication between taskCreateSchema and taskEditSchema.
 */
const taskBaseSchema = {
  /**
   * Title - Required, 1-100 characters
   *
   * .min(1) ensures the field is not empty (better message than just "required")
   * .max(100) prevents overly long titles
   */
  title: z
    .string()
    .min(1, 'Title is required')
    .max(100, 'Title must be 100 characters or less'),

  /**
   * Description - Optional, max 500 characters
   *
   * .max() applies only if a value is provided
   * .optional() allows undefined (not provided)
   *
   * Note: Empty string → undefined conversion is handled in the
   * form's onSubmit handler, not here. This keeps the schema type
   * compatible with React Hook Form's defaultValues.
   */
  description: z
    .string()
    .max(500, 'Description must be 500 characters or less')
    .optional(),

  /**
   * Status - Required enum value
   *
   * Uses the taskStatusValues defined above.
   * The form always has a default, so this will always be valid.
   */
  status: z.enum(taskStatusValues),

  /**
   * Project ID - Optional, can be null or a number
   *
   * .nullable() allows null (no project selected)
   * .optional() allows undefined (field not included)
   */
  projectId: z.number().nullable().optional(),

  /**
   * Due Date - Optional date string (YYYY-MM-DD format)
   *
   * The actual date validation (future date) is done in the refine()
   * because it needs access to multiple fields (date + time + includeTime).
   */
  dueDate: z.string().optional(),

  /**
   * Due Time - Optional time string (HH:mm format)
   *
   * Only relevant when includeTime is true.
   */
  dueTime: z.string().optional(),

  /**
   * Include Time - Whether the time input is enabled
   *
   * Used in the refine() validation to determine how to construct
   * the full datetime for validation.
   */
  includeTime: z.boolean().optional(),
}

// =============================================================================
// CREATE SCHEMA (WITH FUTURE DATE VALIDATION)
// =============================================================================

/**
 * Schema for creating a new task.
 *
 * Includes validation that due date (if set) must be in the future.
 *
 * HOW REFINE() WORKS:
 * - Runs AFTER all field validations pass
 * - Receives the full parsed data object
 * - Returns true if valid, false if invalid
 * - The second argument configures the error message and which field to attach it to
 */
export const taskCreateSchema = z.object(taskBaseSchema).refine(
  (data) => {
    // If no due date is set, validation passes
    if (!data.dueDate) {
      return true
    }

    // Construct the full datetime string
    // If includeTime is true and time is provided, use it
    // Otherwise default to midnight (00:00)
    const timeValue = data.includeTime && data.dueTime ? data.dueTime : '00:00'
    const dateTimeString = `${data.dueDate}T${timeValue}:00`

    // Parse and compare to current time
    const dueDateTime = new Date(dateTimeString)
    const now = new Date()

    return dueDateTime > now
  },
  {
    // Error message shown when validation fails
    message: 'Due date must be in the future',
    // Which field to attach the error to (for display purposes)
    path: ['dueDate'],
  },
)

// =============================================================================
// EDIT SCHEMA (NO FUTURE DATE REQUIREMENT)
// =============================================================================

/**
 * Schema for editing an existing task.
 *
 * Same as create schema but WITHOUT the future date validation.
 * This allows users to edit tasks that are already overdue.
 *
 * NOTE: We don't use .refine() here, so all validation is field-level only.
 */
export const taskEditSchema = z.object(taskBaseSchema)

// =============================================================================
// TYPE INFERENCE
// =============================================================================

/**
 * TypeScript type inferred from the schema.
 *
 * z.infer<typeof schema> extracts the TypeScript type that matches
 * the schema's shape. This gives us compile-time type safety without
 * having to manually define a separate interface.
 *
 * The resulting type is:
 * {
 *   title: string
 *   description?: string | undefined
 *   status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
 *   projectId?: number | null | undefined
 *   dueDate?: string | undefined
 *   dueTime?: string | undefined
 *   includeTime?: boolean | undefined
 * }
 */
export type TaskFormData = z.infer<typeof taskCreateSchema>
