/**
 * Tasks List Route - /tasks
 *
 * This file demonstrates TanStack Router's file-based routing:
 * - File location: src/routes/tasks.tsx
 * - URL path: /tasks
 *
 * The file name directly maps to the URL path. No configuration needed!
 */

import { createFileRoute, Link } from '@tanstack/react-router'

/**
 * createFileRoute() - The core API for defining routes
 *
 * The string argument '/tasks' MUST match the file path.
 * TanStack Router validates this at build time - if they don't match,
 * you'll get a TypeScript error.
 *
 * The function returns a route configuration object where you can define:
 * - component: The React component to render
 * - loader: Data fetching before render (covered in Session 03)
 * - errorComponent: What to show on errors
 * - pendingComponent: What to show while loading
 */
export const Route = createFileRoute('/tasks')({
  component: TasksPage,
})

/**
 * TasksPage Component
 *
 * This is a simple placeholder for now. In Session 03, we'll:
 * - Fetch real tasks from the backend API
 * - Display them in a list
 * - Add loading and error states
 */
function TasksPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-800 mb-4">Tasks</h1>
      <p className="text-gray-600 mb-6">
        This page will display your task list. Data fetching coming in Session
        03!
      </p>

      {/* Placeholder task cards - will be replaced with real data */}
      <div className="space-y-3">
        {/*
         * Linking to dynamic routes
         *
         * For routes with parameters like /tasks/$taskId, you must provide:
         * - to: The route path pattern (with $ prefix)
         * - params: An object with the parameter values
         *
         * TypeScript enforces this! If you forget params, you get an error.
         */}
        <Link
          to="/tasks/$taskId"
          params={{ taskId: '1' }}
          className="block bg-white p-4 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
        >
          <p className="font-medium text-gray-800">Sample Task 1</p>
          <p className="text-sm text-gray-500">Click to view details</p>
        </Link>
        <Link
          to="/tasks/$taskId"
          params={{ taskId: '2' }}
          className="block bg-white p-4 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
        >
          <p className="font-medium text-gray-800">Sample Task 2</p>
          <p className="text-sm text-gray-500">Click to view details</p>
        </Link>
      </div>
    </div>
  )
}
