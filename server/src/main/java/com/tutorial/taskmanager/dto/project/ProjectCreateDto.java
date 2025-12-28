package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new Project.
 *
 * <p>Used as input to the service layer. Conversion to entity is handled
 * by {@link com.tutorial.taskmanager.mapper.ProjectMapper}.
 *
 * <p><strong>User Assignment:</strong>
 * The project is automatically assigned to the authenticated user. The user ID
 * is extracted from the JWT token by the controller, not from the request body.
 * This ensures users can only create projects for themselves.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>name - Project name (required, must be unique per user)</li>
 *   <li>description - Project description (optional)</li>
 *   <li>status - Project status (optional, defaults to PLANNING if not provided)</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreateDto {
    private String name;
    private String description;
    private ProjectStatus status;
}
