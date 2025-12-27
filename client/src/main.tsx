import { StrictMode } from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider, createRouter } from '@tanstack/react-router';

import * as TanStackQueryProvider from './integrations/tanstack-query/root-provider';
import { AuthProvider, useAuth } from './contexts/AuthContext';

// Import the generated route tree
import { routeTree } from './routeTree.gen';

import './styles.css';

// =============================================================================
// ROUTER SETUP
// =============================================================================

/**
 * Get the TanStack Query context (QueryClient).
 * This is created at module level since it doesn't depend on React state.
 */
const TanStackQueryProviderContext = TanStackQueryProvider.getContext();

/**
 * Create a router instance with a placeholder auth context.
 *
 * WHY THIS PATTERN?
 * - TanStack Router needs the router instance for type registration
 * - Type registration must happen at module level (before React renders)
 * - But auth context isn't available until inside the React tree
 *
 * SOLUTION:
 * - Create router with empty auth placeholder here (for types)
 * - Update the context with real auth in InnerApp component
 *
 * The `context` passed here is the INITIAL context. We override it
 * in the InnerApp component with the actual auth state.
 */
const router = createRouter({
  routeTree,
  context: {
    ...TanStackQueryProviderContext,
    // Auth will be provided by InnerApp - this is just for type inference
    auth: undefined!
  },
  defaultPreload: 'intent',
  scrollRestoration: true,
  defaultStructuralSharing: true,
  defaultPreloadStaleTime: 0
});

// Register the router instance for type safety
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

// =============================================================================
// INNER APP COMPONENT
// =============================================================================

/**
 * InnerApp - Connects auth context to the router
 *
 * This component exists because:
 * 1. useAuth() can only be called inside AuthProvider
 * 2. The router needs auth context for route guards (beforeLoad)
 * 3. We pass the live auth state to RouterProvider
 *
 * The router's context is updated on every render with current auth state.
 */
function InnerApp() {
  const auth = useAuth();

  return (
    <RouterProvider
      router={router}
      context={{
        ...TanStackQueryProviderContext,
        auth
      }}
    />
  );
}

// =============================================================================
// RENDER
// =============================================================================

/**
 * Application Entry Point
 *
 * Provider hierarchy (outer to inner):
 * 1. StrictMode - React development checks
 * 2. TanStackQueryProvider - Data fetching (QueryClient) - outermost so other providers can use useQuery/useMutation
 * 3. AuthProvider - Authentication state (may use useMutation for real API auth in future)
 * 4. InnerApp - Connects auth to router and renders RouterProvider
 *
 * IMPORTANT: QueryProvider must be outside AuthProvider because:
 * - When we add real backend auth, AuthProvider will use useMutation for login/register
 * - Inner providers can use outer provider contexts, but not vice versa
 *
 * Note: AuthProvider must be outside InnerApp so useAuth() works there.
 */
const rootElement = document.getElementById('app');
if (rootElement && !rootElement.innerHTML) {
  const root = ReactDOM.createRoot(rootElement);
  root.render(
    <StrictMode>
      <TanStackQueryProvider.Provider {...TanStackQueryProviderContext}>
        <AuthProvider>
          <InnerApp />
        </AuthProvider>
      </TanStackQueryProvider.Provider>
    </StrictMode>
  );
}
