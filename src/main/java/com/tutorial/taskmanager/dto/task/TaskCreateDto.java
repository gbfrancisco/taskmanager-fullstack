package com.tutorial.taskmanager.dto.task;

import com.tutorial.taskmanager.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateDto {
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
}
