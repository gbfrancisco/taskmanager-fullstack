/**
 * RegisterForm Component
 *
 * Handles user registration with:
 * - Form validation using React Hook Form + Zod
 * - Mock registration via AuthContext (auto-login after register)
 * - Error display for duplicate username/email
 * - Loading state during submission
 *
 * Usage:
 * ```tsx
 * <RegisterForm onSuccess={() => navigate({ to: '/' })} />
 * ```
 */

import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerSchema, type RegisterFormData } from '@/schemas/auth';
import { useAuth } from '@/contexts/AuthContext';

// =============================================================================
// TYPES
// =============================================================================

interface RegisterFormProps {
  /** Called after successful registration - typically used for navigation */
  onSuccess?: () => void;
}

// =============================================================================
// COMPONENT
// =============================================================================

export function RegisterForm({ onSuccess }: RegisterFormProps) {
  const { register: registerUser } = useAuth();

  // Track submission state and registration errors
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [registerError, setRegisterError] = useState<string | null>(null);

  // ---------------------------------------------------------------------------
  // REACT HOOK FORM SETUP
  // ---------------------------------------------------------------------------

  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
    defaultValues: {
      username: '',
      email: '',
      password: '',
      confirmPassword: ''
    }
  });

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  async function onSubmit(data: RegisterFormData) {
    setIsSubmitting(true);
    setRegisterError(null);

    try {
      await registerUser(data.username, data.email, data.password);
      onSuccess?.();
    } catch (err) {
      // Display registration error (e.g., "Username already taken")
      setRegisterError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setIsSubmitting(false);
    }
  }

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* Registration Error Display */}
      {registerError && (
        <div className="bg-red-50 border-2 border-danger p-3">
          <p className="text-danger text-sm">{registerError}</p>
        </div>
      )}

      {/* Username Field */}
      <div>
        <label
          htmlFor="username"
          className="block text-display text-ink mb-1"
        >
          Username
        </label>
        <input
          type="text"
          id="username"
          {...register('username')}
          autoComplete="off"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.username ? 'border-danger' : ''
          }`}
          placeholder="Choose a username"
        />
        {errors.username && (
          <p className="text-danger text-sm mt-1">{errors.username.message}</p>
        )}
      </div>

      {/* Email Field */}
      <div>
        <label
          htmlFor="email"
          className="block text-display text-ink mb-1"
        >
          Email
        </label>
        <input
          type="email"
          id="email"
          {...register('email')}
          autoComplete="off"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.email ? 'border-danger' : ''
          }`}
          placeholder="Enter your email"
        />
        {errors.email && (
          <p className="text-danger text-sm mt-1">{errors.email.message}</p>
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
          autoComplete="new-password"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.password ? 'border-danger' : ''
          }`}
          placeholder="Create a password (8+ characters)"
        />
        {errors.password && (
          <p className="text-danger text-sm mt-1">{errors.password.message}</p>
        )}
      </div>

      {/* Confirm Password Field */}
      <div>
        <label
          htmlFor="confirmPassword"
          className="block text-display text-ink mb-1"
        >
          Confirm Password
        </label>
        <input
          type="password"
          id="confirmPassword"
          {...register('confirmPassword')}
          autoComplete="new-password"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.confirmPassword ? 'border-danger' : ''
          }`}
          placeholder="Confirm your password"
        />
        {errors.confirmPassword && (
          <p className="text-danger text-sm mt-1">
            {errors.confirmPassword.message}
          </p>
        )}
      </div>

      {/* Submit Button */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-amber-vivid text-ink border-comic shadow-comic py-3 px-6 text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? 'Creating account...' : 'Create Account'}
        </button>
      </div>

      {/* Link to login page */}
      <p className="text-center text-sm text-ink-light mt-4">
        Already have an account?{' '}
        <Link to="/login" className="text-ink underline hover:text-amber-vivid">
          Sign in
        </Link>
      </p>
    </form>
  );
}
