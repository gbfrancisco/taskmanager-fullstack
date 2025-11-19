package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.exception.ValidationException;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import com.tutorial.taskmanager.repository.AppUserRepository;
import com.tutorial.taskmanager.repository.ProjectRepository;
import com.tutorial.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TaskService.
 * <p>
 * Tests all CRUD operations, validation logic, filtering, and business rules.
 * Uses Mockito to mock repository dependencies and AssertJ for assertions.
 * <p>
 * Test organization:
 * - Create operations (createTask)
 * - Read operations (findTaskById, getTaskById, getAllTasks)
 * - Update operations (updateTask, assignToProject, removeFromProject)
 * - Delete operations (deleteTask)
 * - Filter operations (getTasksByUser, getTasksByProject, getTasksByStatus, etc.)
 * - Date-based queries (getOverdueTasks, getTasksDueBetween)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    // Test data
    private AppUser testUser;
    private Project testProject;
    private Task testTask;
    private TaskCreateDto createDto;
    private TaskUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");

        // Set up test project
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setAppUser(testUser);

        // Set up test task
        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus(TaskStatus.TODO);
        testTask.setDueDate(LocalDateTime.now().plusDays(7));
        testTask.setAppUser(testUser);
        testTask.setProject(testProject);

        // Set up create DTO
        createDto = TaskCreateDto.builder()
                .title("New Task")
                .description("New Description")
                .dueDate(LocalDateTime.now().plusDays(3))
                .appUserId(1L)
                .projectId(1L)
                .build();

        // Set up update DTO
        updateDto = TaskUpdateDto.builder()
                .title("Updated Task")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(LocalDateTime.now().plusDays(5))
                .build();
    }

    // ==================== CREATE OPERATIONS ====================

    @Nested
    @DisplayName("Create Task Tests")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task with all fields successfully")
        void shouldCreateTaskWithAllFields() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);

            // Act
            TaskResponseDto result = taskService.createTask(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(testTask.getTitle());
            assertThat(result.getDescription()).isEqualTo(testTask.getDescription());
            assertThat(result.getStatus()).isEqualTo(testTask.getStatus());
            assertThat(result.getAppUserId()).isEqualTo(testUser.getId());
            assertThat(result.getProjectId()).isEqualTo(testProject.getId());

            verify(appUserRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should create task without project (project is optional)")
        void shouldCreateTaskWithoutProject() {
            // Arrange
            createDto.setProjectId(null);
            Task taskWithoutProject = new Task();
            taskWithoutProject.setId(1L);
            taskWithoutProject.setTitle(createDto.getTitle());
            taskWithoutProject.setDescription(createDto.getDescription());
            taskWithoutProject.setAppUser(testUser);
            taskWithoutProject.setProject(null);

            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutProject);

            // Act
            TaskResponseDto result = taskService.createTask(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isNull();

            verify(appUserRepository).findById(1L);
            verify(projectRepository, never()).findById(anyLong());
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should create task without due date (due date is optional)")
        void shouldCreateTaskWithoutDueDate() {
            // Arrange
            createDto.setDueDate(null);
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);

            // Act
            TaskResponseDto result = taskService.createTask(createDto);

            // Assert
            assertThat(result).isNotNull();
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.createTask(createDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 1");

            verify(appUserRepository).findById(1L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void shouldThrowExceptionWhenProjectNotFound() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.createTask(createDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found with id: 1");

            verify(appUserRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when project doesn't belong to user")
        void shouldThrowExceptionWhenProjectDoesntBelongToUser() {
            // Arrange
            AppUser differentUser = new AppUser();
            differentUser.setId(2L);
            differentUser.setUsername("otheruser");
            testProject.setAppUser(differentUser);

            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> taskService.createTask(createDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Project does not belong to user");

            verify(appUserRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should set default status to TODO when creating task")
        void shouldSetDefaultStatusToTodo() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task savedTask = invocation.getArgument(0);
                assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.TODO);
                return testTask;
            });

            // Act
            taskService.createTask(createDto);

            // Assert
            verify(taskRepository).save(any(Task.class));
        }
    }

    // ==================== READ OPERATIONS ====================

    @Nested
    @DisplayName("Read Task Tests")
    class ReadTaskTests {

        @Test
        @DisplayName("Should find task by ID (returns Optional)")
        void shouldFindTaskById() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // Act
            Optional<TaskResponseDto> result = taskService.findTaskById(1L);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getTitle()).isEqualTo(testTask.getTitle());

            verify(taskRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty Optional when task not found by ID")
        void shouldReturnEmptyWhenTaskNotFound() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<TaskResponseDto> result = taskService.findTaskById(99L);

            // Assert
            assertThat(result).isEmpty();
            verify(taskRepository).findById(99L);
        }

        @Test
        @DisplayName("Should get task by ID (throws exception)")
        void shouldGetTaskById() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // Act
            TaskResponseDto result = taskService.getTaskById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(taskRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent task")
        void shouldThrowExceptionWhenGettingNonExistentTask() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: 99");

            verify(taskRepository).findById(99L);
        }

        @Test
        @DisplayName("Should get all tasks")
        void shouldGetAllTasks() {
            // Arrange
            Task task2 = new Task();
            task2.setId(2L);
            task2.setTitle("Task 2");
            task2.setAppUser(testUser);

            when(taskRepository.findAll()).thenReturn(List.of(testTask, task2));

            // Act
            List<TaskResponseDto> results = taskService.getAllTasks();

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TaskResponseDto::getId)
                    .containsExactly(1L, 2L);

            verify(taskRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void shouldReturnEmptyListWhenNoTasks() {
            // Arrange
            when(taskRepository.findAll()).thenReturn(List.of());

            // Act
            List<TaskResponseDto> results = taskService.getAllTasks();

            // Assert
            assertThat(results).isEmpty();
            verify(taskRepository).findAll();
        }
    }

    // ==================== UPDATE OPERATIONS ====================

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update all task fields")
        void shouldUpdateAllTaskFields() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TaskResponseDto result = taskService.updateTask(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(updateDto.getTitle());
            assertThat(result.getDescription()).isEqualTo(updateDto.getDescription());
            assertThat(result.getStatus()).isEqualTo(updateDto.getStatus());
            assertThat(result.getDueDate()).isEqualTo(updateDto.getDueDate());

            verify(taskRepository).findById(1L);
            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("Should update only provided fields (partial update)")
        void shouldUpdateOnlyProvidedFields() {
            // Arrange
            String originalTitle = testTask.getTitle();
            updateDto.setTitle(null);  // Don't update title
            updateDto.setDescription(null);  // Don't update description

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TaskResponseDto result = taskService.updateTask(1L, updateDto);

            // Assert
            assertThat(result.getTitle()).isEqualTo(originalTitle);  // Unchanged
            assertThat(result.getStatus()).isEqualTo(updateDto.getStatus());  // Changed

            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent task")
        void shouldThrowExceptionWhenUpdatingNonExistentTask() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.updateTask(99L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: 99");

            verify(taskRepository).findById(99L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should assign task to project")
        void shouldAssignTaskToProject() {
            // Arrange
            testTask.setProject(null);  // Start with no project
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TaskResponseDto result = taskService.assignToProject(1L, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isEqualTo(1L);
            assertThat(testTask.getProject()).isEqualTo(testProject);

            verify(taskRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("Should throw exception when assigning to non-existent project")
        void shouldThrowExceptionWhenAssigningToNonExistentProject() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.assignToProject(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found with id: 99");

            verify(taskRepository).findById(1L);
            verify(projectRepository).findById(99L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when assigning to project of different user")
        void shouldThrowExceptionWhenAssigningToProjectOfDifferentUser() {
            // Arrange
            AppUser differentUser = new AppUser();
            differentUser.setId(2L);
            testProject.setAppUser(differentUser);

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> taskService.assignToProject(1L, 1L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Project does not belong to the same user as the task");

            verify(taskRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should remove task from project")
        void shouldRemoveTaskFromProject() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TaskResponseDto result = taskService.removeFromProject(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isNull();
            assertThat(testTask.getProject()).isNull();

            verify(taskRepository).findById(1L);
            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent task from project")
        void shouldThrowExceptionWhenRemovingNonExistentTaskFromProject() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.removeFromProject(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: 99");

            verify(taskRepository).findById(99L);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task successfully")
        void shouldDeleteTask() {
            // Arrange
            when(taskRepository.existsById(1L)).thenReturn(true);
            doNothing().when(taskRepository).deleteById(1L);

            // Act
            taskService.deleteTask(1L);

            // Assert
            verify(taskRepository).existsById(1L);
            verify(taskRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent task")
        void shouldThrowExceptionWhenDeletingNonExistentTask() {
            // Arrange
            when(taskRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> taskService.deleteTask(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: 99");

            verify(taskRepository).existsById(99L);
            verify(taskRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== FILTER OPERATIONS ====================

    @Nested
    @DisplayName("Filter Tasks Tests")
    class FilterTasksTests {

        @Test
        @DisplayName("Should get tasks by user ID")
        void shouldGetTasksByUserId() {
            // Arrange
            Task task2 = new Task();
            task2.setId(2L);
            task2.setTitle("Task 2");
            task2.setAppUser(testUser);

            when(taskRepository.findByAppUserId(1L)).thenReturn(List.of(testTask, task2));

            // Act
            List<TaskResponseDto> results = taskService.getTasksByUserId(1L);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TaskResponseDto::getAppUserId)
                    .containsOnly(1L);

            verify(taskRepository).findByAppUserId(1L);
        }

        @Test
        @DisplayName("Should get tasks by project ID")
        void shouldGetTasksByProjectId() {
            // Arrange
            Task task2 = new Task();
            task2.setId(2L);
            task2.setTitle("Task 2");
            task2.setAppUser(testUser);
            task2.setProject(testProject);

            when(taskRepository.findByProjectId(1L)).thenReturn(List.of(testTask, task2));

            // Act
            List<TaskResponseDto> results = taskService.getTasksByProjectId(1L);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TaskResponseDto::getProjectId)
                    .containsOnly(1L);

            verify(taskRepository).findByProjectId(1L);
        }

        @Test
        @DisplayName("Should get tasks by status")
        void shouldGetTasksByStatus() {
            // Arrange
            when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(List.of(testTask));

            // Act
            List<TaskResponseDto> results = taskService.getTasksByStatus(TaskStatus.TODO);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(TaskStatus.TODO);

            verify(taskRepository).findByStatus(TaskStatus.TODO);
        }

        @Test
        @DisplayName("Should get tasks by user and status")
        void shouldGetTasksByUserIdAndStatus() {
            // Arrange
            when(taskRepository.findByAppUserIdAndStatus(1L, TaskStatus.TODO))
                    .thenReturn(List.of(testTask));

            // Act
            List<TaskResponseDto> results = taskService.getTasksByUserIdAndStatus(1L, TaskStatus.TODO);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAppUserId()).isEqualTo(1L);
            assertThat(results.get(0).getStatus()).isEqualTo(TaskStatus.TODO);

            verify(taskRepository).findByAppUserIdAndStatus(1L, TaskStatus.TODO);
        }

        @Test
        @DisplayName("Should get tasks by project and status")
        void shouldGetTasksByProjectIdAndStatus() {
            // Arrange
            when(taskRepository.findByProjectIdAndStatus(1L, TaskStatus.TODO))
                    .thenReturn(List.of(testTask));

            // Act
            List<TaskResponseDto> results = taskService.getTasksByProjectIdAndStatus(1L, TaskStatus.TODO);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getProjectId()).isEqualTo(1L);
            assertThat(results.get(0).getStatus()).isEqualTo(TaskStatus.TODO);

            verify(taskRepository).findByProjectIdAndStatus(1L, TaskStatus.TODO);
        }

        @Test
        @DisplayName("Should return empty list when no tasks match filters")
        void shouldReturnEmptyListWhenNoTasksMatchFilters() {
            // Arrange
            when(taskRepository.findByStatus(TaskStatus.COMPLETED)).thenReturn(List.of());

            // Act
            List<TaskResponseDto> results = taskService.getTasksByStatus(TaskStatus.COMPLETED);

            // Assert
            assertThat(results).isEmpty();
            verify(taskRepository).findByStatus(TaskStatus.COMPLETED);
        }
    }

    // ==================== DATE-BASED QUERIES ====================

    @Nested
    @DisplayName("Date-Based Query Tests")
    class DateBasedQueryTests {

        @Test
        @DisplayName("Should get overdue tasks")
        void shouldGetOverdueTasks() {
            // Arrange
            Task overdueTask = new Task();
            overdueTask.setId(2L);
            overdueTask.setTitle("Overdue Task");
            overdueTask.setDueDate(LocalDateTime.now().minusDays(1));
            overdueTask.setStatus(TaskStatus.TODO);
            overdueTask.setAppUser(testUser);

            when(taskRepository.findByDueDateBeforeAndStatusNotIn(
                    any(LocalDateTime.class),
                    eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(List.of(overdueTask));

            // Act
            List<TaskResponseDto> results = taskService.getOverdueTasks();

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(2L);

            verify(taskRepository).findByDueDateBeforeAndStatusNotIn(
                    any(LocalDateTime.class),
                    eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            );
        }

        @Test
        @DisplayName("Should get overdue tasks for specific user")
        void shouldGetOverdueTasksForUser() {
            // Arrange
            Task overdueTask = new Task();
            overdueTask.setId(2L);
            overdueTask.setTitle("Overdue Task");
            overdueTask.setDueDate(LocalDateTime.now().minusDays(1));
            overdueTask.setStatus(TaskStatus.TODO);
            overdueTask.setAppUser(testUser);

            when(taskRepository.findByAppUserIdAndDueDateBeforeAndStatusNotIn(
                    eq(1L),
                    any(LocalDateTime.class),
                    eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(List.of(overdueTask));

            // Act
            List<TaskResponseDto> results = taskService.getOverdueTasksForUser(1L);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAppUserId()).isEqualTo(1L);

            verify(taskRepository).findByAppUserIdAndDueDateBeforeAndStatusNotIn(
                    eq(1L),
                    any(LocalDateTime.class),
                    eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            );
        }

        @Test
        @DisplayName("Should get tasks due between dates")
        void shouldGetTasksDueBetween() {
            // Arrange
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = LocalDateTime.now().plusDays(7);

            when(taskRepository.findByDueDateBetween(startDate, endDate))
                    .thenReturn(List.of(testTask));

            // Act
            List<TaskResponseDto> results = taskService.getTasksDueBetween(startDate, endDate);

            // Assert
            assertThat(results).hasSize(1);
            verify(taskRepository).findByDueDateBetween(startDate, endDate);
        }

        @Test
        @DisplayName("Should return empty list when no overdue tasks")
        void shouldReturnEmptyListWhenNoOverdueTasks() {
            // Arrange
            when(taskRepository.findByDueDateBeforeAndStatusNotIn(
                    any(LocalDateTime.class),
                    eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(List.of());

            // Act
            List<TaskResponseDto> results = taskService.getOverdueTasks();

            // Assert
            assertThat(results).isEmpty();
        }
    }
}
