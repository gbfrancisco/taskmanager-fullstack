package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
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
 * Comprehensive unit tests for TaskService.
 * <p>
 * Tests all CRUD operations, validation logic, filtering, and business rules.
 * Uses Mockito to mock repository and mapper dependencies and AssertJ for assertions.
 * <p>
 * Test organization:
 * - Create operations (createTask) - returns TaskResponseDto
 * - Read operations (findById, getById, findAll) - returns TaskResponseDto
 * - Update operations (updateTask, assignToProject, removeFromProject) - returns TaskResponseDto
 * - Delete operations (deleteTask)
 * - Filter operations (findByUserId, findByProjectId, findByStatus, etc.) - returns TaskResponseDto
 * - Date-based queries (findOverdueTasks, findTasksDueBetween) - returns TaskResponseDto
 * <p>
 * Note: Services return DTOs for public API. Controllers pass DTOs to clients.
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
    private Project testProject;
    private Task testTask;
    private TaskResponseDto testTaskResponseDto;
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

        // Set up test task response DTO
        testTaskResponseDto = TaskResponseDto.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .dueDate(testTask.getDueDate())
                .appUserId(1L)
                .projectId(1L)
                .build();

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
            when(taskMapper.toEntity(createDto)).thenReturn(testTask);
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

            // Act
            TaskResponseDto result = taskService.createTask(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(testTaskResponseDto.getTitle());
            assertThat(result.getDescription()).isEqualTo(testTaskResponseDto.getDescription());
            assertThat(result.getStatus()).isEqualTo(testTaskResponseDto.getStatus());
            assertThat(result.getAppUserId()).isEqualTo(1L);
            assertThat(result.getProjectId()).isEqualTo(1L);

            verify(appUserRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskMapper).toEntity(createDto);
            verify(taskRepository).save(any(Task.class));
            verify(taskMapper).toResponseDto(testTask);
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

            TaskResponseDto responseDtoWithoutProject = TaskResponseDto.builder()
                .id(1L)
                .title(createDto.getTitle())
                .description(createDto.getDescription())
                .appUserId(1L)
                .projectId(null)
                .build();

            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskMapper.toEntity(createDto)).thenReturn(taskWithoutProject);
            when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutProject);
            when(taskMapper.toResponseDto(taskWithoutProject)).thenReturn(responseDtoWithoutProject);

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
            when(taskMapper.toEntity(createDto)).thenReturn(testTask);
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

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
                .hasMessageContaining("appUser with id '1' not found");

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
                .hasMessageContaining("project with id '1' not found");

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
            when(taskMapper.toEntity(createDto)).thenReturn(testTask);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task savedTask = invocation.getArgument(0);
                assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.TODO);
                return testTask;
            });
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

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
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

            // Act
            Optional<TaskResponseDto> result = taskService.findById(1L);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getTitle()).isEqualTo(testTaskResponseDto.getTitle());

            verify(taskRepository).findById(1L);
            verify(taskMapper).toResponseDto(testTask);
        }

        @Test
        @DisplayName("Should return empty Optional when task not found by ID")
        void shouldReturnEmptyWhenTaskNotFound() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<TaskResponseDto> result = taskService.findById(99L);

            // Assert
            assertThat(result).isEmpty();
            verify(taskRepository).findById(99L);
        }

        @Test
        @DisplayName("Should throw exception when getting optional task but id is null")
        void shouldThrowExceptionWhenGettingOptionalTaskButIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> taskService.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id is null");

            verify(taskRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should get task by ID (throws exception)")
        void shouldGetTaskById() {
            // Arrange
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskMapper.toResponseDto(testTask)).thenReturn(testTaskResponseDto);

            // Act
            TaskResponseDto result = taskService.getById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(taskRepository).findById(1L);
            verify(taskMapper).toResponseDto(testTask);
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent task")
        void shouldThrowExceptionWhenGettingNonExistentTask() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("task with id '99' not found");

            verify(taskRepository).findById(99L);
        }

        @Test
        @DisplayName("Should throw exception when getting task but id is null")
        void shouldThrowExceptionWhenGettingTaskButIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> taskService.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id is null");

            verify(taskRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should get all tasks")
        void shouldFindAllTasks() {
            // Arrange
            Task task2 = new Task();
            task2.setId(2L);
            task2.setTitle("Task 2");
            task2.setAppUser(testUser);

            TaskResponseDto task2ResponseDto = TaskResponseDto.builder()
                .id(2L)
                .title("Task 2")
                .appUserId(1L)
                .build();

            List<Task> tasks = List.of(testTask, task2);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto, task2ResponseDto);

            when(taskRepository.findAll()).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findAll();

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TaskResponseDto::getId)
                .containsExactly(1L, 2L);

            verify(taskRepository).findAll();
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void shouldReturnEmptyListWhenNoTasks() {
            // Arrange
            List<Task> emptyList = List.of();
            when(taskRepository.findAll()).thenReturn(emptyList);
            when(taskMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<TaskResponseDto> results = taskService.findAll();

            // Assert
            assertThat(results).isEmpty();
            verify(taskRepository).findAll();
            verify(taskMapper).toResponseDtoList(emptyList);
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
            TaskResponseDto updatedResponseDto = TaskResponseDto.builder()
                .id(1L)
                .title(updateDto.getTitle())
                .description(updateDto.getDescription())
                .status(updateDto.getStatus())
                .dueDate(updateDto.getDueDate())
                .appUserId(1L)
                .projectId(1L)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(updatedResponseDto);

            // Act
            TaskResponseDto result = taskService.updateTask(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(updateDto.getTitle());
            assertThat(result.getDescription()).isEqualTo(updateDto.getDescription());
            assertThat(result.getStatus()).isEqualTo(updateDto.getStatus());
            assertThat(result.getDueDate()).isEqualTo(updateDto.getDueDate());

            verify(taskRepository).findById(1L);
            verify(taskMapper).updateEntityFromDto(updateDto, testTask);
            verify(taskRepository).save(testTask);
            verify(taskMapper).toResponseDto(testTask);
        }
        @Test
        @DisplayName("Should update only provided fields (partial update)")
        void shouldUpdateOnlyProvidedFields() {
            // Arrange
            updateDto.setTitle(null);  // Don't update title
            updateDto.setDescription(null);  // Don't update description

            TaskResponseDto partiallyUpdatedDto = TaskResponseDto.builder()
                .id(1L)
                .title(testTask.getTitle())  // Original title
                .description(testTask.getDescription())  // Original description
                .status(updateDto.getStatus())  // Updated status
                .dueDate(updateDto.getDueDate())
                .appUserId(1L)
                .projectId(1L)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(partiallyUpdatedDto);

            // Act
            TaskResponseDto result = taskService.updateTask(1L, updateDto);

            // Assert
            assertThat(result.getTitle()).isEqualTo(testTask.getTitle());  // Unchanged
            assertThat(result.getStatus()).isEqualTo(updateDto.getStatus());  // Changed

            verify(taskMapper).updateEntityFromDto(updateDto, testTask);
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
                .hasMessageContaining("task with id '99' not found");

            verify(taskRepository).findById(99L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("Should assign task to project")
        void shouldAssignTaskToProject() {
            // Arrange
            testTask.setProject(null);  // Start with no project
            TaskResponseDto assignedResponseDto = TaskResponseDto.builder()
                .id(1L)
                .title(testTask.getTitle())
                .appUserId(1L)
                .projectId(1L)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(assignedResponseDto);

            // Act
            TaskResponseDto result = taskService.assignToProject(1L, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isEqualTo(1L);

            verify(taskRepository).findById(1L);
            verify(projectRepository).findById(1L);
            verify(taskRepository).save(testTask);
            verify(taskMapper).toResponseDto(testTask);
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
                .hasMessageContaining("project with id '99' not found");

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
            TaskResponseDto removedFromProjectDto = TaskResponseDto.builder()
                .id(1L)
                .title(testTask.getTitle())
                .appUserId(1L)
                .projectId(null)
                .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(taskMapper.toResponseDto(testTask)).thenReturn(removedFromProjectDto);

            // Act
            TaskResponseDto result = taskService.removeFromProject(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isNull();

            verify(taskRepository).findById(1L);
            verify(taskRepository).save(testTask);
            verify(taskMapper).toResponseDto(testTask);
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent task from project")
        void shouldThrowExceptionWhenRemovingNonExistentTaskFromProject() {
            // Arrange
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.removeFromProject(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("task with id '99' not found");

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
                .hasMessageContaining("task with id '99' not found");

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
        void shouldFindTasksByUserId() {
            // Arrange
            Task task2 = new Task();
            task2.setId(2L);
            task2.setTitle("Task 2");
            task2.setAppUser(testUser);

            TaskResponseDto task2ResponseDto = TaskResponseDto.builder()
                .id(2L)
                .title("Task 2")
                .appUserId(1L)
                .build();

            List<Task> tasks = List.of(testTask, task2);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto, task2ResponseDto);

            when(taskRepository.findByAppUserId(1L)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByAppUserId(1L);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TaskResponseDto::getAppUserId)
                .containsOnly(1L);

            verify(taskRepository).findByAppUserId(1L);
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should get tasks by project ID")
        void shouldFindTasksByProjectId() {
            // Arrange
            Task task2 = new Task();
            task2.setId(2L);
            task2.setTitle("Task 2");
            task2.setAppUser(testUser);
            task2.setProject(testProject);

            TaskResponseDto task2ResponseDto = TaskResponseDto.builder()
                .id(2L)
                .title("Task 2")
                .appUserId(1L)
                .projectId(1L)
                .build();

            List<Task> tasks = List.of(testTask, task2);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto, task2ResponseDto);

            when(taskRepository.findByProjectId(1L)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByProjectId(1L);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TaskResponseDto::getProjectId)
                .containsOnly(1L);

            verify(taskRepository).findByProjectId(1L);
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should get tasks by status")
        void shouldFindTasksByStatus() {
            // Arrange
            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByStatus(TaskStatus.TODO);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getStatus()).isEqualTo(TaskStatus.TODO);

            verify(taskRepository).findByStatus(TaskStatus.TODO);
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should get tasks by user and status")
        void shouldFindTasksByUserIdAndStatus() {
            // Arrange
            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(taskRepository.findByAppUserIdAndStatus(1L, TaskStatus.TODO)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByAppUserIdAndStatus(1L, TaskStatus.TODO);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getAppUserId()).isEqualTo(1L);
            assertThat(results.getFirst().getStatus()).isEqualTo(TaskStatus.TODO);

            verify(taskRepository).findByAppUserIdAndStatus(1L, TaskStatus.TODO);
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should get tasks by project and status")
        void shouldFindTasksByProjectIdAndStatus() {
            // Arrange
            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(taskRepository.findByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByProjectIdAndStatus(1L, TaskStatus.TODO);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getProjectId()).isEqualTo(1L);
            assertThat(results.getFirst().getStatus()).isEqualTo(TaskStatus.TODO);

            verify(taskRepository).findByProjectIdAndStatus(1L, TaskStatus.TODO);
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should return empty list when no tasks match filters")
        void shouldReturnEmptyListWhenNoTasksMatchFilters() {
            // Arrange
            List<Task> emptyList = List.of();
            when(taskRepository.findByStatus(TaskStatus.COMPLETED)).thenReturn(emptyList);
            when(taskMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<TaskResponseDto> results = taskService.findByStatus(TaskStatus.COMPLETED);

            // Assert
            assertThat(results).isEmpty();
            verify(taskRepository).findByStatus(TaskStatus.COMPLETED);
            verify(taskMapper).toResponseDtoList(emptyList);
        }
    }

    // ==================== DATE-BASED QUERIES ====================

    @Nested
    @DisplayName("Date-Based Query Tests")
    class DateBasedQueryTests {

        @Test
        @DisplayName("Should get overdue tasks")
        void shouldFindOverdueTasks() {
            // Arrange
            Task overdueTask = new Task();
            overdueTask.setId(2L);
            overdueTask.setTitle("Overdue Task");
            overdueTask.setDueDate(LocalDateTime.now().minusDays(1));
            overdueTask.setStatus(TaskStatus.TODO);
            overdueTask.setAppUser(testUser);

            TaskResponseDto overdueTaskResponseDto = TaskResponseDto.builder()
                .id(2L)
                .title("Overdue Task")
                .status(TaskStatus.TODO)
                .appUserId(1L)
                .build();

            List<Task> overdueTasks = List.of(overdueTask);
            List<TaskResponseDto> responseDtos = List.of(overdueTaskResponseDto);

            when(taskRepository.findByDueDateBeforeAndStatusNotIn(
                any(LocalDateTime.class),
                eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(overdueTasks);
            when(taskMapper.toResponseDtoList(overdueTasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findOverdueTasks();

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getId()).isEqualTo(2L);

            verify(taskRepository).findByDueDateBeforeAndStatusNotIn(
                any(LocalDateTime.class),
                eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            );
            verify(taskMapper).toResponseDtoList(overdueTasks);
        }

        @Test
        @DisplayName("Should get overdue tasks for specific user")
        void shouldFindOverdueTasksForUser() {
            // Arrange
            Task overdueTask = new Task();
            overdueTask.setId(2L);
            overdueTask.setTitle("Overdue Task");
            overdueTask.setDueDate(LocalDateTime.now().minusDays(1));
            overdueTask.setStatus(TaskStatus.TODO);
            overdueTask.setAppUser(testUser);

            TaskResponseDto overdueTaskResponseDto = TaskResponseDto.builder()
                .id(2L)
                .title("Overdue Task")
                .status(TaskStatus.TODO)
                .appUserId(1L)
                .build();

            List<Task> overdueTasks = List.of(overdueTask);
            List<TaskResponseDto> responseDtos = List.of(overdueTaskResponseDto);

            when(taskRepository.findByAppUserIdAndDueDateBeforeAndStatusNotIn(
                eq(1L),
                any(LocalDateTime.class),
                eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(overdueTasks);
            when(taskMapper.toResponseDtoList(overdueTasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findOverdueTasksByAppUserId(1L);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getAppUserId()).isEqualTo(1L);

            verify(taskRepository).findByAppUserIdAndDueDateBeforeAndStatusNotIn(
                eq(1L),
                any(LocalDateTime.class),
                eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            );
            verify(taskMapper).toResponseDtoList(overdueTasks);
        }

        @Test
        @DisplayName("Should get tasks due between dates")
        void shouldFindTasksDueBetween() {
            // Arrange
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = LocalDateTime.now().plusDays(7);

            List<Task> tasks = List.of(testTask);
            List<TaskResponseDto> responseDtos = List.of(testTaskResponseDto);

            when(taskRepository.findByDueDateBetween(startDate, endDate)).thenReturn(tasks);
            when(taskMapper.toResponseDtoList(tasks)).thenReturn(responseDtos);

            // Act
            List<TaskResponseDto> results = taskService.findByDueDateBetween(startDate, endDate);

            // Assert
            assertThat(results).hasSize(1);
            verify(taskRepository).findByDueDateBetween(startDate, endDate);
            verify(taskMapper).toResponseDtoList(tasks);
        }

        @Test
        @DisplayName("Should return empty list when no overdue tasks")
        void shouldReturnEmptyListWhenNoOverdueTasks() {
            // Arrange
            List<Task> emptyList = List.of();
            when(taskRepository.findByDueDateBeforeAndStatusNotIn(
                any(LocalDateTime.class),
                eq(List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED))
            )).thenReturn(emptyList);
            when(taskMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<TaskResponseDto> results = taskService.findOverdueTasks();

            // Assert
            assertThat(results).isEmpty();
            verify(taskMapper).toResponseDtoList(emptyList);
        }
    }
}
