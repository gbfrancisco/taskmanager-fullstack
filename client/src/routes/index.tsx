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

import { createFileRoute, Link } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: HomePage,
})

function HomePage() {
  return (
    <div className="p-6">
      <div className="max-w-4xl mx-auto">
        <div className="text-center py-12">
          <h1 className="text-4xl font-bold text-gray-800 mb-4">
            Welcome to Task Manager
          </h1>
          <p className="text-gray-600 text-lg mb-8">
            Your fullstack task management application
          </p>

          {/* Quick navigation cards */}
          <div className="grid gap-4 md:grid-cols-2 max-w-2xl mx-auto">
            {/*
             * <Link> with params example
             *
             * For routes without parameters, just use `to="/path"`
             * For routes WITH parameters, you'd add `params={{ taskId: '123' }}`
             */}
            <Link
              to="/tasks"
              className="block p-6 bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
            >
              <h2 className="text-xl font-semibold text-gray-800 mb-2">
                Tasks
              </h2>
              <p className="text-gray-600">View and manage your tasks</p>
            </Link>

            <Link
              to="/projects"
              className="block p-6 bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
            >
              <h2 className="text-xl font-semibold text-gray-800 mb-2">
                Projects
              </h2>
              <p className="text-gray-600">Organize tasks into projects</p>
            </Link>
          </div>
        </div>

        {/* Session info */}
        <div className="mt-12 p-4 bg-blue-50 rounded-lg border border-blue-100">
          <h3 className="font-semibold text-blue-800 mb-2">
            Session 02 Complete!
          </h3>
          <p className="text-blue-700 text-sm">
            You now have working routes with navigation. Next up: fetching real
            data from the backend API using TanStack Query.
          </p>
        </div>
      </div>
    </div>
  )
}
