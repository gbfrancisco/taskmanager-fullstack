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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service layer for Task operations.
 *
 * <p><strong>Authorization:</strong>
 * All methods require an authenticated user ID. Data is automatically scoped
 * to the authenticated user - users can only access their own tasks.
 *
 * <p>Ownership is validated on every operation:
 * <ul>
 *   <li>Create: Task is assigned to authenticated user</li>
 *   <li>Read: Only returns tasks owned by authenticated user</li>
 *   <li>Update/Delete: Validates task belongs to authenticated user</li>
 * </ul>
 */
@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;

    public TaskService(
        TaskRepository taskRepository,
        AppUserRepository appUserRepository,
        ProjectRepository projectRepository,
        TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.taskMapper = taskMapper;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Create a new task for the authenticated user.
     *
     * @param taskCreateDto Task data
     * @param userId ID of the authenticated user (from JWT)
     * @return Created task
     * @throws ResourceNotFoundException if user or project not found
     * @throws ValidationException if project doesn't belong to user
     */
    public TaskResponseDto createTask(TaskCreateDto taskCreateDto, Long userId) {
        if (taskCreateDto == null) {
            throw new IllegalArgumentException("taskCreateDto is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        AppUser appUser = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", userId));

        Project project = null;
        Long projectId = taskCreateDto.getProjectId();
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

            // Verify project belongs to the same user
            if (!Objects.equals(userId, project.getAppUser().getId())) {
                throw new ValidationException("Project does not belong to authenticated user");
            }
        }

        Task taskToSave = taskMapper.toEntity(taskCreateDto);
        taskToSave.setAppUser(appUser);
        taskToSave.setProject(project);
        taskToSave = taskRepository.save(taskToSave);
        return taskMapper.toResponseDto(taskToSave);
    }

    // =========================================================================
    // READ - Single Task
    // =========================================================================

    /**
     * Find a task by ID, with ownership check.
     *
     * @param taskId Task ID
     * @param userId ID of the authenticated user
     * @return Optional containing task if found and owned by user
     */
    @Transactional(readOnly = true)
    public Optional<TaskResponseDto> findById(Long taskId, Long userId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        return taskRepository.findWithAppUserAndProjectById(taskId)
            .filter(task -> Objects.equals(task.getAppUser().getId(), userId))
            .map(taskMapper::toResponseDto);
    }

    /**
     * Get a task by ID, with ownership check.
     *
     * @param taskId Task ID
     * @param userId ID of the authenticated user
     * @return Task data
     * @throws ResourceNotFoundException if task not found
     * @throws ValidationException if task doesn't belong to user
     */
    @Transactional(readOnly = true)
    public TaskResponseDto getById(Long taskId, Long userId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Task task = taskRepository.findWithAppUserAndProjectById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));

        validateOwnership(task, userId);
        return taskMapper.toResponseDto(task);
    }

    // =========================================================================
    // READ - Lists (All scoped to authenticated user)
    // =========================================================================

    /**
     * Get all tasks for the authenticated user.
     *
     * @param userId ID of the authenticated user
     * @return List of user's tasks
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findAll(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        List<Task> tasks = taskRepository.findWithAppUserAndProjectByAppUserId(userId);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Get tasks by project ID, with project ownership check.
     *
     * @param projectId Project ID
     * @param userId ID of the authenticated user
     * @return List of tasks in the project
     * @throws ResourceNotFoundException if project not found
     * @throws ValidationException if project doesn't belong to user
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByProjectId(Long projectId, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        // Verify project ownership
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        if (!Objects.equals(project.getAppUser().getId(), userId)) {
            throw new ValidationException("Project does not belong to authenticated user");
        }

        List<Task> tasks = taskRepository.findWithAppUserAndProjectByProjectId(projectId);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Get tasks by status for the authenticated user.
     *
     * @param status Task status
     * @param userId ID of the authenticated user
     * @return List of user's tasks with given status
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByStatus(TaskStatus status, Long userId) {
        if (status == null) {
            throw new IllegalArgumentException("status is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        List<Task> tasks = taskRepository.findByAppUserIdAndStatus(userId, status);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Get tasks by project and status, with project ownership check.
     *
     * @param projectId Project ID
     * @param status Task status
     * @param userId ID of the authenticated user
     * @return List of tasks in project with given status
     * @throws ResourceNotFoundException if project not found
     * @throws ValidationException if project doesn't belong to user
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByProjectIdAndStatus(Long projectId, TaskStatus status, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        // Verify project ownership
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        if (!Objects.equals(project.getAppUser().getId(), userId)) {
            throw new ValidationException("Project does not belong to authenticated user");
        }

        List<Task> tasks = taskRepository.findByProjectIdAndStatus(projectId, status);
        return taskMapper.toResponseDtoList(tasks);
    }

    /**
     * Get overdue tasks for the authenticated user.
     *
     * @param userId ID of the authenticated user
     * @return List of user's overdue tasks
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDto> findOverdueTasks(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        List<Task> overdueTasks = taskRepository.findByAppUserIdAndDueDateBeforeAndStatusNotIn(
            userId,
            LocalDateTime.now(),
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
        );
        return taskMapper.toResponseDtoList(overdueTasks);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Update a task, with ownership check.
     *
     * @param taskId Task ID
     * @param taskUpdateDto Updated task data
     * @param userId ID of the authenticated user
     * @return Updated task
     * @throws ResourceNotFoundException if task not found
     * @throws ValidationException if task doesn't belong to user
     */
    public TaskResponseDto updateTask(Long taskId, TaskUpdateDto taskUpdateDto, Long userId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is null");
        }
        if (taskUpdateDto == null) {
            throw new IllegalArgumentException("taskUpdateDto is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Task taskToUpdate = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));

        validateOwnership(taskToUpdate, userId);

        taskMapper.patchEntityFromDto(taskUpdateDto, taskToUpdate);
        taskToUpdate = taskRepository.save(taskToUpdate);
        return taskMapper.toResponseDto(taskToUpdate);
    }

    /**
     * Assign a task to a project, with ownership checks for both.
     *
     * @param taskId Task ID
     * @param projectId Project ID
     * @param userId ID of the authenticated user
     * @return Updated task
     * @throws ResourceNotFoundException if task or project not found
     * @throws ValidationException if either doesn't belong to user
     */
    public TaskResponseDto assignToProject(Long taskId, Long projectId, Long userId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is null");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        validateOwnership(task, userId);

        if (!Objects.equals(project.getAppUser().getId(), userId)) {
            throw new ValidationException("Project does not belong to authenticated user");
        }

        task.setProject(project);
        task = taskRepository.save(task);
        return taskMapper.toResponseDto(task);
    }

    /**
     * Remove a task from its project, with ownership check.
     *
     * @param taskId Task ID
     * @param userId ID of the authenticated user
     * @return Updated task
     * @throws ResourceNotFoundException if task not found
     * @throws ValidationException if task doesn't belong to user
     */
    public TaskResponseDto removeFromProject(Long taskId, Long userId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));

        validateOwnership(task, userId);

        task.setProject(null);
        task = taskRepository.save(task);
        return taskMapper.toResponseDto(task);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Delete a task, with ownership check.
     *
     * @param taskId Task ID
     * @param userId ID of the authenticated user
     * @throws ResourceNotFoundException if task not found
     * @throws ValidationException if task doesn't belong to user
     */
    public void deleteTask(Long taskId, Long userId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));

        validateOwnership(task, userId);

        taskRepository.deleteById(taskId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Validate that a task belongs to the given user.
     *
     * @param task The task to check
     * @param userId The user ID to validate against
     * @throws ValidationException if task doesn't belong to user
     */
    private void validateOwnership(Task task, Long userId) {
        if (!Objects.equals(task.getAppUser().getId(), userId)) {
            throw new ValidationException("Task does not belong to authenticated user");
        }
    }
}
