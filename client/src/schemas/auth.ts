/**
 * Authentication Validation Schemas
 *
 * Zod schemas for login form validation.
 * Currently a skeleton - will be extended when backend auth is implemented.
 */

import { z } from 'zod';

// =============================================================================
// LOGIN SCHEMA
// =============================================================================

/**
 * Schema for login form validation.
 *
 * Basic validation rules:
 * - username: Required, 3-50 characters
 * - password: Required, 8+ characters
 *
 * Note: These are client-side validations only. The backend will perform
 * its own validation and authentication checks.
 */
export const loginSchema = z.object({
  username: z
    .string()
    .min(1, 'Username is required')
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be 50 characters or less'),

  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters')
});

// =============================================================================
// TYPE INFERENCE
// =============================================================================

/**
 * TypeScript type inferred from the login schema.
 *
 * Results in:
 * {
 *   username: string
 *   password: string
 * }
 */
export type LoginFormData = z.infer<typeof loginSchema>;
