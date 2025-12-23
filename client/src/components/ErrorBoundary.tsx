/**
 * ErrorBoundary - Catches React Rendering Errors
 *
 * Error Boundaries are React components that catch JavaScript errors
 * anywhere in their child component tree, log those errors, and display
 * a fallback UI instead of crashing the whole app.
 *
 * KEY CONCEPTS:
 *
 * 1. Must be a CLASS component (hooks don't support error boundaries yet)
 *    - getDerivedStateFromError() - Update state to show fallback UI
 *    - componentDidCatch() - Log error info (analytics, etc.)
 *
 * 2. What they catch:
 *    - Errors during rendering
 *    - Errors in lifecycle methods
 *    - Errors in constructors
 *
 * 3. What they DON'T catch:
 *    - Event handlers (use try/catch)
 *    - Async code (promises, setTimeout)
 *    - Server-side rendering errors
 *    - Errors in the error boundary itself
 *
 * @see https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary
 */

import { Component } from 'react';
import type { ReactNode, ErrorInfo } from 'react';

interface ErrorBoundaryProps {
  children: ReactNode;
  /** Optional custom fallback UI */
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  /**
   * getDerivedStateFromError - Called when a child throws an error
   *
   * This is a static lifecycle method that:
   * - Receives the error that was thrown
   * - Returns a new state object
   * - Triggers a re-render with the fallback UI
   *
   * This runs during the "render" phase, so side effects are not allowed.
   */
  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  /**
   * componentDidCatch - Called after an error has been thrown
   *
   * This runs during the "commit" phase, so side effects ARE allowed.
   * Use this for:
   * - Logging errors to an error reporting service
   * - Analytics tracking
   *
   * @param error - The error that was thrown
   * @param errorInfo - Object with componentStack trace
   */
  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Log to console in development
    console.error('ErrorBoundary caught an error:', error);
    console.error('Component stack:', errorInfo.componentStack);

    // TODO: In production, send to error tracking service
    // Example: Sentry.captureException(error, { extra: errorInfo });
  }

  /**
   * Reset the error boundary state
   *
   * Called when user clicks "Try Again" button.
   * This clears the error and attempts to re-render children.
   */
  handleReset = (): void => {
    this.setState({ hasError: false, error: null });
  };

  render(): ReactNode {
    if (this.state.hasError) {
      // If custom fallback provided, use it
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // Default fallback UI - Graphic Novel style
      return (
        <div className="min-h-[400px] flex items-center justify-center p-6">
          <div className="max-w-md w-full bg-danger-bg border-comic-heavy shadow-comic-lg p-6 text-center">
            <div className="text-danger text-5xl mb-4">⚠️</div>
            <h2 className="text-display text-2xl text-ink mb-2">
              Something went wrong
            </h2>
            <p className="text-danger text-sm mb-4">
              An unexpected error occurred while rendering this page.
            </p>
            {this.state.error && (
              <p className="text-danger text-xs font-mono mb-4 p-3 bg-paper border-comic overflow-auto">
                {this.state.error.message}
              </p>
            )}
            <button
              onClick={this.handleReset}
              className="px-6 py-3 bg-danger text-paper border-comic shadow-comic text-display tracking-wide shadow-comic-interactive focus:outline-none focus:ring-2 focus:ring-ink focus:ring-offset-2"
            >
              Try Again
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
