/**
 * Test Route Error - /test/route-error
 *
 * This route has a loader that always throws.
 * Used to test that RouteErrorComponent displays correctly.
 */

import { createFileRoute } from '@tanstack/react-router';
import { RouteErrorComponent } from '../../components/RouteErrorComponent';

export const Route = createFileRoute('/test/route-error')({
  loader: async () => {
    // Simulate a failed API call
    throw new Error('Test route error! RouteErrorComponent should catch this.');
  },
  errorComponent: RouteErrorComponent,
  component: () => <div>You should never see this</div>
});
