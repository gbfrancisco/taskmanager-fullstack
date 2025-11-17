package com.tutorial.taskmanager.dto.task;

import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.model.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateDto {
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;

    public TaskUpdateDto(Task task) {
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.dueDate = task.getDueDate();
    }
}
