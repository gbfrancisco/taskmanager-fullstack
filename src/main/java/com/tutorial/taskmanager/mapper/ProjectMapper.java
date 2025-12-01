package com.tutorial.taskmanager.mapper;

import com.tutorial.taskmanager.dto.project.ProjectCreateDto;
import com.tutorial.taskmanager.dto.project.ProjectResponseDto;
import com.tutorial.taskmanager.dto.project.ProjectUpdateDto;
import com.tutorial.taskmanager.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between Project entity and DTOs.
 *
 * <p>This mapper handles the conversion between Project entities and their corresponding
 * DTOs, including proper handling of the AppUser relationship.
 *
 * <p><strong>Relationship Mapping Strategy:</strong>
 * <ul>
 *   <li>Entity → DTO: Maps {@code appUser.id} to {@code appUserId}</li>
 *   <li>DTO → Entity: AppUser ID is set in the service layer (not mapped here)</li>
 * </ul>
 *
 * <p><strong>Tasks Collection:</strong>
 * The tasks collection is ignored in all mappings to prevent circular references
 * and lazy loading issues. Tasks should be fetched separately if needed.
 *
 * @see Project
 * @see ProjectCreateDto
 * @see ProjectUpdateDto
 * @see ProjectResponseDto
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ProjectMapper {

    /**
     * Converts a CreateDto to a Project entity.
     *
     * <p>MapStruct will automatically map:
     * <ul>
     *   <li>name → name</li>
     *   <li>description → description</li>
     *   <li>status → status</li>
     * </ul>
     *
     * <p><strong>AppUser relationship:</strong>
     * NOT mapped here. The service layer must:
     * <ol>
     *   <li>Call this mapper to create the base Project</li>
     *   <li>Fetch AppUser by appUserId from the DTO</li>
     *   <li>Set project.setAppUser(user)</li>
     * </ol>
     *
     * @param dto the creation DTO
     * @return a new Project entity (appUser not set)
     */
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Project toEntity(ProjectCreateDto dto);

    /**
     * Converts a Project entity to a ResponseDto.
     *
     * <p>MapStruct will automatically map:
     * <ul>
     *   <li>id → id</li>
     *   <li>name → name</li>
     *   <li>description → description</li>
     *   <li>status → status</li>
     *   <li>createdAt → createdAt</li>
     *   <li>updatedAt → updatedAt</li>
     *   <li>appUser.id → appUserId (explicit mapping)</li>
     * </ul>
     *
     * <p><strong>Null safety:</strong>
     * If project.appUser is null, appUserId will be null (safe navigation).
     *
     * @param entity the Project entity
     * @return a DTO suitable for API responses
     */
    @Mapping(source = "appUser.id", target = "appUserId")
    ProjectResponseDto toResponseDto(Project entity);

    /**
     * Converts a list of Project entities to ResponseDtos.
     *
     * @param entities list of Project entities
     * @return list of response DTOs
     */
    List<ProjectResponseDto> toResponseDtoList(List<Project> entities);

    /**
     * Updates an existing Project entity from an UpdateDto.
     *
     * <p>MapStruct will update:
     * <ul>
     *   <li>name (if provided)</li>
     *   <li>description (if provided)</li>
     *   <li>status (if provided)</li>
     * </ul>
     *
     * <p><strong>AppUser relationship:</strong>
     * NOT updated here. Project ownership (appUser) typically should not change
     * after creation. If needed, the service layer would handle this separately.
     *
     * @param dto the update DTO
     * @param entity the existing entity to update (modified in-place)
     */
    @Mapping(target = "appUser", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    void updateEntityFromDto(ProjectUpdateDto dto, @MappingTarget Project entity);
}
