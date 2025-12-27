/**
 * Login Page Route - /login
 *
 * Authentication page with:
 * - Redirect search param: /login?redirect=/tasks returns user to /tasks after login
 * - Route guard: Already authenticated users are redirected to home
 * - Login form with error handling
 *
 * Demo credentials: demo / password123
 */

import { createFileRoute, Link, redirect, useNavigate } from '@tanstack/react-router';
import { z } from 'zod';
import { LoginForm } from '@/components/LoginForm';

// =============================================================================
// SEARCH PARAMS SCHEMA
// =============================================================================

/**
 * Validate the ?redirect search parameter.
 * This allows /login?redirect=/tasks to redirect after login.
 */
const loginSearchSchema = z.object({
  redirect: z.string().optional()
});

// =============================================================================
// ROUTE DEFINITION
// =============================================================================

export const Route = createFileRoute('/login')({
  // Validate search params with Zod
  validateSearch: loginSearchSchema,

  // Redirect if already authenticated
  beforeLoad: ({ context, search }) => {
    if (context.auth.isAuthenticated) {
      throw redirect({ to: search.redirect || '/' });
    }
  },

  component: LoginPage
});

// =============================================================================
// PAGE COMPONENT
// =============================================================================

function LoginPage() {
  const navigate = useNavigate();
  const { redirect: redirectTo } = Route.useSearch();

  /**
   * Handle successful login - navigate to redirect param or home
   */
  function handleLoginSuccess() {
    navigate({ to: redirectTo || '/' });
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="max-w-md mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-display text-4xl text-ink mb-2">Sign In</h1>
          <p className="text-ink-soft">
            Welcome back! Enter your credentials to continue.
          </p>
        </div>

        {/* Login Form Card */}
        <div className="bg-paper border-comic shadow-comic-soft-lg p-6">
          <LoginForm onSuccess={handleLoginSuccess} />
        </div>

        {/* Demo credentials hint */}
        <div className="text-center mt-4 text-sm text-ink-soft">
          Demo: <span className="font-mono">demo</span> /{' '}
          <span className="font-mono">password123</span>
        </div>

        {/* Back to home link */}
        <div className="text-center mt-4">
          <Link to="/" className="text-ink-soft hover:text-ink underline text-sm">
            Back to home
          </Link>
        </div>
      </div>
    </div>
  );
}
