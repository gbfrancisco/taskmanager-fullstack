/**
 * API Types - TypeScript types matching backend DTOs
 *
 * These types mirror the Java DTOs from the Spring Boot backend.
 * Keeping frontend and backend types aligned ensures type safety
 * across the entire application.
 *
 * IMPORTANT: When the backend DTO changes, update these types too!
 *
 * @see server/src/main/java/com/tutorial/taskmanager/dto/
 */

// =============================================================================
// ENUMS
// =============================================================================

/**
 * TaskStatus - Matches backend TaskStatus enum
 *
 * Represents the lifecycle states of a task.
 * The backend stores these as strings (EnumType.STRING), so we use
 * string literal union types to match.
 */
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

/**
 * ProjectStatus - Matches backend ProjectStatus enum
 *
 * Represents the lifecycle states of a project.
 */
export type ProjectStatus =
  | 'PLANNING'
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED'

// =============================================================================
// USER TYPES
// =============================================================================

/**
 * User - Matches AppUserResponseDto
 *
 * Note: Password is NEVER included in response DTOs for security.
 * The backend calls this "AppUser" to avoid SQL reserved word "User".
 */
export interface User {
  id: number
  username: string
  email: string
}

/**
 * UserCreateInput - Matches AppUserCreateDto
 *
 * Used when creating a new user.
 */
export interface UserCreateInput {
  username: string
  email: string
  password: string
}

/**
 * UserUpdateInput - Matches AppUserUpdateDto
 *
 * Used when updating an existing user.
 * Note: Username cannot be updated (enforced by backend).
 */
export interface UserUpdateInput {
  email?: string
  password?: string
}

// =============================================================================
// TASK TYPES
// =============================================================================

/**
 * Task - Matches TaskResponseDto
 *
 * Notice we use IDs for relationships (appUserId, projectId) instead
 * of nested objects. This prevents circular references and matches
 * the backend's approach.
 *
 * The dueDate comes from the backend as an ISO string (LocalDateTime).
 */
export interface Task {
  id: number
  title: string
  description: string | null
  status: TaskStatus
  dueDate: string | null // ISO date string (e.g., "2024-12-31T23:59:59")
  appUserId: number
  projectId: number | null // null if not assigned to a project
}

/**
 * TaskCreateInput - Matches TaskCreateDto
 *
 * Used when creating a new task.
 */
export interface TaskCreateInput {
  title: string
  description?: string
  status?: TaskStatus
  dueDate?: string
  appUserId: number
  projectId?: number
}

/**
 * TaskUpdateInput - Matches TaskUpdateDto
 *
 * Used when updating an existing task.
 * All fields are optional - only provided fields are updated.
 */
export interface TaskUpdateInput {
  title?: string
  description?: string
  status?: TaskStatus
  dueDate?: string
}

// =============================================================================
// PROJECT TYPES
// =============================================================================

/**
 * Project - Matches ProjectResponseDto
 *
 * Uses appUserId to reference the project owner instead of
 * a nested User object.
 */
export interface Project {
  id: number
  name: string
  description: string | null
  status: ProjectStatus
  appUserId: number
}

/**
 * ProjectCreateInput - Matches ProjectCreateDto
 *
 * Used when creating a new project.
 */
export interface ProjectCreateInput {
  name: string
  description?: string
  status?: ProjectStatus
  appUserId: number
}

/**
 * ProjectUpdateInput - Matches ProjectUpdateDto
 *
 * Used when updating an existing project.
 * All fields are optional - only provided fields are updated.
 */
export interface ProjectUpdateInput {
  name?: string
  description?: string
  status?: ProjectStatus
}

// =============================================================================
// API RESPONSE TYPES
// =============================================================================

/**
 * ErrorResponse - Matches backend GlobalExceptionHandler format
 *
 * The backend returns this structure for all errors.
 */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}
