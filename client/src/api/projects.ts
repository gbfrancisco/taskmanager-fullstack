/**
 * Projects API - Functions for project-related API calls
 *
 * This module provides typed functions for all project operations.
 * Each function corresponds to a backend endpoint.
 *
 * @see server/src/main/java/com/tutorial/taskmanager/controller/ProjectController.java
 */

import { get, post, put, del } from './client'
import type {
  Project,
  ProjectCreateInput,
  ProjectUpdateInput,
  ProjectStatus,
} from '../types/api'

// =============================================================================
// QUERY FUNCTIONS (GET requests - used with useQuery)
// =============================================================================

/**
 * Fetch all projects
 *
 * GET /api/projects
 *
 * @returns Array of all projects
 */
export function fetchProjects(): Promise<Project[]> {
  return get<Project[]>('/api/projects')
}

/**
 * Fetch a single project by ID
 *
 * GET /api/projects/:id
 *
 * @param id - The project ID
 * @returns The project data
 * @throws ApiClientError with status 404 if project not found
 */
export function fetchProjectById(id: number): Promise<Project> {
  return get<Project>(`/api/projects/${id}`)
}

/**
 * Fetch projects by user ID
 *
 * GET /api/projects?userId=:userId
 *
 * @param userId - The user ID to filter by
 * @returns Array of projects owned by the user
 */
export function fetchProjectsByUserId(userId: number): Promise<Project[]> {
  return get<Project[]>(`/api/projects?userId=${userId}`)
}

/**
 * Fetch projects by status
 *
 * GET /api/projects?status=:status
 *
 * @param status - The status to filter by
 * @returns Array of projects with the given status
 */
export function fetchProjectsByStatus(status: ProjectStatus): Promise<Project[]> {
  return get<Project[]>(`/api/projects?status=${status}`)
}

/**
 * Search projects by name
 *
 * GET /api/projects?name=:name
 *
 * The backend performs a case-insensitive partial match.
 *
 * @param name - The name to search for
 * @returns Array of projects matching the name
 */
export function searchProjectsByName(name: string): Promise<Project[]> {
  return get<Project[]>(`/api/projects?name=${encodeURIComponent(name)}`)
}

// =============================================================================
// MUTATION FUNCTIONS (POST/PUT/DELETE - used with useMutation)
// =============================================================================

/**
 * Create a new project
 *
 * POST /api/projects
 *
 * @param input - The project data to create
 * @returns The created project with its new ID
 */
export function createProject(input: ProjectCreateInput): Promise<Project> {
  return post<Project>('/api/projects', input)
}

/**
 * Update an existing project
 *
 * PUT /api/projects/:id
 *
 * @param id - The project ID to update
 * @param input - The fields to update (partial update)
 * @returns The updated project
 */
export function updateProject(
  id: number,
  input: ProjectUpdateInput,
): Promise<Project> {
  return put<Project>(`/api/projects/${id}`, input)
}

/**
 * Delete a project
 *
 * DELETE /api/projects/:id
 *
 * @param id - The project ID to delete
 */
export function deleteProject(id: number): Promise<void> {
  return del(`/api/projects/${id}`)
}

// =============================================================================
// QUERY KEY FACTORY
// =============================================================================

/**
 * Query Keys for Projects
 *
 * Same pattern as taskKeys - provides consistent key structure
 * for caching and invalidation.
 */
export const projectKeys = {
  // Base key for all project queries
  all: ['projects'] as const,

  // Keys for list queries
  lists: () => [...projectKeys.all, 'list'] as const,
  list: () => [...projectKeys.lists()] as const,

  // Keys for filtered lists
  listByUser: (userId: number) => [...projectKeys.lists(), { userId }] as const,
  listByStatus: (status: ProjectStatus) =>
    [...projectKeys.lists(), { status }] as const,
  search: (name: string) => [...projectKeys.lists(), { name }] as const,

  // Keys for detail queries
  details: () => [...projectKeys.all, 'detail'] as const,
  detail: (id: number) => [...projectKeys.details(), id] as const,
}
