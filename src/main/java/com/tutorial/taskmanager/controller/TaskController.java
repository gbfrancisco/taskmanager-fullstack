package com.tutorial.taskmanager.controller;

import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Task operations.
 *
 * <p>Provides CRUD endpoints and filtering for tasks in the Task Manager application.
 * All endpoints return JSON and follow RESTful conventions.
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
 *   <li>userId - Filter by user ID</li>
 *   <li>projectId - Filter by project ID</li>
 *   <li>status - Filter by task status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)</li>
 *   <li>overdue - If true, return only overdue tasks</li>
 * </ul>
 *
 * <h2>Error Responses:</h2>
 * <ul>
 *   <li>404 Not Found - Task/User/Project not found</li>
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
     * Create a new task.
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
     *   "appUserId": 1,
     *   "projectId": 1
     * }
     * </pre>
     *
     * <p><strong>Required Fields:</strong> title, appUserId
     * <p><strong>Optional Fields:</strong> description, status (defaults to TODO), dueDate, projectId
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>201 Created - Task created successfully</li>
     *   <li>400 Bad Request - Validation failed or project doesn't belong to user</li>
     *   <li>404 Not Found - User or project not found</li>
     * </ul>
     *
     * @param createDto the task data to create
     * @return ResponseEntity with created task and 201 status
     */
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@RequestBody TaskCreateDto createDto) {
        TaskResponseDto createdTask = taskService.createTask(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    // ========================================================================
    // READ OPERATIONS
    // ========================================================================

    /**
     * Get a task by ID.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/tasks/{id}
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task found and returned</li>
     *   <li>404 Not Found - Task with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the task ID to look up
     * @return ResponseEntity with task data and 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long id) {
        TaskResponseDto task = taskService.getById(id);
        return ResponseEntity.ok(task);
    }

    /**
     * Get all tasks with optional filters.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/tasks
     *
     * <p><strong>Query Parameters (all optional):</strong>
     * <ul>
     *   <li>userId - Filter by user ID</li>
     *   <li>projectId - Filter by project ID</li>
     *   <li>status - Filter by status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)</li>
     *   <li>overdue - If true, return only overdue tasks</li>
     * </ul>
     *
     * <p><strong>Filter Combinations:</strong>
     * <ul>
     *   <li>No params: Returns all tasks</li>
     *   <li>userId only: Tasks for specific user</li>
     *   <li>projectId only: Tasks for specific project</li>
     *   <li>status only: Tasks with specific status</li>
     *   <li>userId + status: User's tasks with specific status</li>
     *   <li>projectId + status: Project's tasks with specific status</li>
     *   <li>overdue=true: All overdue tasks</li>
     *   <li>userId + overdue=true: User's overdue tasks</li>
     * </ul>
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Returns list (empty list if no matching tasks)</li>
     * </ul>
     *
     * @param userId filter by user ID (optional)
     * @param projectId filter by project ID (optional)
     * @param status filter by task status (optional)
     * @param overdue if true, return only overdue tasks (optional)
     * @return ResponseEntity with list of tasks and 200 status
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) Boolean overdue
    ) {
        List<TaskResponseDto> tasks;
        if (Boolean.TRUE.equals(overdue) && userId != null) {
            tasks = taskService.findOverdueTasksByAppUserId(userId);
            return ResponseEntity.ok(tasks);
        }
        if (Boolean.TRUE.equals(overdue)) {
            tasks = taskService.findOverdueTasks();
            return ResponseEntity.ok(tasks);
        }
        if (userId != null && status != null) {
            tasks = taskService.findByAppUserIdAndStatus(userId, status);
            return ResponseEntity.ok(tasks);
        }
        if (projectId != null && status != null) {
            tasks = taskService.findByProjectIdAndStatus(projectId, status);
            return ResponseEntity.ok(tasks);
        }
        if (userId != null) {
            tasks = taskService.findByAppUserId(userId);
            return ResponseEntity.ok(tasks);
        }
        if (projectId != null) {
            tasks = taskService.findByProjectId(projectId);
            return ResponseEntity.ok(tasks);
        }
        if (status != null) {
            tasks = taskService.findByStatus(status);
            return ResponseEntity.ok(tasks);
        }
        tasks = taskService.findAll();
        return ResponseEntity.ok(tasks);
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing task.
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
     * User assignment cannot be changed (use reassign endpoint if needed in future).
     * Project assignment uses separate endpoints (assignToProject/removeFromProject).
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task updated successfully</li>
     *   <li>404 Not Found - Task with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the ID of the task to update
     * @param updateDto the updated task data
     * @return ResponseEntity with updated task and 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @RequestBody TaskUpdateDto updateDto) {
        TaskResponseDto updatedTask = taskService.updateTask(id, updateDto);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Assign a task to a project.
     *
     * <p><strong>HTTP Method:</strong> PUT
     * <p><strong>Path:</strong> /api/tasks/{id}/project/{projectId}
     *
     * <p><strong>Validation:</strong> The project must belong to the same user as the task.
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Task assigned to project successfully</li>
     *   <li>404 Not Found - Task or project not found</li>
     *   <li>400 Bad Request - Project doesn't belong to task's user</li>
     * </ul>
     *
     * @param id the task ID
     * @param projectId the project ID to assign to
     * @return ResponseEntity with updated task and 200 status
     */
    @PutMapping("/{id}/project/{projectId}")
    public ResponseEntity<TaskResponseDto> assignToProject(@PathVariable Long id, @PathVariable Long projectId) {
        TaskResponseDto updatedTask = taskService.assignToProject(id, projectId);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Remove a task from its project.
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
     *   <li>404 Not Found - Task not found</li>
     * </ul>
     *
     * @param id the task ID
     * @return ResponseEntity with updated task and 200 status
     */
    @DeleteMapping("/{id}/project")
    public ResponseEntity<TaskResponseDto> removeFromProject(@PathVariable Long id) {
        TaskResponseDto updatedTask = taskService.removeFromProject(id);
        return ResponseEntity.ok(updatedTask);
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a task by ID.
     *
     * <p><strong>HTTP Method:</strong> DELETE
     * <p><strong>Path:</strong> /api/tasks/{id}
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>204 No Content - Task deleted successfully</li>
     *   <li>404 Not Found - Task with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the ID of the task to delete
     * @return ResponseEntity with 204 status and no body
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
