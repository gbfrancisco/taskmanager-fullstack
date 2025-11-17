package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreateDto {
    private String name;
    private String description;
    private ProjectStatus status;
}
