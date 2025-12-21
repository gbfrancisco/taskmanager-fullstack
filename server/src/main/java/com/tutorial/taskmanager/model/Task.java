package com.tutorial.taskmanager.model;

import com.tutorial.taskmanager.constants.DatabaseTableConstants;
import com.tutorial.taskmanager.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = DatabaseTableConstants.TASK_TABLE)
@AttributeOverride(
    name = DatabaseTableConstants.GENERIC_ID_NAME,
    column = @Column(name = DatabaseTableConstants.TASK_ID_COLUMN)
)
public class Task extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DatabaseTableConstants.APP_USER_ID_COLUMN)
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DatabaseTableConstants.PROJECT_ID_COLUMN)
    private Project project;
}
