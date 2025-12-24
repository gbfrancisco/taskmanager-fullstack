/**
 * Root Layout - __root.tsx
 *
 * This is the ROOT of all routes in TanStack Router.
 * Every single page in your app is a "child" of this root layout.
 *
 * KEY CONCEPTS:
 *
 * 1. __root.tsx is SPECIAL
 *    - The double underscore prefix marks this as the root
 *    - It ALWAYS renders, regardless of which route is active
 *    - Perfect for app-wide UI like headers, footers, sidebars
 *
 * 2. <Outlet /> is where child routes render
 *    - Similar to Vue's <router-view> or React Router's <Outlet>
 *    - When you navigate to /tasks, the Tasks component renders HERE
 *    - The root layout stays in place, only the Outlet content changes
 *
 * 3. Context sharing with createRootRouteWithContext
 *    - We pass QueryClient through context so all routes can access it
 *    - This enables data fetching in route loaders (Session 03)
 */

import { Outlet, createRootRouteWithContext } from '@tanstack/react-router';
import { TanStackRouterDevtoolsPanel } from '@tanstack/react-router-devtools';
import { TanStackDevtools } from '@tanstack/react-devtools';

import TanStackQueryDevtools from '@/integrations/tanstack-query/devtools';
import { Header } from '@/components/Header';
import { Footer } from '@/components/Footer';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { RouteErrorComponent } from '@/components/RouteErrorComponent';

import type { QueryClient } from '@tanstack/react-query';

/**
 * Router Context Type
 *
 * This interface defines what data is available to ALL routes.
 * Currently just QueryClient, but you could add:
 * - Authentication state
 * - User preferences
 * - Feature flags
 */
interface MyRouterContext {
  queryClient: QueryClient;
}

/**
 * Root Route Definition
 *
 * createRootRouteWithContext<T>() creates a root route that:
 * 1. Accepts typed context (MyRouterContext)
 * 2. Makes that context available to all child routes
 * 3. Renders the root layout component
 *
 * errorComponent:
 * - Displayed when any child route fails to load
 * - Can be overridden per-route if needed
 * - Receives error info and reset function
 */
export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: RootLayout,
  errorComponent: RouteErrorComponent
});

/**
 * RootLayout Component
 *
 * This is the shell of your entire application.
 * Structure: Header (always visible) + Outlet (changes per route)
 */
function RootLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-halftone">
      {/* Header is OUTSIDE the Outlet - it never changes */}
      <Header />

      {/* Main content area */}
      <main className="flex-1">
        {/*
         * ErrorBoundary wraps the Outlet to catch rendering errors.
         * If any child route component throws during render,
         * the ErrorBoundary catches it and shows a fallback UI.
         *
         * <Outlet /> - The magic component
         *
         * This is where child routes render their content:
         * - Navigate to "/" → HomePage renders here
         * - Navigate to "/tasks" → TasksPage renders here
         * - Navigate to "/tasks/123" → TaskDetailPage renders here
         *
         * The Header stays in place, only this Outlet swaps content.
         */}
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>

      {/* Footer is OUTSIDE the Outlet - it never changes */}
      <Footer />

      {/* Development tools - only visible in dev mode */}
      <TanStackDevtools
        config={{
          position: 'bottom-right'
        }}
        plugins={[
          {
            name: 'Tanstack Router',
            render: <TanStackRouterDevtoolsPanel />
          },
          TanStackQueryDevtools
        ]}
      />
    </div>
  );
}
