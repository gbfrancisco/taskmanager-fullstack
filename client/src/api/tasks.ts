/**
 * Tasks API - Functions for task-related API calls
 *
 * This module provides typed functions for all task operations.
 * Each function corresponds to a backend endpoint.
 *
 * PATTERN: API modules export functions that:
 * 1. Call the API client with the correct endpoint
 * 2. Return typed data matching our TypeScript types
 * 3. Can be used directly in TanStack Query hooks
 *
 * @see server/src/main/java/com/tutorial/taskmanager/controller/TaskController.java
 */

import { get, post, put, del } from './client';
import type {
  Task,
  TaskCreateInput,
  TaskUpdateInput,
  TaskStatus
} from '../types/api';

// =============================================================================
// QUERY FUNCTIONS (GET requests - used with useQuery)
// =============================================================================

/**
 * Fetch all tasks
 *
 * GET /api/tasks
 *
 * @returns Array of all tasks
 *
 * @example
 * // In a component with TanStack Query:
 * const { data: tasks } = useQuery({
 *   queryKey: ['tasks'],
 *   queryFn: fetchTasks,
 * })
 */
export function fetchTasks(): Promise<Task[]> {
  return get<Task[]>('/api/tasks');
}

/**
 * Fetch a single task by ID
 *
 * GET /api/tasks/:id
 *
 * @param id - The task ID
 * @returns The task data
 * @throws ApiClientError with status 404 if task not found
 *
 * @example
 * const { data: task } = useQuery({
 *   queryKey: ['tasks', taskId],
 *   queryFn: () => fetchTaskById(taskId),
 * })
 */
export function fetchTaskById(id: number): Promise<Task> {
  return get<Task>(`/api/tasks/${id}`);
}

/**
 * Fetch tasks by user ID
 *
 * @deprecated No longer needed - backend automatically returns authenticated user's tasks.
 * Use fetchTasks() instead.
 */
export function fetchTasksByUserId(_userId: number): Promise<Task[]> {
  // Backend now filters by authenticated user from JWT token
  return get<Task[]>('/api/tasks');
}

/**
 * Fetch tasks by project ID
 *
 * GET /api/tasks?projectId=:projectId
 *
 * @param projectId - The project ID to filter by
 * @returns Array of tasks in the project
 */
export function fetchTasksByProjectId(projectId: number): Promise<Task[]> {
  return get<Task[]>(`/api/tasks?projectId=${projectId}`);
}

/**
 * Fetch tasks by status
 *
 * GET /api/tasks?status=:status
 *
 * @param status - The status to filter by
 * @returns Array of tasks with the given status
 */
export function fetchTasksByStatus(status: TaskStatus): Promise<Task[]> {
  return get<Task[]>(`/api/tasks?status=${status}`);
}

/**
 * Fetch overdue tasks for the authenticated user
 *
 * GET /api/tasks?overdue=true
 *
 * @returns Array of overdue tasks for the authenticated user
 */
export function fetchOverdueTasks(): Promise<Task[]> {
  // Backend filters by authenticated user from JWT token
  return get<Task[]>('/api/tasks?overdue=true');
}

// =============================================================================
// MUTATION FUNCTIONS (POST/PUT/DELETE - used with useMutation)
// =============================================================================

/**
 * Create a new task
 *
 * POST /api/tasks
 *
 * @param input - The task data to create
 * @returns The created task with its new ID
 *
 * @example
 * const mutation = useMutation({
 *   mutationFn: createTask,
 *   onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
 * })
 */
export function createTask(input: TaskCreateInput): Promise<Task> {
  return post<Task>('/api/tasks', input);
}

/**
 * Update an existing task
 *
 * PUT /api/tasks/:id
 *
 * @param id - The task ID to update
 * @param input - The fields to update (partial update)
 * @returns The updated task
 */
export function updateTask(id: number, input: TaskUpdateInput): Promise<Task> {
  return put<Task>(`/api/tasks/${id}`, input);
}

/**
 * Delete a task
 *
 * DELETE /api/tasks/:id
 *
 * @param id - The task ID to delete
 */
export function deleteTask(id: number): Promise<void> {
  return del(`/api/tasks/${id}`);
}

/**
 * Assign a task to a project
 *
 * PUT /api/tasks/:taskId/project/:projectId
 *
 * @param taskId - The task ID
 * @param projectId - The project ID to assign to
 * @returns The updated task
 */
export function assignTaskToProject(
  taskId: number,
  projectId: number
): Promise<Task> {
  return put<Task>(`/api/tasks/${taskId}/project/${projectId}`, {});
}

/**
 * Remove a task from its project
 *
 * DELETE /api/tasks/:taskId/project
 *
 * @param taskId - The task ID
 * @returns The updated task (with projectId: null)
 */
export function removeTaskFromProject(taskId: number): Promise<Task> {
  return del(`/api/tasks/${taskId}/project`) as unknown as Promise<Task>;
}

// =============================================================================
// QUERY KEY FACTORY
// =============================================================================

/**
 * Query Keys for Tasks
 *
 * TanStack Query uses "query keys" to identify and cache queries.
 * Using a factory pattern ensures consistent key structure across the app.
 *
 * WHY QUERY KEYS MATTER:
 * - Keys are used to cache and deduplicate requests
 * - Changing a key invalidates the cache for that query
 * - Related queries should share a common prefix for easy invalidation
 *
 * @example
 * // Invalidate all task-related queries
 * queryClient.invalidateQueries({ queryKey: taskKeys.all })
 *
 * // Invalidate just the lists
 * queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
 *
 * // Invalidate a specific task
 * queryClient.invalidateQueries({ queryKey: taskKeys.detail(1) })
 */
export const taskKeys = {
  // Base key for all task queries
  all: ['tasks'] as const,

  // Keys for list queries (all lists share this prefix)
  lists: () => [...taskKeys.all, 'list'] as const,

  // Key for the default list (all tasks)
  list: () => [...taskKeys.lists()] as const,

  // Key for filtered lists
  listByUser: (userId: number) => [...taskKeys.lists(), { userId }] as const,
  listByProject: (projectId: number) =>
    [...taskKeys.lists(), { projectId }] as const,
  listByStatus: (status: TaskStatus) =>
    [...taskKeys.lists(), { status }] as const,
  listOverdue: (userId: number) =>
    [...taskKeys.lists(), { userId, overdue: true }] as const,

  // Keys for detail queries (single task)
  details: () => [...taskKeys.all, 'detail'] as const,
  detail: (id: number) => [...taskKeys.details(), id] as const
};
