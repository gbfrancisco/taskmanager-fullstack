/**
 * Task Detail Route - /tasks/:taskId
 *
 * This file demonstrates DYNAMIC ROUTES with TanStack Router:
 * - File location: src/routes/tasks/$taskId.tsx
 * - URL path: /tasks/:taskId (e.g., /tasks/1, /tasks/42, /tasks/abc)
 *
 * The $ prefix in the filename creates a dynamic segment:
 * - $taskId.tsx → :taskId parameter
 * - $userId.tsx → :userId parameter
 * - $slug.tsx → :slug parameter
 *
 * The parameter name comes from the filename (without the $ and .tsx)
 */

import { createFileRoute } from '@tanstack/react-router'

/**
 * Route Definition
 *
 * Notice the path includes '$taskId' - this MUST match the filename.
 * TanStack Router uses this to:
 * 1. Match URLs like /tasks/123
 * 2. Extract the parameter value (123)
 * 3. Provide type-safe access to the parameter
 */
export const Route = createFileRoute('/tasks/$taskId')({
  component: TaskDetailPage,
})

/**
 * TaskDetailPage Component
 *
 * This demonstrates how to access route parameters.
 */
function TaskDetailPage() {
  /**
   * Route.useParams() - Access URL parameters
   *
   * This hook returns an object with all parameters for this route.
   * Since our route is /tasks/$taskId, we get { taskId: string }
   *
   * KEY INSIGHT: The parameter is ALWAYS a string, even if it looks like a number.
   * If you need a number, you must parse it: parseInt(taskId, 10)
   *
   * This is fully type-safe! Try typing `params.` and you'll see
   * only `taskId` is available - TypeScript knows exactly what params exist.
   */
  const { taskId } = Route.useParams()

  return (
    <div className="p-6">
      {/* Display the extracted parameter */}
      <div className="mb-4">
        <span className="text-sm text-gray-500">Task ID:</span>
        <span className="ml-2 font-mono bg-gray-100 px-2 py-1 rounded">
          {taskId}
        </span>
      </div>

      <h1 className="text-2xl font-bold text-gray-800 mb-4">Task Details</h1>

      <p className="text-gray-600 mb-6">
        This page will display details for task #{taskId}. In Session 03, we'll
        fetch this task from the backend API using the taskId parameter.
      </p>

      {/* Placeholder content */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h2 className="font-semibold text-lg text-gray-800 mb-2">
          Sample Task Title
        </h2>
        <p className="text-gray-600 mb-4">
          This is placeholder content. Real task data coming soon!
        </p>
        <div className="flex gap-2">
          <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-sm rounded">
            In Progress
          </span>
          <span className="px-2 py-1 bg-blue-100 text-blue-800 text-sm rounded">
            High Priority
          </span>
        </div>
      </div>
    </div>
  )
}
