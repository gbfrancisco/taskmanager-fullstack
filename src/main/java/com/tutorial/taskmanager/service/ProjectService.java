package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // TODO: Create Project
    // - Method: createProject(Project project)
    // - Validate that appUser exists (project must have an owner)
    // - Validate that project name is unique for this user (use existsByAppUserIdAndName)
    // - Set default status to PLANNING if not provided (should already be set in entity)
    // - Save and return the created project

    // TODO: Find Project by ID
    // - Method: findById(Long id)
    // - Return Optional<Project>

    // TODO: Get Project by ID (with exception if not found)
    // - Method: getById(Long id)
    // - Throw custom exception if project not found

    // TODO: Find all Projects
    // - Method: findAll()
    // - Return List<Project>

    // TODO: Find Projects by AppUser ID
    // - Method: findByAppUserId(Long appUserId)
    // - Return all projects owned by a specific user
    // - Use ID-based query for performance

    // TODO: Find Projects by Status
    // - Method: findByStatus(ProjectStatus status)
    // - Return all projects with a specific status
    // - Useful for filtering (e.g., all ACTIVE projects)

    // TODO: Find Projects by AppUser ID and Status
    // - Method: findByAppUserIdAndStatus(Long appUserId, ProjectStatus status)
    // - Return projects filtered by both user and status
    // - Example: all ACTIVE projects for a user

    // TODO: Search Projects by Name
    // - Method: searchByName(String nameQuery)
    // - Use repository.findByNameContainingIgnoreCase()
    // - Case-insensitive partial matching

    // TODO: Search Projects by AppUser ID and Name
    // - Method: searchByAppUserIdAndName(Long appUserId, String nameQuery)
    // - Search within a specific user's projects
    // - Use repository.findByAppUserIdAndNameContainingIgnoreCase()

    // TODO: Find Project by Name (exact match)
    // - Method: findByName(String name)
    // - Return Optional<Project>
    // - Use repository.findByName()

    // TODO: Update Project
    // - Method: updateProject(Long id, Project updatedProject)
    // - Find existing project (throw exception if not found)
    // - Update fields: name, description, status
    // - If name is changed, validate uniqueness for the owner
    // - Don't allow changing appUser (project ownership is immutable)
    // - Save and return updated project

    // TODO: Update Project Status
    // - Method: updateProjectStatus(Long id, ProjectStatus newStatus)
    // - Dedicated method for status updates (common operation)
    // - Find project, update status, save
    // - Optional: Add business logic (e.g., can't set to COMPLETED if tasks are not done)

    // TODO: Delete Project by ID
    // - Method: deleteById(Long id)
    // - Check if project exists (throw exception if not found)
    // - Note: Deleting project will also delete associated tasks (due to cascade)
    // - Consider: Maybe prevent deletion if project has tasks, or offer a flag to force delete
    // - Delete the project

    // TODO: Check if Project name exists for a user
    // - Method: existsByAppUserIdAndName(Long appUserId, String name)
    // - Return boolean
    // - Useful for validation before creating/updating projects
}
