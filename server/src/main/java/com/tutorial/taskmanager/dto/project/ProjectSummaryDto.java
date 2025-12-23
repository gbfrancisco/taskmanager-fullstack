package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for embedding project info in other responses.
 *
 * <p>This DTO contains only essential project information needed for
 * display purposes when a project is referenced by another entity
 * (e.g., when showing project details on a Task).
 *
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Embedded in TaskResponseDto to show which project a task belongs to</li>
 *   <li>Anywhere a lightweight project reference is needed</li>
 * </ul>
 *
 * <p><strong>Why not use ProjectResponseDto?</strong>
 * <ul>
 *   <li>Avoids circular references (ProjectResponseDto might include tasks)</li>
 *   <li>Keeps responses lightweight - only essential fields</li>
 *   <li>Clear API contract - embedding returns summary, not full details</li>
 * </ul>
 *
 * @see com.tutorial.taskmanager.dto.task.TaskResponseDto
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSummaryDto {
    private Long id;
    private String name;
    private ProjectStatus status;
}
