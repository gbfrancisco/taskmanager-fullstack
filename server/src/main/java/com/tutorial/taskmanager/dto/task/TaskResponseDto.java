package com.tutorial.taskmanager.dto.task;

import com.tutorial.taskmanager.dto.project.ProjectSummaryDto;
import com.tutorial.taskmanager.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Task responses.
 *
 * <p>Used as output from the service layer. Conversion from entity is handled
 * by {@link com.tutorial.taskmanager.mapper.TaskMapper}.
 *
 * <p><strong>Relationship Handling:</strong>
 * <ul>
 *   <li>appUserId - ID only (to avoid circular references with user's tasks)</li>
 *   <li>project - Embedded summary (lightweight, no circular reference risk)</li>
 * </ul>
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - Task's unique identifier</li>
 *   <li>title - Task title</li>
 *   <li>description - Task description</li>
 *   <li>status - Current task status</li>
 *   <li>dueDate - Due date/time</li>
 *   <li>appUserId - ID of assigned user</li>
 *   <li>project - Project summary (null if not assigned to a project)</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
    private Long appUserId;

    /**
     * Embedded project summary with id, name, and status.
     * Null if the task is not assigned to any project.
     */
    private ProjectSummaryDto project;
}
