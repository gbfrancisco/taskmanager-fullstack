package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing Project.
 *
 * <p>Used as input to the service layer. Conversion to entity is handled
 * by {@link com.tutorial.taskmanager.mapper.ProjectMapper#updateEntityFromDto}.
 *
 * <p><strong>All fields are optional.</strong> Only provided fields will be updated.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>name - New project name (optional, must be unique per user if changed)</li>
 *   <li>description - New description (optional)</li>
 *   <li>status - New status (optional)</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Project ownership (appUser) cannot be changed.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectUpdateDto {
    private String name;
    private String description;
    private ProjectStatus status;
}
