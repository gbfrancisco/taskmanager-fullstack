package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // TODO: Create Task
    // - Method: createTask(Task task)
    // - Validate that appUser exists (task must have an owner)
    // - Optionally validate that project exists (if task is assigned to a project)
    // - Set default status to TODO if not provided (should already be set in entity)
    // - Save and return the created task

    // TODO: Find Task by ID
    // - Method: findById(Long id)
    // - Return Optional<Task>

    // TODO: Get Task by ID (with exception if not found)
    // - Method: getById(Long id)
    // - Throw custom exception if task not found

    // TODO: Find all Tasks
    // - Method: findAll()
    // - Return List<Task>

    // TODO: Find Tasks by AppUser ID
    // - Method: findByAppUserId(Long appUserId)
    // - Return all tasks owned by a specific user
    // - Use ID-based query for performance

    // TODO: Find Tasks by Project ID
    // - Method: findByProjectId(Long projectId)
    // - Return all tasks in a specific project
    // - Use ID-based query for performance

    // TODO: Find Tasks by Status
    // - Method: findByStatus(TaskStatus status)
    // - Return all tasks with a specific status
    // - Useful for dashboard views (e.g., all TODO tasks)

    // TODO: Find Tasks by AppUser ID and Status
    // - Method: findByAppUserIdAndStatus(Long appUserId, TaskStatus status)
    // - Return tasks filtered by both user and status
    // - Example: all IN_PROGRESS tasks for a user

    // TODO: Find Tasks by Project ID and Status
    // - Method: findByProjectIdAndStatus(Long projectId, TaskStatus status)
    // - Return tasks filtered by both project and status

    // TODO: Find Overdue Tasks by AppUser ID
    // - Method: findOverdueTasksByAppUserId(Long appUserId)
    // - Find tasks where dueDate is before current date and status is not COMPLETED or CANCELLED
    // - Use repository.findByAppUserIdAndDueDateBeforeAndStatusNotIn()

    // TODO: Find Tasks due between dates
    // - Method: findByDueDateBetween(LocalDateTime start, LocalDateTime end)
    // - Useful for calendar views or weekly/monthly task lists

    // TODO: Update Task
    // - Method: updateTask(Long id, Task updatedTask)
    // - Find existing task (throw exception if not found)
    // - Update fields: title, description, status, dueDate, project
    // - Don't allow changing appUser (task ownership is immutable)
    // - Save and return updated task

    // TODO: Update Task Status
    // - Method: updateTaskStatus(Long id, TaskStatus newStatus)
    // - Dedicated method for status updates (common operation)
    // - Find task, update status, save

    // TODO: Assign Task to Project
    // - Method: assignTaskToProject(Long taskId, Long projectId)
    // - Find task (throw exception if not found)
    // - Validate that project exists
    // - Optionally validate that task owner is same as project owner
    // - Set task.project and save

    // TODO: Unassign Task from Project
    // - Method: unassignTaskFromProject(Long taskId)
    // - Find task, set project to null, save

    // TODO: Delete Task by ID
    // - Method: deleteById(Long id)
    // - Check if task exists (throw exception if not found)
    // - Delete the task
}
