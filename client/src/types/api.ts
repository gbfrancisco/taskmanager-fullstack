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
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

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
  | 'CANCELLED';

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
  id: number;
  username: string;
  email: string;
}

/**
 * UserSummary - Matches AppUserSummaryDto
 *
 * A lightweight user representation used when embedding user info
 * in other responses (e.g., in TaskResponseDto, ProjectResponseDto).
 *
 * Contains only essential fields for display purposes.
 */
export interface UserSummary {
  id: number;
  username: string;
}

/**
 * UserCreateInput - Matches AppUserCreateDto
 *
 * Used when creating a new user.
 */
export interface UserCreateInput {
  username: string;
  email: string;
  password: string;
}

/**
 * UserUpdateInput - Matches AppUserUpdateDto
 *
 * Used when updating an existing user.
 * Note: Username cannot be updated (enforced by backend).
 */
export interface UserUpdateInput {
  email?: string;
  password?: string;
}

// =============================================================================
// TASK TYPES
// =============================================================================

/**
 * Task - Matches TaskResponseDto
 *
 * Relationships are embedded as summary DTOs for convenience:
 * - appUser: UserSummary with id and username
 * - project: ProjectSummary with id, name, and status
 *
 * The dueDate comes from the backend as an ISO string (LocalDateTime).
 */
export interface Task {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  dueDate: string | null; // ISO date string (e.g., "2024-12-31T23:59:59")
  createdTimestamp: string;
  updatedTimestamp: string;
  appUser: UserSummary; // Embedded user summary (always present)
  project: ProjectSummary | null; // Embedded project summary (null if not assigned)
}

/**
 * TaskCreateInput - Matches TaskCreateDto
 *
 * Used when creating a new task.
 * Note: appUserId is NOT included - the backend extracts the user from the JWT token.
 */
export interface TaskCreateInput {
  title: string;
  description?: string;
  status?: TaskStatus;
  dueDate?: string;
  projectId?: number;
}

/**
 * TaskUpdateInput - Matches TaskUpdateDto
 *
 * Used when updating an existing task.
 * All fields are optional - only provided fields are updated.
 */
export interface TaskUpdateInput {
  title?: string;
  description?: string;
  status?: TaskStatus;
  dueDate?: string;
}

// =============================================================================
// PROJECT TYPES
// =============================================================================

/**
 * ProjectSummary - Matches ProjectSummaryDto
 *
 * A lightweight project representation used when embedding project info
 * in other responses (e.g., in TaskResponseDto).
 *
 * Contains only essential fields for display purposes.
 */
export interface ProjectSummary {
  id: number;
  name: string;
  status: ProjectStatus;
}

/**
 * Project - Matches ProjectResponseDto
 *
 * The owner relationship is embedded as a UserSummary for convenience.
 */
export interface Project {
  id: number;
  name: string;
  description: string | null;
  status: ProjectStatus;
  createdTimestamp: string;
  updatedTimestamp: string;
  appUser: UserSummary; // Embedded user summary (project owner)
  taskCount: number; // Computed field: number of tasks in this project
}

/**
 * ProjectCreateInput - Matches ProjectCreateDto
 *
 * Used when creating a new project.
 * Note: appUserId is NOT included - the backend extracts the user from the JWT token.
 */
export interface ProjectCreateInput {
  name: string;
  description?: string;
  status?: ProjectStatus;
}

/**
 * ProjectUpdateInput - Matches ProjectUpdateDto
 *
 * Used when updating an existing project.
 * All fields are optional - only provided fields are updated.
 */
export interface ProjectUpdateInput {
  name?: string;
  description?: string;
  status?: ProjectStatus;
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
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

// =============================================================================
// AUTHENTICATION TYPES
// =============================================================================

/**
 * LoginRequest - Matches backend LoginRequestDto
 *
 * Used for POST /api/auth/login.
 * Accepts either username or email for authentication.
 */
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

/**
 * RegisterRequest - Matches backend RegisterRequestDto
 *
 * Used for POST /api/auth/register
 */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

/**
 * AuthResponse - Matches backend AuthResponseDto
 *
 * Returned from both /api/auth/login and /api/auth/register.
 * Contains the JWT token and user info.
 */
export interface AuthResponse {
  token: string;
  tokenType: string; // "Bearer"
  expiresIn: number; // seconds until expiration
  user: User;
}
