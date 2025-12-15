package com.tutorial.taskmanager.controller;

import com.tutorial.taskmanager.dto.project.ProjectCreateDto;
import com.tutorial.taskmanager.dto.project.ProjectResponseDto;
import com.tutorial.taskmanager.dto.project.ProjectUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Project operations.
 *
 * <p>Provides CRUD endpoints and filtering for projects in the Task Manager application.
 * All endpoints return JSON and follow RESTful conventions.
 *
 * <h2>Endpoints Summary:</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th><th>Status Code</th></tr>
 *   <tr><td>POST</td><td>/api/projects</td><td>Create new project</td><td>201 Created</td></tr>
 *   <tr><td>GET</td><td>/api/projects/{id}</td><td>Get project by ID</td><td>200 OK</td></tr>
 *   <tr><td>GET</td><td>/api/projects</td><td>Get all projects (with filters)</td><td>200 OK</td></tr>
 *   <tr><td>PUT</td><td>/api/projects/{id}</td><td>Update project</td><td>200 OK</td></tr>
 *   <tr><td>DELETE</td><td>/api/projects/{id}</td><td>Delete project</td><td>204 No Content</td></tr>
 * </table>
 *
 * <h2>Query Parameters for GET /api/projects:</h2>
 * <ul>
 *   <li>userId - Filter by user ID</li>
 *   <li>status - Filter by project status (PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED)</li>
 *   <li>name - Search by name (contains, case-insensitive)</li>
 * </ul>
 *
 * <h2>Error Responses:</h2>
 * <ul>
 *   <li>404 Not Found - Project/User not found</li>
 *   <li>400 Bad Request - Validation failed (e.g., duplicate project name)</li>
 * </ul>
 *
 * @see ProjectService
 * @see ProjectResponseDto
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // ========================================================================
    // CREATE OPERATIONS
    // ========================================================================

    /**
     * Create a new project.
     *
     * <p><strong>HTTP Method:</strong> POST
     * <p><strong>Path:</strong> /api/projects
     * <p><strong>Request Body:</strong> {@link ProjectCreateDto} (JSON)
     * <pre>
     * {
     *   "name": "My Project",
     *   "description": "Project description",
     *   "status": "PLANNING",
     *   "appUserId": 1
     * }
     * </pre>
     *
     * <p><strong>Required Fields:</strong> name, appUserId
     * <p><strong>Optional Fields:</strong> description, status (defaults to PLANNING)
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>201 Created - Project created successfully</li>
     *   <li>400 Bad Request - Project name already exists for user</li>
     *   <li>404 Not Found - User not found</li>
     * </ul>
     *
     * @param createDto the project data to create
     * @return ResponseEntity with created project and 201 status
     */
    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(@RequestBody ProjectCreateDto createDto) {
        ProjectResponseDto createdProject = projectService.createProject(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    // ========================================================================
    // READ OPERATIONS
    // ========================================================================

    /**
     * Get a project by ID.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/projects/{id}
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Project found and returned</li>
     *   <li>404 Not Found - Project with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the project ID to look up
     * @return ResponseEntity with project data and 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> getProjectById(@PathVariable Long id) {
        ProjectResponseDto project = projectService.getById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Get all projects with optional filters.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/projects
     *
     * <p><strong>Query Parameters (all optional):</strong>
     * <ul>
     *   <li>userId - Filter by user ID</li>
     *   <li>status - Filter by status (PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED)</li>
     *   <li>name - Search by name (contains, case-insensitive)</li>
     * </ul>
     *
     * <p><strong>Filter Combinations:</strong>
     * <ul>
     *   <li>No params: Returns all projects</li>
     *   <li>userId only: Projects for specific user</li>
     *   <li>status only: Projects with specific status</li>
     *   <li>name only: Projects matching name search</li>
     *   <li>userId + status: User's projects with specific status</li>
     *   <li>userId + name: User's projects matching name search</li>
     * </ul>
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Returns list (empty list if no matching projects)</li>
     * </ul>
     *
     * @param userId filter by user ID (optional)
     * @param status filter by project status (optional)
     * @param name search by project name (optional)
     * @return ResponseEntity with list of projects and 200 status
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> getAllProjects(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) ProjectStatus status,
        @RequestParam(required = false) String name
    ) {
        List<ProjectResponseDto> projects;
        if (userId != null && status != null) {
            projects = projectService.findByAppUserIdAndStatus(userId, status);
            return ResponseEntity.ok(projects);
        }
        if (userId != null && name != null) {
            projects = projectService.findByAppUserIdAndNameContaining(userId, name);
            return ResponseEntity.ok(projects);
        }
        if (userId != null) {
            projects = projectService.findByAppUserId(userId);
            return ResponseEntity.ok(projects);
        }
        if (status != null) {
            projects = projectService.findByStatus(status);
            return ResponseEntity.ok(projects);
        }
        if (name != null) {
            projects = projectService.findByNameContaining(name);
            return ResponseEntity.ok(projects);
        }
        projects = projectService.findAll();
        return ResponseEntity.ok(projects);
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing project.
     *
     * <p><strong>HTTP Method:</strong> PUT
     * <p><strong>Path:</strong> /api/projects/{id}
     * <p><strong>Request Body:</strong> {@link ProjectUpdateDto} (JSON)
     * <pre>
     * {
     *   "name": "Updated name",
     *   "description": "Updated description",
     *   "status": "ACTIVE"
     * }
     * </pre>
     *
     * <p><strong>Note:</strong> All fields are optional. Only provided fields will be updated.
     * Project ownership (appUser) cannot be changed.
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Project updated successfully</li>
     *   <li>400 Bad Request - Project name already exists for user</li>
     *   <li>404 Not Found - Project with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the ID of the project to update
     * @param updateDto the updated project data
     * @return ResponseEntity with updated project and 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> updateProject(
        @PathVariable Long id,
        @RequestBody ProjectUpdateDto updateDto
    ) {
        ProjectResponseDto updatedProject = projectService.updateProject(id, updateDto);
        return ResponseEntity.ok(updatedProject);
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a project by ID.
     *
     * <p><strong>HTTP Method:</strong> DELETE
     * <p><strong>Path:</strong> /api/projects/{id}
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>204 No Content - Project deleted successfully</li>
     *   <li>404 Not Found - Project with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the ID of the project to delete
     * @return ResponseEntity with 204 status and no body
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
