/**
 * Home Page Route - /
 *
 * This is the index route (home page):
 * - File location: src/routes/index.tsx
 * - URL path: / (root)
 *
 * The filename "index.tsx" is special - it represents the root of its parent.
 * In this case, it's directly in routes/, so it maps to "/"
 */

import { createFileRoute, Link } from '@tanstack/react-router';

export const Route = createFileRoute('/')({
  component: HomePage
});

function HomePage() {
  return (
    <div className="p-6">
      <div className="max-w-4xl mx-auto">
        {/* Hero Section */}
        <div className="text-center py-12">
          <h1 className="text-display text-6xl text-ink mb-4">
            Task Manager
          </h1>
          <p className="text-ink-soft text-lg mb-8">
            Your fullstack task management application
          </p>

          {/* Quick navigation cards */}
          <div className="grid gap-6 md:grid-cols-2 max-w-2xl mx-auto">
            {/*
             * <Link> with params example
             *
             * For routes without parameters, just use `to="/path"`
             * For routes WITH parameters, you'd add `params={{ taskId: '123' }}`
             */}
            <Link
              to="/tasks"
              className="block p-6 bg-paper border-comic shadow-comic shadow-comic-interactive text-left"
            >
              <h2 className="text-display text-2xl text-ink mb-2">
                Tasks
              </h2>
              <p className="text-ink-soft">View and manage your tasks</p>
            </Link>

            <Link
              to="/projects"
              className="block p-6 bg-paper border-comic shadow-comic shadow-comic-interactive text-left"
            >
              <h2 className="text-display text-2xl text-ink mb-2">
                Projects
              </h2>
              <p className="text-ink-soft">Organize tasks into projects</p>
            </Link>
          </div>
        </div>

        {/* Session info */}
        <div className="mt-12 p-4 bg-amber-light border-comic shadow-comic-sm">
          <h3 className="text-display text-lg text-ink mb-2">
            Learning Project
          </h3>
          <p className="text-ink-soft text-sm">
            This is a fullstack tutorial demonstrating React + Spring Boot
            with modern patterns and best practices.
          </p>
        </div>
      </div>
    </div>
  );
}
