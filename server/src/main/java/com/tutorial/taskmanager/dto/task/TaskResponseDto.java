package com.tutorial.taskmanager.dto.task;

import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
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
 * Both relationships are embedded as lightweight summary DTOs to provide
 * descriptive information without circular references.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - Task's unique identifier</li>
 *   <li>title - Task title</li>
 *   <li>description - Task description</li>
 *   <li>status - Current task status</li>
 *   <li>dueDate - Due date/time</li>
 *   <li>appUser - Assigned user summary (id, username)</li>
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

    /**
     * Embedded user summary with id and username.
     * Provides descriptive user info without exposing sensitive data.
     */
    private AppUserSummaryDto appUser;

    /**
     * Embedded project summary with id, name, and status.
     * Null if the task is not assigned to any project.
     */
    private ProjectSummaryDto project;
}
