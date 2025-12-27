/**
 * LoginForm Component - Authentication form skeleton
 *
 * This is a UI skeleton for the login form. Currently it:
 * - Validates input with React Hook Form + Zod
 * - Shows validation errors
 * - Logs a TODO message on submit (no actual auth yet)
 *
 * TODO: Integrate with backend authentication when implemented
 */

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, type LoginFormData } from '@/schemas/auth';

// =============================================================================
// COMPONENT
// =============================================================================

export function LoginForm() {
  // Track submission state manually since we don't have a real mutation yet
  const [isSubmitting, setIsSubmitting] = useState(false);

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
      username: '',
      password: ''
    }
  });

  // ---------------------------------------------------------------------------
  // FORM SUBMISSION
  // ---------------------------------------------------------------------------

  async function onSubmit(data: LoginFormData) {
    setIsSubmitting(true);

    // TODO: Replace with actual authentication API call
    // Example future implementation:
    // const response = await loginUser(data.username, data.password);
    // if (response.token) {
    //   localStorage.setItem('token', response.token);
    //   navigate({ to: '/' });
    // }

    console.log('TODO: Implement login', data);

    // Simulate network delay for demo
    await new Promise((resolve) => setTimeout(resolve, 500));

    alert('Login not implemented yet. Check console for form data.');
    setIsSubmitting(false);
  }

  // ---------------------------------------------------------------------------
  // RENDER
  // ---------------------------------------------------------------------------

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
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
          autoComplete="username"
          className={`w-full px-4 py-3 bg-paper border-comic shadow-comic-sm focus:outline-none focus:ring-2 focus:ring-amber-vivid focus:ring-offset-2 ${
            errors.username ? 'border-danger' : ''
          }`}
          placeholder="Enter your username"
        />
        {errors.username && (
          <p className="text-danger text-sm mt-1">{errors.username.message}</p>
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

      {/* Future: Link to register page */}
      <p className="text-center text-sm text-ink-light mt-4">
        Don't have an account?{' '}
        <span className="text-ink underline cursor-not-allowed">
          Register (coming soon)
        </span>
      </p>
    </form>
  );
}
