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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            AppUserRepository appUserRepository,
            ProjectMapper projectMapper
    ) {
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
        this.projectMapper = projectMapper;
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
        return projectMapper.toResponseDto(project);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectResponseDto> findById(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("id is null");
        }

        return projectRepository.findById(projectId).map(projectMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto getById(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("id is null");
        }

        return projectRepository.findById(projectId)
            .map(projectMapper::toResponseDto)
            .orElseThrow(() -> new ResourceNotFoundException("project", projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findAll() {
        return projectMapper.toResponseDtoList(projectRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByAppUserId(Long appUserId) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        return projectMapper.toResponseDtoList(projectRepository.findByAppUserId(appUserId));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByStatus(ProjectStatus projectStatus) {
        if (projectStatus == null) {
            throw new IllegalArgumentException("projectStatus is null");
        }

        return projectMapper.toResponseDtoList(projectRepository.findByStatus(projectStatus));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByAppUserIdAndStatus(Long appUserId, ProjectStatus projectStatus) {
        if (appUserId == null) {
            throw new IllegalArgumentException("appUserId is null");
        }

        if (projectStatus == null) {
            throw new IllegalArgumentException("projectStatus is null");
        }

        return projectMapper.toResponseDtoList(projectRepository.findByAppUserIdAndStatus(appUserId, projectStatus));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByNameContaining(String nameQuery) {
        if (StringUtils.isEmpty(nameQuery)) {
            throw new IllegalArgumentException("nameQuery is empty");
        }

        return projectMapper.toResponseDtoList(projectRepository.findByNameContainingIgnoreCase(nameQuery));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findByAppUserIdAndNameContaining(Long appUserId, String nameQuery) {
        if (appUserId == null) {
            throw new  IllegalArgumentException("appUserId is null");
        }

        if (StringUtils.isEmpty(nameQuery)) {
            throw new  IllegalArgumentException("nameQuery is empty");
        }

        List<Project> projects = projectRepository.findByAppUserIdAndNameContainingIgnoreCase(appUserId, nameQuery);
        return projectMapper.toResponseDtoList(projects);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectResponseDto> findByName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name is empty");
        }

        return projectRepository.findByName(name).map(projectMapper::toResponseDto);
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
        return projectMapper.toResponseDto(projectRepository.save(projectToUpdate));
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
