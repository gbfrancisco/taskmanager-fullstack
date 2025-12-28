package com.tutorial.taskmanager.dto.task;

import com.tutorial.taskmanager.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a new Task.
 *
 * <p>Used as input to the service layer. Conversion to entity is handled
 * by {@link com.tutorial.taskmanager.mapper.TaskMapper}.
 *
 * <p><strong>User Assignment:</strong>
 * The task is automatically assigned to the authenticated user. The user ID
 * is extracted from the JWT token by the controller, not from the request body.
 * This ensures users can only create tasks for themselves.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>title - Task title (required)</li>
 *   <li>description - Task description (optional)</li>
 *   <li>status - Task status (optional, defaults to TODO if not provided)</li>
 *   <li>dueDate - Due date/time (optional)</li>
 *   <li>projectId - ID of the project this task belongs to (optional)</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateDto {
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
    private Long projectId;
}
