package com.tutorial.taskmanager.controller;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.service.AppUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for AppUser operations.
 *
 * <p>Provides CRUD endpoints for managing users in the Task Manager application.
 * All endpoints return JSON and follow RESTful conventions.
 *
 * <h2>Endpoints Summary:</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th><th>Status Code</th></tr>
 *   <tr><td>POST</td><td>/api/users</td><td>Create new user</td><td>201 Created</td></tr>
 *   <tr><td>GET</td><td>/api/users/{id}</td><td>Get user by ID</td><td>200 OK</td></tr>
 *   <tr><td>GET</td><td>/api/users</td><td>Get all users</td><td>200 OK</td></tr>
 *   <tr><td>GET</td><td>/api/users/username/{username}</td><td>Get user by username</td><td>200 OK</td></tr>
 *   <tr><td>PUT</td><td>/api/users/{id}</td><td>Update user</td><td>200 OK</td></tr>
 *   <tr><td>DELETE</td><td>/api/users/{id}</td><td>Delete user</td><td>204 No Content</td></tr>
 * </table>
 *
 * <h2>Error Responses:</h2>
 * <ul>
 *   <li>404 Not Found - When user with given ID/username doesn't exist</li>
 *   <li>400 Bad Request - When validation fails (duplicate username/email, missing fields)</li>
 * </ul>
 *
 * @see AppUserService
 * @see AppUserResponseDto
 */
@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    // ========================================================================
    // CREATE OPERATIONS
    // ========================================================================

    /**
     * Create a new user.
     *
     * <p><strong>HTTP Method:</strong> POST
     * <p><strong>Path:</strong> /api/users
     * <p><strong>Request Body:</strong> {@link AppUserCreateDto} (JSON)
     * <pre>
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "secret123"
     * }
     * </pre>
     *
     * <p><strong>Response:</strong> {@link AppUserResponseDto} (JSON)
     * <pre>
     * {
     *   "id": 1,
     *   "username": "john_doe",
     *   "email": "john@example.com"
     * }
     * </pre>
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>201 Created - User created successfully</li>
     *   <li>400 Bad Request - Username/email already exists or validation failed</li>
     * </ul>
     *
     * @param createDto the user data to create
     * @return ResponseEntity with created user and 201 status
     */
    @PostMapping
    public ResponseEntity<AppUserResponseDto> createUser(@RequestBody AppUserCreateDto createDto) {
        AppUserResponseDto createdUser = appUserService.createAppUser(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    // ========================================================================
    // READ OPERATIONS
    // ========================================================================

    /**
     * Get a user by ID.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/users/{id}
     * <p><strong>Path Variable:</strong> id - The user's unique identifier
     *
     * <p><strong>Response:</strong> {@link AppUserResponseDto} (JSON)
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - User found and returned</li>
     *   <li>404 Not Found - User with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the user ID to look up
     * @return ResponseEntity with user data and 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
        AppUserResponseDto user = appUserService.getById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/users
     *
     * <p><strong>Response:</strong> List of {@link AppUserResponseDto} (JSON array)
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - Returns list (empty list if no users exist)</li>
     * </ul>
     *
     * @return ResponseEntity with list of users and 200 status
     */
    @GetMapping
    public ResponseEntity<List<AppUserResponseDto>> getAllUsers() {
        List<AppUserResponseDto> users = appUserService.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a user by username.
     *
     * <p><strong>HTTP Method:</strong> GET
     * <p><strong>Path:</strong> /api/users/username/{username}
     * <p><strong>Path Variable:</strong> username - The user's unique username
     *
     * <p><strong>Response:</strong> {@link AppUserResponseDto} (JSON)
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - User found and returned</li>
     *   <li>404 Not Found - User with given username doesn't exist</li>
     * </ul>
     *
     * @param username the username to look up
     * @return ResponseEntity with user data and 200 status
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<AppUserResponseDto> getUserByUsername(@PathVariable String username) {
        AppUserResponseDto user = appUserService.getByUsername(username);
        return ResponseEntity.ok(user);
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing user.
     *
     * <p><strong>HTTP Method:</strong> PUT
     * <p><strong>Path:</strong> /api/users/{id}
     * <p><strong>Path Variable:</strong> id - The user's unique identifier
     * <p><strong>Request Body:</strong> {@link AppUserUpdateDto} (JSON)
     * <pre>
     * {
     *   "email": "newemail@example.com",
     *   "password": "newpassword123"
     * }
     * </pre>
     *
     * <p><strong>Note:</strong> Username cannot be updated (immutable business rule).
     * Only email and password can be changed. Both fields are optional.
     *
     * <p><strong>Response:</strong> {@link AppUserResponseDto} (JSON)
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>200 OK - User updated successfully</li>
     *   <li>404 Not Found - User with given ID doesn't exist</li>
     *   <li>400 Bad Request - New email already exists</li>
     * </ul>
     *
     * @param id the ID of the user to update
     * @param updateDto the updated user data
     * @return ResponseEntity with updated user and 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> updateUser(
        @PathVariable Long id,
        @RequestBody AppUserUpdateDto updateDto
    ) {
        AppUserResponseDto updatedUser = appUserService.updateAppUser(id, updateDto);
        return ResponseEntity.ok(updatedUser);
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a user by ID.
     *
     * <p><strong>HTTP Method:</strong> DELETE
     * <p><strong>Path:</strong> /api/users/{id}
     * <p><strong>Path Variable:</strong> id - The user's unique identifier
     *
     * <p><strong>Response:</strong> No content (empty body)
     *
     * <p><strong>Status Codes:</strong>
     * <ul>
     *   <li>204 No Content - User deleted successfully</li>
     *   <li>404 Not Found - User with given ID doesn't exist</li>
     * </ul>
     *
     * @param id the ID of the user to delete
     * @return ResponseEntity with 204 status and no body
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        appUserService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
