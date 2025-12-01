package com.tutorial.taskmanager.dto.task;

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
 * Instead of including nested AppUser/Project objects (which would cause
 * circular references), we include only their IDs. Clients can fetch
 * full details separately if needed.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - Task's unique identifier</li>
 *   <li>title - Task title</li>
 *   <li>description - Task description</li>
 *   <li>status - Current task status</li>
 *   <li>dueDate - Due date/time</li>
 *   <li>appUserId - ID of assigned user</li>
 *   <li>projectId - ID of project (null if not assigned to a project)</li>
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
    private Long projectId;
}
