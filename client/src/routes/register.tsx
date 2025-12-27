/**
 * Register Page Route - /register
 *
 * Registration page with:
 * - Route guard: Already authenticated users are redirected to home
 * - Registration form with validation
 * - Auto-login after successful registration
 */

import { createFileRoute, Link, redirect, useNavigate } from '@tanstack/react-router';
import { RegisterForm } from '@/components/RegisterForm';

// =============================================================================
// ROUTE DEFINITION
// =============================================================================

export const Route = createFileRoute('/register')({
  // Redirect if already authenticated
  beforeLoad: ({ context }) => {
    if (context.auth.isAuthenticated) {
      throw redirect({ to: '/' });
    }
  },

  component: RegisterPage
});

// =============================================================================
// PAGE COMPONENT
// =============================================================================

function RegisterPage() {
  const navigate = useNavigate();

  /**
   * Handle successful registration - navigate to home
   * User is automatically logged in after registration
   */
  function handleRegisterSuccess() {
    navigate({ to: '/' });
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="max-w-md mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-display text-4xl text-ink mb-2">Create Account</h1>
          <p className="text-ink-soft">
            Join Task Manager to organize your work.
          </p>
        </div>

        {/* Register Form Card */}
        <div className="bg-paper border-comic shadow-comic-soft-lg p-6">
          <RegisterForm onSuccess={handleRegisterSuccess} />
        </div>

        {/* Back to home link */}
        <div className="text-center mt-6">
          <Link to="/" className="text-ink-soft hover:text-ink underline text-sm">
            Back to home
          </Link>
        </div>
      </div>
    </div>
  );
}
