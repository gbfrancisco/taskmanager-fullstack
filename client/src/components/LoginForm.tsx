/**
 * LoginForm Component
 *
 * Handles user authentication with:
 * - Form validation using React Hook Form + Zod
 * - Mock authentication via AuthContext
 * - Error display for invalid credentials
 * - Loading state during submission
 *
 * Usage:
 * ```tsx
 * <LoginForm onSuccess={() => navigate({ to: '/' })} />
 * ```
 */

import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, type LoginFormData } from '@/schemas/auth';
import { useAuth } from '@/contexts/AuthContext';

// =============================================================================
// TYPES
// =============================================================================

interface LoginFormProps {
  /** Called after successful login - typically used for navigation */
  onSuccess?: () => void;
}

// =============================================================================
// COMPONENT
// =============================================================================

export function LoginForm({ onSuccess }: LoginFormProps) {
  const { login } = useAuth();

  // Track submission state and auth errors
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  // ---------------------------------------------------------------------------
  // REACT HOOK FORM SETUP
  // ---------------------------------------------------------------------------

  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    defaultValues: {
      usernameOrEmail: '',
      password: ''
    }
  });

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  async function onSubmit(data: LoginFormData) {
    setIsSubmitting(true);
    setAuthError(null);

    try {
      await login(data.usernameOrEmail, data.password);
      onSuccess?.();
    } catch (err) {
      // Display auth error (e.g., "Invalid username or password")
      setAuthError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setIsSubmitting(false);
    }
  }

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* Auth Error Display */}
      {authError && (
        <div className="bg-red-50 border-2 border-danger p-3">
          <p className="text-danger text-sm">{authError}</p>
        </div>
      )}

      {/* Username or Email Field */}
      <div>
        <label
          htmlFor="usernameOrEmail"
          className="block text-display text-ink mb-1"
        >
          Username or Email
        </label>
        <input
          type="text"
          id="usernameOrEmail"
          {...register('usernameOrEmail')}
          autoComplete="off"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.usernameOrEmail ? 'border-danger' : ''
          }`}
          placeholder="Enter username or email"
        />
        {errors.usernameOrEmail && (
          <p className="text-danger text-sm mt-1">{errors.usernameOrEmail.message}</p>
        )}
      </div>

      {/* Password Field */}
      <div>
        <label
          htmlFor="password"
          className="block text-display text-ink mb-1"
        >
          Password
        </label>
        <input
          type="password"
          id="password"
          {...register('password')}
          autoComplete="current-password"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.password ? 'border-danger' : ''
          }`}
          placeholder="Enter your password"
        />
        {errors.password && (
          <p className="text-danger text-sm mt-1">{errors.password.message}</p>
        )}
      </div>

      {/* Submit Button */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-amber-vivid text-ink border-comic shadow-comic py-3 px-6 text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? 'Signing in...' : 'Sign In'}
        </button>
      </div>

      {/* Link to register page */}
      <p className="text-center text-sm text-ink-light mt-4">
        Don't have an account?{' '}
        <Link to="/register" className="text-ink underline hover:text-amber-vivid">
          Register
        </Link>
      </p>
    </form>
  );
}
