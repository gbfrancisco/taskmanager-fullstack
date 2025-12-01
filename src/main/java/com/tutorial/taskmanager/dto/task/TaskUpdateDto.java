package com.tutorial.taskmanager.dto.task;

import com.tutorial.taskmanager.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for updating an existing Task.
 *
 * <p>Used as input to the service layer. Conversion to entity is handled
 * by {@link com.tutorial.taskmanager.mapper.TaskMapper#updateEntityFromDto}.
 *
 * <p><strong>All fields are optional.</strong> Only provided fields will be updated.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>title - New title (optional)</li>
 *   <li>description - New description (optional)</li>
 *   <li>status - New status (optional)</li>
 *   <li>dueDate - New due date (optional)</li>
 * </ul>
 *
 * <p><strong>Note:</strong> To change task assignment (appUser) or project,
 * use separate methods in the service layer.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateDto {
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
}
