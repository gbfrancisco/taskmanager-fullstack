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

import type { QueryClient } from '@tanstack/react-query';
import type { AuthContextType } from '@/contexts/AuthContext';

/**
 * Router Context Type
 *
 * This interface defines what data is available to ALL routes.
 * - queryClient: For data fetching with TanStack Query
 * - auth: Authentication state and methods (login, logout, etc.)
 *
 * Routes can access this context in:
 * - `beforeLoad`: For route guards (redirect if not authenticated)
 * - `loader`: For data fetching with user context
 * - Components: Via route hooks
 */
interface MyRouterContext {
  queryClient: QueryClient;
  auth: AuthContextType;
}

/**
 * Root Route Definition
 *
 * createRootRouteWithContext<T>() creates a root route that:
 * 1. Accepts typed context (MyRouterContext)
 * 2. Makes that context available to all child routes
 * 3. Renders the root layout component
 *
 * Note: errorComponent is set on individual child routes (not here)
 * so the header/footer remain visible during errors.
 */
export const Route = createRootRouteWithContext<MyRouterContext>()({
  component: RootLayout
});

/**
 * RootLayout Component
 *
 * This is the shell of your entire application.
 * Structure: Header (always visible) + Outlet (changes per route)
 */
function RootLayout() {
  return (
    <div className="min-h-dvh flex flex-col bg-halftone">
      {/* Header is OUTSIDE the Outlet - it never changes */}
      <Header />

      {/* Main content area */}
      <main className="flex-1">
        {/*
         * <Outlet /> - The magic component
         *
         * This is where child routes render their content:
         * - Navigate to "/" → HomePage renders here
         * - Navigate to "/tasks" → TasksPage renders here
         * - Navigate to "/tasks/123" → TaskDetailPage renders here
         *
         * The Header stays in place, only this Outlet swaps content.
         *
         * Error handling is provided by TanStack Router's errorComponent
         * (RouteErrorComponent) which catches both loader and render errors
         * and properly resets on navigation.
         */}
        <Outlet />
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
