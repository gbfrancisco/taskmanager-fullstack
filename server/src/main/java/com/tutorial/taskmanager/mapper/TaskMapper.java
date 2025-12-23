package com.tutorial.taskmanager.mapper;

import com.tutorial.taskmanager.dto.project.ProjectSummaryDto;
import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
import com.tutorial.taskmanager.model.Project;
import com.tutorial.taskmanager.model.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between Task entity and DTOs.
 *
 * <p>This mapper handles the conversion between Task entities and their corresponding
 * DTOs, including proper handling of relationships (AppUser and Project).
 *
 * <p><strong>Relationship Mapping Strategy:</strong>
 * Both relationships are mapped to embedded summary DTOs:
 * <ul>
 *   <li>Entity → DTO: Maps {@code appUser} to embedded {@code AppUserSummaryDto}</li>
 *   <li>Entity → DTO: Maps {@code project} to embedded {@code ProjectSummaryDto}</li>
 *   <li>DTO → Entity: IDs are set separately in the service layer (not mapped here)</li>
 * </ul>
 *
 * <p><strong>Automatic Nested Object Mapping:</strong>
 * MapStruct automatically finds and uses mapping methods for nested objects:
 * <ul>
 *   <li>{@link AppUserMapper#toSummary} for appUser → AppUserSummaryDto</li>
 *   <li>{@link #toProjectSummary} for project → ProjectSummaryDto</li>
 * </ul>
 *
 * <p><strong>Why not map IDs to entities automatically?</strong>
 * MapStruct cannot automatically convert an ID (Long) to an entity (AppUser/Project).
 * The service layer is responsible for fetching entities by ID and setting relationships.
 *
 * @see Task
 * @see TaskCreateDto
 * @see TaskUpdateDto
 * @see TaskResponseDto
 * @see ProjectSummaryDto
 * @see AppUserMapper
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN,
    uses = AppUserMapper.class
)
public interface TaskMapper {

    /**
     * Converts a CreateDto to a Task entity.
     *
     * <p>MapStruct will automatically map:
     * <ul>
     *   <li>title → title</li>
     *   <li>description → description</li>
     *   <li>status → status</li>
     *   <li>dueDate → dueDate</li>
     * </ul>
     *
     * <p><strong>Relationships (appUser, project):</strong>
     * These are NOT mapped here. The service layer must:
     * <ol>
     *   <li>Call this mapper to create the base Task</li>
     *   <li>Fetch AppUser by appUserId</li>
     *   <li>Fetch Project by projectId (if provided)</li>
     *   <li>Set task.setAppUser(user) and task.setProject(project)</li>
     * </ol>
     *
     * @param dto the creation DTO
     * @return a new Task entity (relationships not set)
     */
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "project", ignore = true)
    Task toEntity(TaskCreateDto dto);

    /**
     * Converts a Task entity to a ResponseDto.
     *
     * <p>MapStruct will automatically map:
     * <ul>
     *   <li>id → id</li>
     *   <li>title → title</li>
     *   <li>description → description</li>
     *   <li>status → status</li>
     *   <li>dueDate → dueDate</li>
     *   <li>appUser → appUser (uses {@link AppUserMapper#toSummary} via 'uses' attribute)</li>
     *   <li>project → project (uses {@link #toProjectSummary(Project)} automatically)</li>
     * </ul>
     *
     * <p><strong>Handling null relationships:</strong>
     * If task.appUser is null, appUser summary will be null.
     * If task.project is null, project summary will be null.
     * MapStruct uses null-safe navigation by default.
     *
     * @param entity the Task entity
     * @return a DTO suitable for API responses
     */
    TaskResponseDto toResponseDto(Task entity);

    /**
     * Converts a Project entity to a lightweight ProjectSummaryDto.
     *
     * <p>This method is automatically used by MapStruct when mapping
     * Task.project to TaskResponseDto.project.
     *
     * <p>Maps: id → id, name → name, status → status
     *
     * @param project the Project entity (can be null)
     * @return a lightweight summary DTO, or null if project is null
     */
    ProjectSummaryDto toProjectSummary(Project project);

    /**
     * Converts a list of Task entities to ResponseDtos.
     *
     * @param entities list of Task entities
     * @return list of response DTOs
     */
    List<TaskResponseDto> toResponseDtoList(List<Task> entities);

    /**
     * Updates an existing Task entity from an UpdateDto.
     *
     * <p>MapStruct will update:
     * <ul>
     *   <li>title (if provided)</li>
     *   <li>description (if provided)</li>
     *   <li>status (if provided)</li>
     *   <li>dueDate (if provided)</li>
     * </ul>
     *
     * <p><strong>Relationship updates (appUserId, projectId):</strong>
     * These are NOT updated here. The service layer handles relationship changes:
     * <ol>
     *   <li>Call this mapper to update basic fields</li>
     *   <li>If appUserId changed, fetch new AppUser and set it</li>
     *   <li>If projectId changed, fetch new Project and set it</li>
     * </ol>
     *
     * @param dto the update DTO
     * @param entity the existing entity to update (modified in-place)
     */
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "project", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchEntityFromDto(TaskUpdateDto dto, @MappingTarget Task entity);
}
