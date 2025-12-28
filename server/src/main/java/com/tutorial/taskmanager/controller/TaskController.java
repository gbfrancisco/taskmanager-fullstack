package com.tutorial.taskmanager.controller;

import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Task operations.
 *
 * <p>Provides CRUD endpoints and filtering for tasks in the Task Manager application.
 * All endpoints return JSON and follow RESTful conventions.
 *
 * <p><strong>Authorization:</strong>
 * All endpoints require authentication. Data is automatically scoped to the
 * authenticated user - users can only access their own tasks.
 *
 * <h2>Endpoints Summary:</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th><th>Status Code</th></tr>
 *   <tr><td>POST</td><td>/api/tasks</td><td>Create new task</td><td>201 Created</td></tr>
 *   <tr><td>GET</td><td>/api/tasks/{id}</td><td>Get task by ID</td><td>200 OK</td></tr>
 *   <tr><td>GET</td><td>/api/tasks</td><td>Get all tasks (with filters)</td><td>200 OK</td></tr>
 *   <tr><td>PUT</td><td>/api/tasks/{id}</td><td>Update task</td><td>200 OK</td></tr>
 *   <tr><td>DELETE</td><td>/api/tasks/{id}</td><td>Delete task</td><td>204 No Content</td></tr>
 *   <tr><td>PUT</td><td>/api/tasks/{id}/project/{projectId}</td><td>Assign to project</td><td>200 OK</td></tr>
 *   <tr><td>DELETE</td><td>/api/tasks/{id}/project</td><td>Remove from project</td><td>200 OK</td></tr>
 * </table>
 *
 * <h2>Query Parameters for GET /api/tasks:</h2>
 * <ul>
 *   <li>projectId - Filter by project ID (must be user's project)</li>
 *   <li>status - Filter by task status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)</li>
 *   <li>overdue - If true, return only overdue tasks</li>
 * </ul>
 *
 * <h2>Error Responses:</h2>
 * <ul>
 *   <li>401 Unauthorized - Not authenticated</li>
 *   <li>404 Not Found - Task/Project not found</li>
 *   <li>400 Bad Request - Validation failed (e.g., project doesn't belong to user)</li>
 * </ul>
 *
 * @see TaskService
 * @see TaskResponseDto
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ========================================================================
    // CREATE OPERATIONS
    // ========================================================================

    /**
     * Create a new task for the authenticated user.
     *
     * <p><strong>HTTP Method:</strong> POST
     * <p><strong>Path:</strong> /api/tasks
     * <p><strong>Request Body:</strong> {@link TaskCreateDto} (JSON)
     * <pre>
     * {
     *   "title": "Implement login feature",
     *   "description": "Add JWT authentication",
     *   "status": "TODO",
     *   "dueDate": "2024-12-31T23:59:59",
     *   "projectId": 1
     * }
     * </pre>
     *
     * <p><strong>Required Fields:</strong> title
     * <p><strong>Optional Fields:</strong> description, status (defaults to TODO), dueDate, projectId
     *
     * <p><strong>Note:</strong> The task is automatically assigned to the authenticated user.
     * No userId field is required in the request body.
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>201 Created - Task created successfully</li>
     *   <li>400 Bad Request - Validation failed or project doesn't belong to user</li>
     *   <li>404 Not Found - Project not found</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param createDto the task data to create
     * @return ResponseEntity with created task and 201 status
     */
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @RequestBody TaskCreateDto createDto
    ) {
        Long userId = userDetails.getAppUser().getId();
        TaskResponseDto createdTask = taskService.createTask(createDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    // ========================================================================
    // READ OPERATIONS
    // ========================================================================

    /**
     * Get a task by ID, with ownership check.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/tasks/{id}
     *
     * <p><strong>Note:</strong> Only returns the task if it belongs to the authenticated user.
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task found and returned</li>
     *   <li>400 Bad Request - Task doesn't belong to authenticated user</li>
     *   <li>404 Not Found - Task with given ID doesn't exist</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param id the task ID to look up
     * @return ResponseEntity with task data and 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @PathVariable Long id
    ) {
        Long userId = userDetails.getAppUser().getId();
        TaskResponseDto task = taskService.getById(id, userId);
        return ResponseEntity.ok(task);
    }

    /**
     * Get all tasks for the authenticated user with optional filters.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/tasks
     *
     * <p><strong>Note:</strong> Always returns only tasks belonging to the authenticated user.
     *
     * <p><strong>Query Parameters (all optional):</strong>
     * <ul>
     *   <li>projectId - Filter by project ID (must be user's project)</li>
     *   <li>status - Filter by status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)</li>
     *   <li>overdue - If true, return only overdue tasks</li>
     * </ul>
     *
     * <p><strong>Filter Combinations:</strong>
     * <ul>
     *   <li>No params: Returns all user's tasks</li>
     *   <li>projectId only: User's tasks for specific project</li>
     *   <li>status only: User's tasks with specific status</li>
     *   <li>projectId + status: User's tasks in project with specific status</li>
     *   <li>overdue=true: User's overdue tasks</li>
     * </ul>
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Returns list (empty list if no matching tasks)</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param projectId filter by project ID (optional)
     * @param status filter by task status (optional)
     * @param overdue if true, return only overdue tasks (optional)
     * @return ResponseEntity with list of tasks and 200 status
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) Boolean overdue
    ) {
        Long userId = userDetails.getAppUser().getId();
        List<TaskResponseDto> tasks;

        // Overdue tasks filter
        if (Boolean.TRUE.equals(overdue)) {
            tasks = taskService.findOverdueTasks(userId);
            return ResponseEntity.ok(tasks);
        }

        // Combined project + status filter
        if (projectId != null && status != null) {
            tasks = taskService.findByProjectIdAndStatus(projectId, status, userId);
            return ResponseEntity.ok(tasks);
        }

        // Project filter only
        if (projectId != null) {
            tasks = taskService.findByProjectId(projectId, userId);
            return ResponseEntity.ok(tasks);
        }

        // Status filter only
        if (status != null) {
            tasks = taskService.findByStatus(status, userId);
            return ResponseEntity.ok(tasks);
        }

        // No filters - return all user's tasks
        tasks = taskService.findAll(userId);
        return ResponseEntity.ok(tasks);
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing task, with ownership check.
     *
     * <p><strong>HTTP Method:</strong> PUT
     * <p><strong>Path:</strong> /api/tasks/{id}
     * <p><strong>Request Body:</strong> {@link TaskUpdateDto} (JSON)
     * <pre>
     * {
     *   "title": "Updated title",
     *   "description": "Updated description",
     *   "status": "IN_PROGRESS",
     *   "dueDate": "2024-12-31T23:59:59"
     * }
     * </pre>
     *
     * <p><strong>Note:</strong> All fields are optional. Only provided fields will be updated.
     * Project assignment uses separate endpoints (assignToProject/removeFromProject).
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task updated successfully</li>
     *   <li>400 Bad Request - Task doesn't belong to authenticated user</li>
     *   <li>404 Not Found - Task with given ID doesn't exist</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param id the ID of the task to update
     * @param updateDto the updated task data
     * @return ResponseEntity with updated task and 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @PathVariable Long id,
        @RequestBody TaskUpdateDto updateDto
    ) {
        Long userId = userDetails.getAppUser().getId();
        TaskResponseDto updatedTask = taskService.updateTask(id, updateDto, userId);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Assign a task to a project, with ownership checks for both.
     *
     * <p><strong>HTTP Method:</strong> PUT
     * <p><strong>Path:</strong> /api/tasks/{id}/project/{projectId}
     *
     * <p><strong>Validation:</strong> Both the task and project must belong to the authenticated user.
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task assigned to project successfully</li>
     *   <li>400 Bad Request - Task or project doesn't belong to authenticated user</li>
     *   <li>404 Not Found - Task or project not found</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param id the task ID
     * @param projectId the project ID to assign to
     * @return ResponseEntity with updated task and 200 status
     */
    @PutMapping("/{id}/project/{projectId}")
    public ResponseEntity<TaskResponseDto> assignToProject(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @PathVariable Long id,
        @PathVariable Long projectId
    ) {
        Long userId = userDetails.getAppUser().getId();
        TaskResponseDto updatedTask = taskService.assignToProject(id, projectId, userId);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Remove a task from its project, with ownership check.
     *
     * <p><strong>HTTP Method:</strong> DELETE
     * <p><strong>Path:</strong> /api/tasks/{id}/project
     *
     * <p><strong>Note:</strong> This removes the task from its project but does not delete the task.
     * The task's projectId will become null.
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task removed from project successfully</li>
     *   <li>400 Bad Request - Task doesn't belong to authenticated user</li>
     *   <li>404 Not Found - Task not found</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param id the task ID
     * @return ResponseEntity with updated task and 200 status
     */
    @DeleteMapping("/{id}/project")
    public ResponseEntity<TaskResponseDto> removeFromProject(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @PathVariable Long id
    ) {
        Long userId = userDetails.getAppUser().getId();
        TaskResponseDto updatedTask = taskService.removeFromProject(id, userId);
        return ResponseEntity.ok(updatedTask);
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a task by ID, with ownership check.
     *
     * <p><strong>HTTP Method:</strong> DELETE
     * <p><strong>Path:</strong> /api/tasks/{id}
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>204 No Content - Task deleted successfully</li>
     *   <li>400 Bad Request - Task doesn't belong to authenticated user</li>
     *   <li>404 Not Found - Task with given ID doesn't exist</li>
     * </ul>
     *
     * @param userDetails the authenticated user (injected by Spring Security)
     * @param id the ID of the task to delete
     * @return ResponseEntity with 204 status and no body
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
        @AuthenticationPrincipal AppUserDetails userDetails,
        @PathVariable Long id
    ) {
        Long userId = userDetails.getAppUser().getId();
        taskService.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }
}
