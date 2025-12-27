/**
 * Login Page Route - /login
 *
 * Authentication page skeleton. Currently just displays the login form
 * without actual authentication functionality.
 *
 * TODO: Add route guard to redirect authenticated users to home
 * TODO: Handle login success and redirect to previous page or home
 */

import { createFileRoute, Link } from '@tanstack/react-router';
import { LoginForm } from '@/components/LoginForm';

export const Route = createFileRoute('/login')({
  component: LoginPage
});

function LoginPage() {
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
          <LoginForm />
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
