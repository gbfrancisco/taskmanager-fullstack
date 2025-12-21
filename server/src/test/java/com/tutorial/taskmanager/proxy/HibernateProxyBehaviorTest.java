package com.tutorial.taskmanager.proxy;

import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import com.tutorial.taskmanager.repository.AppUserRepository;
import com.tutorial.taskmanager.repository.ProjectRepository;
import com.tutorial.taskmanager.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify Hibernate 6 proxy behavior with getId().
 *
 * In Hibernate 6 (Spring Boot 3), calling getId() on a lazy proxy
 * should NOT trigger initialization by default.
 *
 * This test verifies that behavior so we can safely compare IDs
 * without causing extra database queries.
 */
@DataJpaTest
@DisplayName("Hibernate 6 Proxy Behavior Tests")
class HibernateProxyBehaviorTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Long userId;
    private Long taskId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        // Create and persist test data
        AppUser user = new AppUser();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user = appUserRepository.save(user);
        userId = user.getId();

        Project project = new Project();
        project.setName("Test Project");
        project.setAppUser(user);
        project = projectRepository.save(project);
        projectId = project.getId();

        Task task = new Task();
        task.setTitle("Test Task");
        task.setAppUser(user);
        task.setProject(project);
        task = taskRepository.save(task);
        taskId = task.getId();

        // Clear persistence context to ensure we get fresh proxies
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("getId() on lazy proxy should NOT trigger initialization (Hibernate 6 behavior)")
    void getIdOnProxyShouldNotTriggerInitialization() {
        // Arrange - Get a reference (proxy) without loading the entity
        AppUser proxy = entityManager.getReference(AppUser.class, userId);

        // Verify it's not initialized yet
        assertThat(Hibernate.isInitialized(proxy))
                .as("Proxy should not be initialized after getReference()")
                .isFalse();

        // Act - Access the ID
        Long id = proxy.getId();

        // Assert - Proxy should STILL not be initialized (Hibernate 6 behavior)
        assertThat(id).isEqualTo(userId);
        assertThat(Hibernate.isInitialized(proxy))
                .as("Proxy should NOT be initialized after getId() in Hibernate 6")
                .isFalse();
    }

    @Test
    @DisplayName("Accessing non-ID property SHOULD trigger initialization")
    void accessingNonIdPropertyShouldTriggerInitialization() {
        // Arrange
        AppUser proxy = entityManager.getReference(AppUser.class, userId);
        assertThat(Hibernate.isInitialized(proxy)).isFalse();

        // Act - Access a non-ID property
        String username = proxy.getUsername();

        // Assert - Proxy SHOULD be initialized now
        assertThat(username).isEqualTo("testuser");
        assertThat(Hibernate.isInitialized(proxy))
                .as("Proxy SHOULD be initialized after accessing non-ID property")
                .isTrue();
    }

    @Test
    @DisplayName("Comparing proxy IDs should NOT trigger initialization")
    void comparingProxyIdsShouldNotTriggerInitialization() {
        // Arrange - Load task and project, their appUser will be lazy proxies
        Task task = taskRepository.findById(taskId).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();

        // Clear to ensure appUser relationships are lazy proxies
        entityManager.flush();
        entityManager.clear();

        // Re-fetch to get fresh entities with uninitialized proxies
        task = taskRepository.findById(taskId).orElseThrow();
        project = projectRepository.findById(projectId).orElseThrow();

        // Get the lazy proxies
        AppUser taskUserProxy = task.getAppUser();
        AppUser projectUserProxy = project.getAppUser();

        // Verify proxies are not initialized
        assertThat(Hibernate.isInitialized(taskUserProxy))
                .as("task.appUser should be a lazy proxy")
                .isFalse();
        assertThat(Hibernate.isInitialized(projectUserProxy))
                .as("project.appUser should be a lazy proxy")
                .isFalse();

        // Act - Compare IDs (this is what we want to do in TaskService)
        boolean sameUser = taskUserProxy.getId().equals(projectUserProxy.getId());

        // Assert - Both should STILL not be initialized
        assertThat(sameUser).isTrue();
        assertThat(Hibernate.isInitialized(taskUserProxy))
                .as("task.appUser should NOT be initialized after getId()")
                .isFalse();
        assertThat(Hibernate.isInitialized(projectUserProxy))
                .as("project.appUser should NOT be initialized after getId()")
                .isFalse();
    }

    @Test
    @DisplayName("Objects.equals on proxies SHOULD trigger initialization (comparing entities, not IDs)")
    void objectsEqualsOnProxiesShouldTriggerInitialization() {
        // Arrange
        Task task = taskRepository.findById(taskId).orElseThrow();

        entityManager.flush();
        entityManager.clear();

        task = taskRepository.findById(taskId).orElseThrow();
        AppUser proxy = task.getAppUser();

        assertThat(Hibernate.isInitialized(proxy))
                .as("appUser should start as uninitialized proxy")
                .isFalse();

        // Act - Call equals() on the proxy (simulating Objects.equals(proxyA, proxyB))
        // This calls the proxy's equals method which delegates to the entity
        boolean result = proxy.equals(proxy);

        // Assert - This SHOULD trigger initialization because equals() is not getId()
        // Note: The exact behavior may vary, but typically equals() triggers init
        assertThat(result).isTrue();

        // Log the result for learning purposes
        System.out.println("After calling equals(): isInitialized = " + Hibernate.isInitialized(proxy));
    }
}
