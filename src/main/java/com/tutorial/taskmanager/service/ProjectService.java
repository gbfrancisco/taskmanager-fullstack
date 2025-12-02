package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.project.ProjectCreateDto;
import com.tutorial.taskmanager.dto.project.ProjectUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.repository.AppUserRepository;
import com.tutorial.taskmanager.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;

    public ProjectService(ProjectRepository projectRepository, AppUserRepository appUserRepository) {
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
    }

    // TODO: Create Project
    // - Method: createProject(ProjectCreateDto dto)
    // - Validate that appUser exists (use appUserRepository.findById)
    // - Validate project name is unique for this user (use existsByAppUserIdAndName)
    // - Convert DTO to entity, set relationships, save and return

    // TODO: Find Project by ID
    // - Method: findById(Long id)
    // - Return Optional<Project>
    // - Use projectRepository.findById()

    // TODO: Get Project by ID (with exception if not found)
    // - Method: getById(Long id)
    // - Throw ResourceNotFoundException if project not found

    // TODO: Find all Projects
    // - Method: findAll()
    // - Return List<Project>
    // - Use projectRepository.findAll()

    // TODO: Find Projects by AppUser ID
    // - Method: findByAppUserId(Long appUserId)
    // - Return all projects owned by a specific user
    // - Use ID-based query: projectRepository.findByAppUserId()

    // TODO: Find Projects by Status
    // - Method: findByStatus(ProjectStatus status)
    // - Return all projects with a specific status
    // - Use projectRepository.findByStatus()

    // TODO: Find Projects by AppUser ID and Status
    // - Method: findByAppUserIdAndStatus(Long appUserId, ProjectStatus status)
    // - Return projects filtered by both user and status
    // - Use projectRepository.findByAppUserIdAndStatus()

    // TODO: Search Projects by Name
    // - Method: findByNameContaining(String nameQuery)
    // - Use projectRepository.findByNameContainingIgnoreCase()
    // - Case-insensitive partial matching

    // TODO: Search Projects by AppUser ID and Name
    // - Method: findByAppUserIdAndNameContaining(Long appUserId, String nameQuery)
    // - Search within a specific user's projects
    // - Use projectRepository.findByAppUserIdAndNameContainingIgnoreCase()

    // TODO: Find Project by Name (exact match)
    // - Method: findByName(String name)
    // - Return Optional<Project>
    // - Use projectRepository.findByName()

    // TODO: Update Project
    // - Method: updateProject(Long id, ProjectUpdateDto dto)
    // - Find existing project using getById() (throws if not found)
    // - Update only non-null fields from DTO (partial update pattern)
    // - If name is changed, validate uniqueness for the owner
    // - Don't allow changing appUser (project ownership is immutable)
    // - Save and return updated project

    // TODO: Delete Project by ID
    // - Method: deleteProject(Long id)
    // - Check if project exists using existsById (throw exception if not found)
    // - Note: Deleting project will also delete associated tasks (due to cascade)
    // - Delete using deleteById()

    // TODO: Check if Project name exists for a user
    // - Method: existsByAppUserIdAndName(Long appUserId, String name)
    // - Return boolean
    // - Use projectRepository.existsByAppUserIdAndName()
    // - Useful for validation before creating/updating projects
}
