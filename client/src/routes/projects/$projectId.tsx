/**
 * Project Detail Route - /projects/:projectId
 *
 * Same pattern as tasks/$taskId.tsx:
 * - File: $projectId.tsx → Parameter: projectId
 * - URL: /projects/123 → params.projectId = "123"
 */

import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage,
})

function ProjectDetailPage() {
  // Extract the projectId parameter from the URL
  const { projectId } = Route.useParams()

  return (
    <div className="p-6">
      <div className="mb-4">
        <span className="text-sm text-gray-500">Project ID:</span>
        <span className="ml-2 font-mono bg-gray-100 px-2 py-1 rounded">
          {projectId}
        </span>
      </div>

      <h1 className="text-2xl font-bold text-gray-800 mb-4">Project Details</h1>

      <p className="text-gray-600 mb-6">
        This page will display details for project #{projectId}. In Session 03,
        we'll fetch this project and its tasks from the backend API.
      </p>

      {/* Placeholder content */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 mb-6">
        <h2 className="font-semibold text-lg text-gray-800 mb-2">
          Sample Project
        </h2>
        <p className="text-gray-600 mb-4">
          Project description will go here. This is placeholder content.
        </p>
        <div className="flex gap-2 mb-4">
          <span className="px-2 py-1 bg-green-100 text-green-800 text-sm rounded">
            Active
          </span>
        </div>
      </div>

      {/* Placeholder task list for this project */}
      <div>
        <h3 className="font-semibold text-gray-800 mb-3">Project Tasks</h3>
        <div className="space-y-2">
          <div className="bg-gray-50 p-3 rounded border border-gray-200">
            <p className="text-sm text-gray-700">Task 1 for this project</p>
          </div>
          <div className="bg-gray-50 p-3 rounded border border-gray-200">
            <p className="text-sm text-gray-700">Task 2 for this project</p>
          </div>
        </div>
      </div>
    </div>
  )
}
