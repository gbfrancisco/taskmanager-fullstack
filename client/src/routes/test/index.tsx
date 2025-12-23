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
      <h1 className="text-2xl font-bold text-gray-800 mb-4">
        Error Handling Test
      </h1>
      <p className="text-gray-600 mb-6">
        Click the buttons below to test error handling.
      </p>

      <div className="space-y-4">
        {/* Test ErrorBoundary */}
        <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
          <h2 className="font-semibold text-gray-700 mb-2">
            1. ErrorBoundary Test
          </h2>
          <p className="text-sm text-gray-500 mb-3">
            Throws during render. ErrorBoundary catches it.
          </p>
          <button
            onClick={() => setShouldBreak(true)}
            className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
          >
            Trigger Rendering Error
          </button>
        </div>

        {/* Test RouteErrorComponent */}
        <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
          <h2 className="font-semibold text-gray-700 mb-2">
            2. Route Error Test
          </h2>
          <p className="text-sm text-gray-500 mb-3">
            Navigate to a route with a failing loader.
          </p>
          <Link
            to="/test/route-error"
            className="inline-block px-4 py-2 bg-orange-600 text-white rounded-md hover:bg-orange-700"
          >
            Go to Broken Route
          </Link>
        </div>
      </div>

      <p className="mt-6 text-xs text-gray-400">
        See src/routes/test/ for implementation details.
      </p>
    </div>
  );
}
