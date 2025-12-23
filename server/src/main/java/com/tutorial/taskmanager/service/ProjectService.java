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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // ==================== TASK COUNT ENRICHMENT ====================

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

    public ProjectResponseDto createProject(ProjectCreateDto projectCreateDto) {
        if (projectCreateDto == null) {
            throw new IllegalArgumentException("projectCreateDto is null");
        }

        Long appUserId = projectCreateDto.getAppUserId();
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        AppUser appUser = appUserRepository.findById(appUserId)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", appUserId));

        boolean existsByUserAndName = projectRepository.existsByAppUserIdAndName(appUserId, projectCreateDto.getName());
        if (existsByUserAndName) {
            throw new ValidationException("user with project name already exists");
        }

        Project project = projectMapper.toEntity(projectCreateDto);
        project.setAppUser(appUser);
        project = projectRepository.save(project);
        // New project has 0 tasks, but use enrichWithTaskCount for consistency
        return enrichWithTaskCount(projectMapper.toResponseDto(project));
    }

    @Transactional(readOnly = true)
    public Optional<ProjectResponseDto> findById(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("id is null");
        }

        // Use EntityGraph method to eagerly fetch appUser
        return projectRepository.findWithOwnerById(projectId)
            .map(projectMapper::toResponseDto)
            .map(this::enrichWithTaskCount);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto getById(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("id is null");
        }

        // Use EntityGraph method to eagerly fetch appUser
        return projectRepository.findWithOwnerById(projectId)
            .map(projectMapper::toResponseDto)
            .map(this::enrichWithTaskCount)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findAll() {
        // Use EntityGraph method to eagerly fetch appUser for all projects
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(projectRepository.findAllWithOwner());
        return enrichWithTaskCounts(dtos);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByAppUserId(Long appUserId) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        // Use EntityGraph method to eagerly fetch appUser
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(
            projectRepository.findWithOwnerByAppUserId(appUserId)
        );
        return enrichWithTaskCounts(dtos);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByStatus(ProjectStatus projectStatus) {
        if (projectStatus == null) {
            throw new IllegalArgumentException("projectStatus is null");
        }

        // Use EntityGraph method to eagerly fetch appUser
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(
            projectRepository.findWithOwnerByStatus(projectStatus)
        );
        return enrichWithTaskCounts(dtos);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByAppUserIdAndStatus(Long appUserId, ProjectStatus projectStatus) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        if (projectStatus == null) {
            throw new IllegalArgumentException("projectStatus is null");
        }

        // Use EntityGraph method to eagerly fetch appUser
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(
            projectRepository.findWithOwnerByAppUserIdAndStatus(appUserId, projectStatus)
        );
        return enrichWithTaskCounts(dtos);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByNameContaining(String nameQuery) {
        if (StringUtils.isEmpty(nameQuery)) {
            throw new IllegalArgumentException("nameQuery is empty");
        }

        // Use EntityGraph method to eagerly fetch appUser
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(
            projectRepository.findWithOwnerByNameContainingIgnoreCase(nameQuery)
        );
        return enrichWithTaskCounts(dtos);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByAppUserIdAndNameContaining(Long appUserId, String nameQuery) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        if (StringUtils.isEmpty(nameQuery)) {
            throw new IllegalArgumentException("nameQuery is empty");
        }

        // Use EntityGraph method to eagerly fetch appUser
        List<Project> projects = projectRepository.findWithOwnerByAppUserIdAndNameContainingIgnoreCase(
            appUserId, nameQuery
        );
        List<ProjectResponseDto> dtos = projectMapper.toResponseDtoList(projects);
        return enrichWithTaskCounts(dtos);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectResponseDto> findByName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is empty");
        }

        return projectRepository.findByName(name)
            .map(projectMapper::toResponseDto)
            .map(this::enrichWithTaskCount);
    }

    public ProjectResponseDto updateProject(Long projectId, ProjectUpdateDto projectUpdateDto) {
        if (projectId == null) {
            throw new IllegalArgumentException("id is null");
        }

        if (projectUpdateDto == null) {
            throw new IllegalArgumentException("updateDto is null");
        }

        Project projectToUpdate = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));

        boolean hasDuplicateName = projectRepository.existsByAppUserIdAndName(
            projectToUpdate.getAppUser().getId(),
            projectUpdateDto.getName()
        );

        if (hasDuplicateName) {
            throw new ValidationException("name already exists");
        }

        projectMapper.patchEntityFromDto(projectUpdateDto, projectToUpdate);
        return enrichWithTaskCount(projectMapper.toResponseDto(projectRepository.save(projectToUpdate)));
    }

    public void deleteProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("id is null");
        }

        boolean doesProjectExist = projectRepository.existsById(projectId);
        if (!doesProjectExist) {
            throw new ResourceNotFoundException("project", projectId);
        }

        projectRepository.deleteById(projectId);
    }

    @Transactional(readOnly = true)
    public boolean existsByAppUserIdAndName(Long appUserId, String name) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is empty");
        }

        return projectRepository.existsByAppUserIdAndName(appUserId, name);
    }
}
