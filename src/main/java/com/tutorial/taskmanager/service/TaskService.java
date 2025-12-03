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

    public TaskResponseDto createTask(TaskCreateDto taskCreateDto) {
        if (taskCreateDto == null) {
            throw new IllegalArgumentException("taskCreateDto is null");
        }

        Long appUserId = taskCreateDto.getAppUserId();
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        AppUser appUser = appUserRepository.findById(appUserId)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", appUserId));

        Project project = null;
        Long projectId = taskCreateDto.getProjectId();
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

            if (!Objects.equals(appUser.getId(), project.getAppUser().getId())) {
                throw new ValidationException("Project does not belong to user");
            }
        }

        Task taskToSave = taskMapper.toEntity(taskCreateDto);
        taskToSave.setAppUser(appUser);
        taskToSave.setProject(project);
        taskToSave = taskRepository.save(taskToSave);
        return taskMapper.toResponseDto(taskToSave);
    }

    @Transactional(readOnly = true)
    public Optional<TaskResponseDto> findById(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("id is null");
        }

        return taskRepository.findById(taskId).map(taskMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public TaskResponseDto getById(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("id is null");
        }

        return taskRepository.findById(taskId)
            .map(taskMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findAll() {
        return taskMapper.toResponseDtoList(taskRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByAppUserId(Long appUserId) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        List<Task> tasks = taskRepository.findByAppUserId(appUserId);
        return taskMapper.toResponseDtoList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return taskMapper.toResponseDtoList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByStatus(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status is null");
        }

        List<Task> tasks = taskRepository.findByStatus(status);
        return taskMapper.toResponseDtoList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByAppUserIdAndStatus(Long appUserId, TaskStatus status) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status is null");
        }

        List<Task> tasks = taskRepository.findByAppUserIdAndStatus(appUserId, status);
        return taskMapper.toResponseDtoList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByProjectIdAndStatus(Long projectId, TaskStatus status) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status is null");
        }

        List<Task> tasks = taskRepository.findByProjectIdAndStatus(projectId, status);
        return taskMapper.toResponseDtoList(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findOverdueTasks() {
        List<Task> overdueTasks = taskRepository.findByDueDateBeforeAndStatusNotIn(
            LocalDateTime.now(),
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
        );
        return taskMapper.toResponseDtoList(overdueTasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findOverdueTasksByAppUserId(Long appUserId) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        List<Task> overdueTasks = taskRepository.findByAppUserIdAndDueDateBeforeAndStatusNotIn(
            appUserId,
            LocalDateTime.now(),
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
        );
        return taskMapper.toResponseDtoList(overdueTasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findByDueDateBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new IllegalArgumentException("start is null");
        }

        if (end == null) {
            throw new IllegalArgumentException("end is null");
        }

        List<Task> tasks = taskRepository.findByDueDateBetween(start, end);
        return taskMapper.toResponseDtoList(tasks);
    }

    public TaskResponseDto updateTask(Long taskId, TaskUpdateDto taskUpdateDto) {
        if (taskId == null) {
            throw new IllegalArgumentException("id is null");
        }

        if (taskUpdateDto == null) {
            throw new IllegalArgumentException("taskUpdateDto is null");
        }

        Task taskToUpdate = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));

        taskMapper.updateEntityFromDto(taskUpdateDto, taskToUpdate);
        taskToUpdate = taskRepository.save(taskToUpdate);
        return taskMapper.toResponseDto(taskToUpdate);
    }

    public TaskResponseDto assignToProject(Long taskId, Long projectId) {
        if (taskId == null) {
            throw new IllegalArgumentException("id is null");
        }

        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        if (!Objects.equals(task.getAppUser().getId(), project.getAppUser().getId())) {
            throw new ValidationException("Project does not belong to the same user as the task");
        }

        task.setProject(project);
        task = taskRepository.save(task);
        return taskMapper.toResponseDto(task);
    }

    public TaskResponseDto removeFromProject(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("id is null");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("task", taskId));
        task.setProject(null);
        task = taskRepository.save(task);
        return taskMapper.toResponseDto(task);
    }

    public void deleteTask(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        boolean doesTaskExist = taskRepository.existsById(id);
        if (!doesTaskExist) {
            throw new ResourceNotFoundException("task", id);
        }

        taskRepository.deleteById(id);
    }
}