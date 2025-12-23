/**
 * Test Routes - /test
 *
 * Test page for error handling components.
 * Useful for verifying ErrorBoundary and RouteErrorComponent work correctly.
 */

import { useState } from 'react';
import { createFileRoute, Link } from '@tanstack/react-router';

export const Route = createFileRoute('/test/')({
  component: TestPage
});

function TestPage() {
  const [shouldBreak, setShouldBreak] = useState(false);

  // Throw during render - ErrorBoundary catches this
  if (shouldBreak) {
    throw new Error('Test rendering error! ErrorBoundary should catch this.');
  }

  return (
    <div className="p-6 max-w-md mx-auto">
      <h1 className="text-display text-4xl text-ink mb-4">
        Error Handling Test
      </h1>
      <p className="text-ink-soft mb-6">
        Click the buttons below to test error handling.
      </p>

      <div className="space-y-6">
        {/* Test ErrorBoundary */}
        <div className="p-4 bg-paper border-comic shadow-comic">
          <h2 className="text-display text-lg text-ink mb-2">
            1. ErrorBoundary Test
          </h2>
          <p className="text-sm text-ink-soft mb-3">
            Throws during render. ErrorBoundary catches it.
          </p>
          <button
            onClick={() => setShouldBreak(true)}
            className="px-6 py-3 bg-danger text-paper border-comic shadow-comic text-display tracking-wide shadow-comic-interactive"
          >
            Trigger Rendering Error
          </button>
        </div>

        {/* Test RouteErrorComponent */}
        <div className="p-4 bg-paper border-comic shadow-comic">
          <h2 className="text-display text-lg text-ink mb-2">
            2. Route Error Test
          </h2>
          <p className="text-sm text-ink-soft mb-3">
            Navigate to a route with a failing loader.
          </p>
          <Link
            to="/test/route-error"
            className="inline-block px-6 py-3 bg-amber-vivid text-ink border-comic shadow-comic text-display tracking-wide shadow-comic-interactive"
          >
            Go to Broken Route
          </Link>
        </div>
      </div>

      <p className="mt-6 text-xs text-ink-light">
        See src/routes/test/ for implementation details.
      </p>
    </div>
  );
}
