package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Project responses.
 *
 * <p>Used as output from the service layer. Conversion from entity is handled
 * by {@link com.tutorial.taskmanager.mapper.ProjectMapper}.
 *
 * <p><strong>Relationship Handling:</strong>
 * Instead of including the nested AppUser object (which would cause
 * circular references), we include only the appUserId. Clients can fetch
 * full user details separately if needed.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - Project's unique identifier</li>
 *   <li>name - Project name</li>
 *   <li>description - Project description</li>
 *   <li>status - Current project status</li>
 *   <li>appUserId - ID of the project owner</li>
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
    private Long appUserId;
}
