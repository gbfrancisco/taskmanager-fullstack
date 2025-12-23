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
      <div className="max-w-md w-full bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <div className="text-red-600 text-4xl mb-4">🚫</div>
        <h2 className="text-lg font-semibold text-red-800 mb-2">
          Failed to load page
        </h2>
        <p className="text-red-600 text-sm mb-4">
          We couldn't load this page. This might be a temporary issue.
        </p>

        {/* Show error message in development */}
        {error instanceof Error && (
          <p className="text-red-500 text-xs font-mono mb-4 p-2 bg-red-100 rounded overflow-auto max-h-24">
            {error.message}
          </p>
        )}

        {/* Action buttons */}
        <div className="flex gap-3 justify-center">
          <button
            onClick={handleRetry}
            className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
          >
            Try Again
          </button>
          <button
            onClick={handleGoBack}
            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2"
          >
            Go Back
          </button>
        </div>

        {/* Link to home as fallback */}
        <div className="mt-4">
          <Link
            to="/"
            className="text-blue-600 hover:text-blue-800 text-sm"
          >
            ← Return to home
          </Link>
        </div>
      </div>
    </div>
  );
}
