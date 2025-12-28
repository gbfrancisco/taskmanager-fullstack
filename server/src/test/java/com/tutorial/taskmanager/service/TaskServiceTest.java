package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
import com.tutorial.taskmanager.dto.project.ProjectSummaryDto;
import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.exception.ValidationException;
import com.tutorial.taskmanager.mapper.TaskMapper;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TaskService with user-scoped authorization.
 * <p>
 * All service methods now require a userId parameter for authorization.
 * Tests verify both happy paths and authorization failure scenarios.
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

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    // Test data
    private AppUser testUser;
    private AppUser otherUser;
    private AppUserSummaryDto testUserSummary;
    private Project testProject;
    private ProjectSummaryDto testProjectSummary;
    private Task testTask;
    private TaskResponseDto testTaskResponseDto;
    private TaskCreateDto createDto;
    private TaskUpdateDto updateDto;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        // Set up test user (authenticated user)
        testUser = new AppUser();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");

        // Set up other user (for authorization failure tests)
        otherUser = new AppUser();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");

        // Set up test user summary (for response DTOs)
        testUserSummary = AppUserSummaryDto.builder()
                .id(USER_ID)
                .username("testuser")
                .build();

        // Set up test project (owned by testUser)
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setAppUser(testUser);

        // Set up test project summary (for response DTOs)
        testProjectSummary = ProjectSummaryDto.builder()
                .id(1L)
                .name("Test Project")
                .status(ProjectStatus.PLANNING)
                .build();

        // Set up test task (owned by testUser)
        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus(TaskStatus.TODO);
        testTask.setDueDate(LocalDateTime.now().plusDays(7));
        testTask.setAppUser(testUser);
        testTask.setProject(testProject);

        // Set up test task response DTO
        testTaskResponseDto = TaskResponseDto.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .dueDate(testTask.getDueDate())
                .appUser(testUserSummary)
                .project(testProjectSummary)
                .build();

        // Set up create DTO (no appUserId - extracted from JWT)
        createDto = TaskCreateDto.builder()
                .title("New Task")
                .description("New Description")
                .dueDate(LocalDateTime.now().plusDays(3))
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
        @DisplayName("Should create task for authenticated user")
        void shouldCreateTaskForAuthenticatedUser() {
            // Arrange
            when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskMapper.toEntity(createDto)).thenReturn(testTask);
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

            // Act
            TaskResponseDto result = taskService.createTask(createDto, USER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(testTaskResponseDto.getTitle());
            assertThat(result.getAppUser().getId()).isEqualTo(USER_ID);

            verify(appUserRepository).findById(USER_ID);
            verify(projectRepository).findById(1L);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should create task without project")
        void shouldCreateTaskWithoutProject() {
            // Arrange
            createDto.setProjectId(null);
            Task taskWithoutProject = new Task();
            taskWithoutProject.setId(1L);
            taskWithoutProject.setTitle(createDto.getTitle());
            taskWithoutProject.setAppUser(testUser);
            taskWithoutProject.setProject(null);

            TaskResponseDto responseDtoWithoutProject = TaskResponseDto.builder()
                .id(1L)
                .title(createDto.getTitle())
                .appUser(testUserSummary)
                .project(null)
                .build();

            when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(taskMapper.toEntity(createDto)).thenReturn(taskWithoutProject);
            when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutProject);
            when(taskMapper.toResponseDto(taskWithoutProject)).thenReturn(responseDtoWithoutProject);

            // Act
            TaskResponseDto result = taskService.createTask(createDto, USER_ID);

            // Assert
            assertThat(result.getProject()).isNull();
            verify(projectRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when project doesn't belong to user")
        void shouldThrowExceptionWhenProjectDoesntBelongToUser() {
            // Arrange
            testProject.setAppUser(otherUser); // Project belongs to different user

            when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> taskService.createTask(createDto, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Project does not belong to authenticated user");

            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> taskService.createTask(createDto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is null");
        }
    }

    // ==================== READ OPERATIONS ====================

    @Nested
    @DisplayName("Read Task Tests")
    class ReadTaskTests {

        @Test
        @DisplayName("Should find task by ID with ownership check")
        void shouldFindTaskByIdWithOwnershipCheck() {
            // Arrange
            when(taskRepository.findWithAppUserAndProjectById(1L)).thenReturn(Optional.of(testTask));
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

            // Act
            Optional<TaskResponseDto> result = taskService.findById(1L, USER_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            verify(taskRepository).findWithAppUserAndProjectById(1L);
        }

        @Test
        @DisplayName("Should return empty when task belongs to different user")
        void shouldReturnEmptyWhenTaskBelongsToDifferentUser() {
            // Arrange
            testTask.setAppUser(otherUser); // Task belongs to different user
            when(taskRepository.findWithAppUserAndProjectById(1L)).thenReturn(Optional.of(testTask));

            // Act
            Optional<TaskResponseDto> result = taskService.findById(1L, USER_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(taskMapper, never()).toResponseDto(any());
        }

        @Test
        @DisplayName("Should get task by ID with ownership validation")
        void shouldGetTaskByIdWithOwnershipValidation() {
            // Arrange
            when(taskRepository.findWithAppUserAndProjectById(1L)).thenReturn(Optional.of(testTask));
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

            // Act
            TaskResponseDto result = taskService.getById(1L, USER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when task doesn't belong to user")
        void shouldThrowExceptionWhenTaskDoesntBelongToUser() {
            // Arrange
            testTask.setAppUser(otherUser);
            when(taskRepository.findWithAppUserAndProjectById(1L)).thenReturn(Optional.of(testTask));

            // Act & Assert
            assertThatThrownBy(() -> taskService.getById(1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Task does not belong to authenticated user");
        }

        @Test
        @DisplayName("Should find all tasks for authenticated user only")
        void shouldFindAllTasksForAuthenticatedUser() {
            // Arrange
            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(taskRepository.findWithAppUserAndProjectByAppUserId(USER_ID)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findAll(USER_ID);

            // Assert
            assertThat(results).hasSize(1);
            verify(taskRepository).findWithAppUserAndProjectByAppUserId(USER_ID);
        }
    }

    // ==================== UPDATE OPERATIONS ====================

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task with ownership check")
        void shouldUpdateTaskWithOwnershipCheck() {
            // Arrange
            TaskResponseDto updatedResponseDto = TaskResponseDto.builder()
                .id(1L)
                .title(updateDto.getTitle())
                .status(updateDto.getStatus())
                .appUser(testUserSummary)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(updatedResponseDto);

            // Act
            TaskResponseDto result = taskService.updateTask(1L, updateDto, USER_ID);

            // Assert
            assertThat(result.getTitle()).isEqualTo(updateDto.getTitle());
            verify(taskMapper).patchEntityFromDto(updateDto, testTask);
            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("Should throw exception when updating task that doesn't belong to user")
        void shouldThrowExceptionWhenUpdatingUnownedTask() {
            // Arrange
            testTask.setAppUser(otherUser);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // Act & Assert
            assertThatThrownBy(() -> taskService.updateTask(1L, updateDto, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Task does not belong to authenticated user");

            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should assign task to project with ownership checks")
        void shouldAssignTaskToProjectWithOwnershipChecks() {
            // Arrange
            testTask.setProject(null);
            TaskResponseDto assignedResponseDto = TaskResponseDto.builder()
                .id(1L)
                .title(testTask.getTitle())
                .appUser(testUserSummary)
                .project(testProjectSummary)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(assignedResponseDto);

            // Act
            TaskResponseDto result = taskService.assignToProject(1L, 1L, USER_ID);

            // Assert
            assertThat(result.getProject()).isNotNull();
            verify(taskRepository).save(testTask);
        }

        @Test
        @DisplayName("Should throw when assigning unowned task to project")
        void shouldThrowWhenAssigningUnownedTaskToProject() {
            // Arrange
            testTask.setAppUser(otherUser);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> taskService.assignToProject(1L, 1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Task does not belong to authenticated user");
        }

        @Test
        @DisplayName("Should throw when assigning task to unowned project")
        void shouldThrowWhenAssigningTaskToUnownedProject() {
            // Arrange
            testProject.setAppUser(otherUser);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> taskService.assignToProject(1L, 1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Project does not belong to authenticated user");
        }

        @Test
        @DisplayName("Should remove task from project with ownership check")
        void shouldRemoveTaskFromProjectWithOwnershipCheck() {
            // Arrange
            TaskResponseDto removedFromProjectDto = TaskResponseDto.builder()
                .id(1L)
                .title(testTask.getTitle())
                .appUser(testUserSummary)
                .project(null)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(removedFromProjectDto);

            // Act
            TaskResponseDto result = taskService.removeFromProject(1L, USER_ID);

            // Assert
            assertThat(result.getProject()).isNull();
            verify(taskRepository).save(testTask);
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task with ownership check")
        void shouldDeleteTaskWithOwnershipCheck() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            doNothing().when(taskRepository).deleteById(1L);

            // Act
            taskService.deleteTask(1L, USER_ID);

            // Assert
            verify(taskRepository).findById(1L);
            verify(taskRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw when deleting task that doesn't belong to user")
        void shouldThrowWhenDeletingUnownedTask() {
            // Arrange
            testTask.setAppUser(otherUser);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // Act & Assert
            assertThatThrownBy(() -> taskService.deleteTask(1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Task does not belong to authenticated user");

            verify(taskRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== FILTER OPERATIONS ====================

    @Nested
    @DisplayName("Filter Tasks Tests")
    class FilterTasksTests {

        @Test
        @DisplayName("Should find tasks by project with ownership check")
        void shouldFindTasksByProjectWithOwnershipCheck() {
            // Arrange
            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.findWithAppUserAndProjectByProjectId(1L)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByProjectId(1L, USER_ID);

            // Assert
            assertThat(results).hasSize(1);
            verify(projectRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw when filtering by unowned project")
        void shouldThrowWhenFilteringByUnownedProject() {
            // Arrange
            testProject.setAppUser(otherUser);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> taskService.findByProjectId(1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Project does not belong to authenticated user");
        }

        @Test
        @DisplayName("Should find tasks by status for authenticated user")
        void shouldFindTasksByStatusForAuthenticatedUser() {
            // Arrange
            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(taskRepository.findByAppUserIdAndStatus(USER_ID, TaskStatus.TODO)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByStatus(TaskStatus.TODO, USER_ID);

            // Assert
            assertThat(results).hasSize(1);
            verify(taskRepository).findByAppUserIdAndStatus(USER_ID, TaskStatus.TODO);
        }

        @Test
        @DisplayName("Should find overdue tasks for authenticated user")
        void shouldFindOverdueTasksForAuthenticatedUser() {
            // Arrange
            Task overdueTask = new Task();
            overdueTask.setId(2L);
            overdueTask.setTitle("Overdue Task");
            overdueTask.setDueDate(LocalDateTime.now().minusDays(1));
            overdueTask.setStatus(TaskStatus.TODO);
            overdueTask.setAppUser(testUser);

            List<Task> overdueTasks = List.of(overdueTask);
            List<TaskResponseDto> responseDtos = List.of(
                TaskResponseDto.builder().id(2L).title("Overdue Task").build()
            );

            when(taskRepository.findByAppUserIdAndDueDateBeforeAndStatusNotIn(
                eq(USER_ID),
                any(LocalDateTime.class),
                eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(overdueTasks);
            when(taskMapper.toResponseDtoList(overdueTasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findOverdueTasks(USER_ID);

            // Assert
            assertThat(results).hasSize(1);
        }
    }
}
