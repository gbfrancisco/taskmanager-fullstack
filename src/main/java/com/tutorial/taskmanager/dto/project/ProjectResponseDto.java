package com.tutorial.taskmanager.dto.project;

import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.model.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;

    public ProjectResponseDto(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.status = project.getStatus();
    }
}
