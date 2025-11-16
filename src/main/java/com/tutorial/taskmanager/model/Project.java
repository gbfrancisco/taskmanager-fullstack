package com.tutorial.taskmanager.model;

import com.tutorial.taskmanager.constants.DatabaseTableConstants;
import com.tutorial.taskmanager.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = DatabaseTableConstants.PROJECT_TABLE)
@AttributeOverride(
    name = DatabaseTableConstants.GENERIC_ID_NAME,
    column = @Column(name = DatabaseTableConstants.PROJECT_ID_COLUMN)
)
public class Project extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PLANNING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DatabaseTableConstants.APP_USER_ID_COLUMN)
    private AppUser appUser;

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
}
