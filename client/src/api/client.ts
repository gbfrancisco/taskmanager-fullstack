/**
 * API Client - Fetch wrapper with error handling
 *
 * This module provides a centralized way to make HTTP requests to the backend.
 * It handles:
 * - Base URL configuration
 * - JSON serialization/deserialization
 * - Error handling with typed error responses
 * - Common headers (Content-Type, etc.)
 *
 * WHY A WRAPPER?
 * Instead of calling fetch() directly in every component, we use this wrapper to:
 * 1. Avoid repeating the base URL everywhere
 * 2. Automatically parse JSON responses
 * 3. Handle errors consistently
 * 4. Add common headers (and later, auth tokens)
 */

import type { ApiError } from '../types/api'

// =============================================================================
// CONFIGURATION
// =============================================================================

/**
 * Backend API base URL
 *
 * The Spring Boot server runs on port 8080 by default.
 * In production, this would come from environment variables.
 *
 * TIP: Vite uses import.meta.env for environment variables:
 *   const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
 */
const API_BASE_URL = 'http://localhost:8080'

// =============================================================================
// CUSTOM ERROR CLASS
// =============================================================================

/**
 * ApiClientError - Custom error class for API errors
 *
 * This extends the built-in Error class to include:
 * - HTTP status code
 * - Original response for debugging
 * - Typed error body from the backend
 *
 * WHY A CUSTOM ERROR?
 * Fetch doesn't throw on 4xx/5xx responses (only network errors).
 * We need a way to signal HTTP errors to calling code.
 */
export class ApiClientError extends Error {
  constructor(
    message: string,
    public status: number,
    public body?: ApiError,
  ) {
    super(message)
    this.name = 'ApiClientError'
  }
}

// =============================================================================
// HTTP METHODS
// =============================================================================

/**
 * GET request - Fetch a resource
 *
 * @param endpoint - The API endpoint (e.g., '/api/tasks')
 * @returns The parsed JSON response
 * @throws ApiClientError if the request fails
 *
 * @example
 * const tasks = await get<Task[]>('/api/tasks')
 * const task = await get<Task>('/api/tasks/1')
 */
export async function get<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  })

  return handleResponse<T>(response)
}

/**
 * POST request - Create a new resource
 *
 * @param endpoint - The API endpoint
 * @param data - The request body (will be JSON-serialized)
 * @returns The parsed JSON response
 * @throws ApiClientError if the request fails
 *
 * @example
 * const newTask = await post<Task>('/api/tasks', { title: 'New Task', appUserId: 1 })
 */
export async function post<T>(endpoint: string, data: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(data),
  })

  return handleResponse<T>(response)
}

/**
 * PUT request - Update an existing resource
 *
 * @param endpoint - The API endpoint
 * @param data - The request body (will be JSON-serialized)
 * @returns The parsed JSON response
 * @throws ApiClientError if the request fails
 *
 * @example
 * const updatedTask = await put<Task>('/api/tasks/1', { title: 'Updated Title' })
 */
export async function put<T>(endpoint: string, data: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(data),
  })

  return handleResponse<T>(response)
}

/**
 * DELETE request - Remove a resource
 *
 * Note: DELETE typically returns 204 No Content, so we don't parse a body.
 *
 * @param endpoint - The API endpoint
 * @throws ApiClientError if the request fails
 *
 * @example
 * await del('/api/tasks/1')
 */
export async function del(endpoint: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
    },
  })

  // 204 No Content is success for DELETE
  if (response.status === 204) {
    return
  }

  // For other responses, use standard handling
  if (!response.ok) {
    await handleErrorResponse(response)
  }
}

// =============================================================================
// RESPONSE HANDLING
// =============================================================================

/**
 * handleResponse - Process the fetch response
 *
 * This function:
 * 1. Checks if the response is OK (status 200-299)
 * 2. Parses the JSON body
 * 3. Returns the typed result
 *
 * If the response is not OK, it throws an ApiClientError.
 */
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    await handleErrorResponse(response)
  }

  // Handle empty responses (e.g., 204 No Content)
  const text = await response.text()
  if (!text) {
    return undefined as T
  }

  return JSON.parse(text) as T
}

/**
 * handleErrorResponse - Process error responses
 *
 * Attempts to parse the backend's error format (ApiError).
 * Falls back to a generic error message if parsing fails.
 */
async function handleErrorResponse(response: Response): Promise<never> {
  let errorBody: ApiError | undefined

  try {
    const text = await response.text()
    if (text) {
      errorBody = JSON.parse(text) as ApiError
    }
  } catch {
    // Parsing failed, errorBody remains undefined
  }

  const message = errorBody?.message || `HTTP Error ${response.status}`

  throw new ApiClientError(message, response.status, errorBody)
}
