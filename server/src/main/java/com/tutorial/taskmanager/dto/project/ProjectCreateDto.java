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
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>name - Project name (required, must be unique per user)</li>
 *   <li>description - Project description (optional)</li>
 *   <li>status - Project status (optional, defaults to PLANNING if not provided)</li>
 *   <li>appUserId - ID of the user who owns the project (required)</li>
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
    private Long appUserId;
}
