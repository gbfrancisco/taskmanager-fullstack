/**
 * Authentication Validation Schemas
 *
 * Zod schemas for login and registration form validation.
 * These are client-side validations - the backend will perform its own checks.
 */

import { z } from 'zod';

// =============================================================================
// LOGIN SCHEMA
// =============================================================================

/**
 * Schema for login form validation.
 *
 * Validation rules:
 * - usernameOrEmail: Required (accepts either username or email)
 * - password: Required, 8+ characters
 */
export const loginSchema = z.object({
  usernameOrEmail: z
    .string()
    .min(1, 'Username or email is required'),

  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters')
});

export type LoginFormData = z.infer<typeof loginSchema>;

// =============================================================================
// REGISTRATION SCHEMA
// =============================================================================

/**
 * Schema for registration form validation.
 *
 * Validation rules:
 * - username: Required, 3-50 chars, alphanumeric + underscore only
 * - email: Required, valid email format
 * - password: Required, 8+ characters
 * - confirmPassword: Must match password
 *
 * The .refine() at the end validates that passwords match.
 */
export const registerSchema = z.object({
    username: z
      .string()
      .min(1, 'Username is required')
      .min(3, 'Username must be at least 3 characters')
      .max(50, 'Username must be 50 characters or less')
      .regex(
        /^[a-zA-Z0-9_-]+$/,
        'Username can only contain letters, numbers, underscores, and hyphens'
      ),

    // z.email() is the Zod v4 top-level validator (z.string().email() is deprecated)
    // It validates both non-empty and valid email format
    email: z.email({ error: 'Please enter a valid email address' }),

    password: z
      .string()
      .min(1, 'Password is required')
      .min(8, 'Password must be at least 8 characters'),

    confirmPassword: z.string().min(1, 'Please confirm your password')
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'] // Error appears on confirmPassword field
  });

export type RegisterFormData = z.infer<typeof registerSchema>;
