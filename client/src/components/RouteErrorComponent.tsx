/**
 * RouteErrorComponent - TanStack Router Error Handler
 *
 * This component is displayed when a route fails to load.
 * TanStack Router calls this when:
 * - A route loader throws an error
 * - A route component throws during render
 * - Navigation to a route fails
 *
 * KEY CONCEPTS:
 *
 * 1. How to use:
 *    - Set as `errorComponent` in route definition
 *    - Can be set globally on root route
 *    - Can be overridden per-route for custom handling
 *
 * 2. Error info available:
 *    - error: The Error object that was thrown
 *    - reset: Function to retry the failed operation
 *    - info: Additional error context from TanStack Router
 *
 * @see https://tanstack.com/router/latest/docs/framework/react/guide/route-trees#error-component
 */

import { Link, useRouter } from '@tanstack/react-router';
import type { ErrorComponentProps } from '@tanstack/react-router';

/**
 * RouteErrorComponent
 *
 * Displays a user-friendly error page when route loading fails.
 * Provides options to retry or navigate home.
 */
export function RouteErrorComponent({ error, reset }: ErrorComponentProps) {
  const router = useRouter();

  /**
   * Handle retry - attempts to reload the current route
   *
   * reset() is provided by TanStack Router to:
   * - Clear the error state
   * - Re-attempt the failed navigation/load
   */
  function handleRetry(): void {
    reset();
  }

  /**
   * Handle going back - navigates to previous page
   */
  function handleGoBack(): void {
    router.history.back();
  }

  return (
    <div className="min-h-[400px] flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-danger-bg border-comic-heavy shadow-comic-lg p-6 text-center">
        <div className="text-danger text-5xl mb-4">🚫</div>
        <h2 className="text-display text-2xl text-ink mb-2">
          Failed to load page
        </h2>
        <p className="text-danger text-sm mb-4">
          We couldn't load this page. This might be a temporary issue.
        </p>

        {/* Show error message in development */}
        {error instanceof Error && (
          <p className="text-danger text-xs font-mono mb-4 p-3 bg-paper border-comic overflow-auto max-h-24">
            {error.message}
          </p>
        )}

        {/* Action buttons */}
        <div className="flex gap-3 justify-center">
          <button
            onClick={handleRetry}
            className="px-6 py-3 bg-danger text-paper border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
          >
            Try Again
          </button>
          <button
            onClick={handleGoBack}
            className="px-6 py-3 bg-paper text-ink border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
          >
            Go Back
          </button>
        </div>

        {/* Link to home as fallback */}
        <div className="mt-4">
          <Link
            to="/"
            className="text-amber-dark text-display hover:text-amber-vivid"
          >
            ← Return to home
          </Link>
        </div>
      </div>
    </div>
  );
}
