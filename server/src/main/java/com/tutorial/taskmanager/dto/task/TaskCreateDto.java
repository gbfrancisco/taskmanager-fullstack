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
 * <p><strong>Relationship Handling:</strong>
 * <ul>
 *   <li>appUserId - ID of the user to assign the task to (required)</li>
 *   <li>projectId - ID of the project this task belongs to (optional)</li>
 * </ul>
 * The service layer will fetch the actual entities and set the relationships.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>title - Task title (required)</li>
 *   <li>description - Task description (optional)</li>
 *   <li>status - Task status (optional, defaults to TODO if not provided)</li>
 *   <li>dueDate - Due date/time (optional)</li>
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
    private Long appUserId;
    private Long projectId;
}
