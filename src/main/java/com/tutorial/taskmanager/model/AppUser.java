package com.tutorial.taskmanager.model;

import com.tutorial.taskmanager.constants.DatabaseTableConstants;
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
@Table(name = DatabaseTableConstants.APP_USER_TABLE)
@AttributeOverride(
    name = DatabaseTableConstants.GENERIC_ID_NAME,
    column = @Column(name = DatabaseTableConstants.APP_USER_ID_COLUMN)
)
public class AppUser extends BaseEntity {
    @Column(nullable = false, updatable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();
}
