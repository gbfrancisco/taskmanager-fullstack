package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.project.ProjectCreateDto;
import com.tutorial.taskmanager.dto.project.ProjectResponseDto;
import com.tutorial.taskmanager.dto.project.ProjectUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.exception.ValidationException;
import com.tutorial.taskmanager.mapper.ProjectMapper;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.repository.AppUserRepository;
import com.tutorial.taskmanager.repository.ProjectRepository;
import com.tutorial.taskmanager.repository.TaskRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Project operations.
 *
 * <p><strong>Authorization:</strong>
 * All methods require an authenticated user ID. Data is automatically scoped
 * to the authenticated user - users can only access their own projects.
 *
 * <p>Ownership is validated on every operation:
 * <ul>
 *   <li>Create: Project is assigned to authenticated user</li>
 *   <li>Read: Only returns projects owned by authenticated user</li>
 *   <li>Update/Delete: Validates project belongs to authenticated user</li>
 * </ul>
 */
@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            AppUserRepository appUserRepository,
            TaskRepository taskRepository,
            ProjectMapper projectMapper
    ) {
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
        this.taskRepository = taskRepository;
        this.projectMapper = projectMapper;
    }

    // =========================================================================
    // TASK COUNT ENRICHMENT
    // =========================================================================

    /**
     * Enrich a single DTO with task count.
     * Uses a single COUNT query - efficient for single project lookups.
     */
    private ProjectResponseDto enrichWithTaskCount(ProjectResponseDto dto) {
        if (dto != null && dto.getId() != null) {
            dto.setTaskCount(taskRepository.countByProjectId(dto.getId()));
        }
        return dto;
    }

    /**
     * Enrich multiple DTOs with task counts using a single batch query.
     * Avoids N+1 problem when fetching many projects.
     */
    private List<ProjectResponseDto> enrichWithTaskCounts(List<ProjectResponseDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return dtos;
        }

        // Extract project IDs
        List<Long> projectIds = dtos.stream()
            .map(ProjectResponseDto::getId)
            .collect(Collectors.toList());

        // Single batch query for all counts
        List<Object[]> countResults = taskRepository.countByProjectIds(projectIds);

        // Build map: projectId -> count
        Map<Long, Long> countMap = countResults.stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],  // projectId
                row -> (Long) row[1]   // count
            ));

        // Enrich each DTO (default to 0 if not in map = no tasks)
        dtos.forEach(dto -> dto.setTaskCount(countMap.getOrDefault(dto.getId(), 0L)));

        return dtos;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Create a new project for the authenticated user.
     *
     * @param projectCreateDto Project data
     * @param userId ID of the authenticated user (from JWT)
     * @return Created project
     * @throws ResourceNotFoundException if user not found
     * @throws ValidationException if project name already exists for user
     */
    public ProjectResponseDto createProject(ProjectCreateDto projectCreateDto, Long userId) {
        if (projectCreateDto == null) {
            throw new IllegalArgumentException("projectCreateDto is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        AppUser appUser = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", userId));

        boolean existsByUserAndName = projectRepository.existsByAppUserIdAndNameIgnoreCase(
            userId,
            projectCreateDto.getName()
        );
        if (existsByUserAndName) {
            throw new ValidationException("user with project name already exists");
        }

        Project project = projectMapper.toEntity(projectCreateDto);
        project.setAppUser(appUser);
        project = projectRepository.save(project);
        // New project has 0 tasks, but use enrichWithTaskCount for consistency
        return enrichWithTaskCount(projectMapper.toResponseDto(project));
    }

    // =========================================================================
    // READ - Single Project
    // =========================================================================

    /**
     * Find a project by ID, with ownership check.
     *
     * @param projectId Project ID
     * @param userId ID of the authenticated user
     * @return Optional containing project if found and owned by user
     */
    @Transactional(readOnly = true)
    public Optional<ProjectResponseDto> findById(Long projectId, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        return projectRepository.findWithAppUserById(projectId)
            .filter(project -> Objects.equals(project.getAppUser().getId(), userId))
            .map(projectMapper::toResponseDto)
            .map(this::enrichWithTaskCount);
    }

    /**
     * Get a project by ID, with ownership check.
     *
     * @param projectId Project ID
     * @param userId ID of the authenticated user
     * @return Project data
     * @throws ResourceNotFoundException if project not found
     * @throws ValidationException if project doesn't belong to user
     */
    @Transactional(readOnly = true)
    public ProjectResponseDto getById(Long projectId, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Project project = projectRepository.findWithAppUserById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        validateOwnership(project, userId);
        return enrichWithTaskCount(projectMapper.toResponseDto(project));
    }

    // =========================================================================
    // READ - Lists (All scoped to authenticated user)
    // =========================================================================

    /**
     * Get all projects for the authenticated user.
     *
     * @param userId ID of the authenticated user
     * @return List of user's projects
     */
    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findAll(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(
            projectRepository.findWithAppUserByAppUserId(userId)
        );
        return enrichWithTaskCounts(dtos);
    }

    /**
     * Get projects by status for the authenticated user.
     *
     * @param status Project status
     * @param userId ID of the authenticated user
     * @return List of user's projects with given status
     */
    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByStatus(ProjectStatus status, Long userId) {
        if (status == null) {
            throw new IllegalArgumentException("status is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(
            projectRepository.findWithAppUserByAppUserIdAndStatus(userId, status)
        );
        return enrichWithTaskCounts(dtos);
    }

    /**
     * Search projects by name for the authenticated user.
     *
     * @param nameQuery Name search query
     * @param userId ID of the authenticated user
     * @return List of user's projects matching the name query
     */
    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByNameContaining(String nameQuery, Long userId) {
        if (StringUtils.isEmpty(nameQuery)) {
            throw new IllegalArgumentException("nameQuery is empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        List<Project> projects = projectRepository.findWithAppUserByAppUserIdAndNameContainingIgnoreCase(
            userId, nameQuery
        );
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(projects);
        return enrichWithTaskCounts(dtos);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Update a project, with ownership check.
     *
     * @param projectId Project ID
     * @param projectUpdateDto Updated project data
     * @param userId ID of the authenticated user
     * @return Updated project
     * @throws ResourceNotFoundException if project not found
     * @throws ValidationException if project doesn't belong to user or name already exists
     */
    public ProjectResponseDto updateProject(Long projectId, ProjectUpdateDto projectUpdateDto, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (projectUpdateDto == null) {
            throw new IllegalArgumentException("projectUpdateDto is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Project projectToUpdate = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        validateOwnership(projectToUpdate, userId);

        // Check for duplicate name (only if name is changing)
        boolean hasDuplicateName = false;
        if (!Strings.CI.equals(projectToUpdate.getName(), projectUpdateDto.getName())) {
            hasDuplicateName = projectRepository.existsByAppUserIdAndNameIgnoreCase(
                userId,
                projectUpdateDto.getName()
            );
        }

        if (hasDuplicateName) {
            throw new ValidationException("name already exists");
        }

        projectMapper.patchEntityFromDto(projectUpdateDto, projectToUpdate);
        return enrichWithTaskCount(projectMapper.toResponseDto(projectRepository.save(projectToUpdate)));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Delete a project, with ownership check.
     *
     * @param projectId Project ID
     * @param userId ID of the authenticated user
     * @throws ResourceNotFoundException if project not found
     * @throws ValidationException if project doesn't belong to user
     */
    public void deleteProject(Long projectId, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        validateOwnership(project, userId);

        projectRepository.deleteById(projectId);
    }

    // =========================================================================
    // EXISTENCE CHECK
    // =========================================================================

    /**
     * Checks if a project with the given name exists for the authenticated user.
     * Comparison is case-insensitive (e.g., "My Project" matches "my project").
     *
     * @param name the project name to check
     * @param userId ID of the authenticated user
     * @return true if a project with that name exists for the user
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name, Long userId) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }

        return projectRepository.existsByAppUserIdAndNameIgnoreCase(userId, name);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Validate that a project belongs to the given user.
     *
     * @param project The project to check
     * @param userId The user ID to validate against
     * @throws ValidationException if project doesn't belong to user
     */
    private void validateOwnership(Project project, Long userId) {
        if (!Objects.equals(project.getAppUser().getId(), userId)) {
            throw new ValidationException("Project does not belong to authenticated user");
        }
    }
}
