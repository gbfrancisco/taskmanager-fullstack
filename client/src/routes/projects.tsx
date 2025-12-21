/**
 * Projects List Route - /projects
 *
 * This file follows the same pattern as tasks.tsx:
 * - File location: src/routes/projects.tsx
 * - URL path: /projects
 *
 * Same principles apply - file name = URL path, no configuration needed.
 */

import { createFileRoute, Link } from '@tanstack/react-router'

export const Route = createFileRoute('/projects')({
  component: ProjectsPage,
})

function ProjectsPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-800 mb-4">Projects</h1>
      <p className="text-gray-600 mb-6">
        This page will display your projects. Data fetching coming in Session
        03!
      </p>

      {/* Placeholder project cards - now with links to detail pages */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Link
          to="/projects/$projectId"
          params={{ projectId: '1' }}
          className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
        >
          <h3 className="font-semibold text-gray-800 mb-2">Project Alpha</h3>
          <p className="text-sm text-gray-500 mb-3">
            Sample project description
          </p>
          <div className="flex justify-between items-center">
            <span className="text-xs text-gray-400">5 tasks</span>
            <span className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded">
              Active
            </span>
          </div>
        </Link>
        <Link
          to="/projects/$projectId"
          params={{ projectId: '2' }}
          className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
        >
          <h3 className="font-semibold text-gray-800 mb-2">Project Beta</h3>
          <p className="text-sm text-gray-500 mb-3">Another sample project</p>
          <div className="flex justify-between items-center">
            <span className="text-xs text-gray-400">3 tasks</span>
            <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded">
              Planning
            </span>
          </div>
        </Link>
      </div>
    </div>
  )
}
