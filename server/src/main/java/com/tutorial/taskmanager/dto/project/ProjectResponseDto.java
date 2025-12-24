package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Project responses.
 *
 * <p>Used as output from the service layer. Conversion from entity is handled
 * by {@link com.tutorial.taskmanager.mapper.ProjectMapper}.
 *
 * <p><strong>Relationship Handling:</strong>
 * The owner relationship is embedded as a lightweight summary DTO to provide
 * descriptive information without circular references.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - Project's unique identifier</li>
 *   <li>name - Project name</li>
 *   <li>description - Project description</li>
 *   <li>status - Current project status</li>
 *   <li>appUser - Project owner summary (id, username)</li>
 *   <li>taskCount - Number of tasks in this project (computed field)</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private LocalDateTime createdTimestamp;
    private LocalDateTime updatedTimestamp;

    /**
     * Embedded owner summary with id and username.
     * Provides descriptive user info without exposing sensitive data.
     */
    private AppUserSummaryDto appUser;

    /**
     * Number of tasks associated with this project.
     * This is a computed field, populated by the service layer
     * using an efficient COUNT query (not by loading all tasks).
     */
    private Long taskCount;
}
