package com.tutorial.taskmanager.mapper;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.model.AppUser;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between AppUser entity and DTOs.
 *
 * <p>MapStruct generates the implementation of this interface at compile-time.
 * The generated class will be named {@code AppUserMapperImpl} and will be
 * automatically registered as a Spring bean due to {@code componentModel = "spring"}.
 *
 * <p><strong>Configuration:</strong>
 * <ul>
 *   <li>{@code componentModel = "spring"}: Makes this a Spring bean (@Component)</li>
 *   <li>{@code unmappedTargetPolicy = WARN}: Warns at compile-time if any target fields are not mapped</li>
 * </ul>
 *
 * <p><strong>Usage in Service:</strong>
 * <pre>
 * {@code
 * @Service
 * @RequiredArgsConstructor
 * public class AppUserService {
 *     private final AppUserRepository repository;
 *     private final AppUserMapper mapper;  // Injected by Spring
 *
 *     public AppUserResponseDto createUser(AppUserCreateDto dto) {
 *         AppUser user = mapper.toEntity(dto);
 *         user = repository.save(user);
 *         return mapper.toResponseDto(user);
 *     }
 * }
 * }
 * </pre>
 *
 * @see AppUser
 * @see AppUserCreateDto
 * @see AppUserUpdateDto
 * @see AppUserResponseDto
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AppUserMapper {

    /**
     * Converts a CreateDto to an AppUser entity.
     *
     * <p>MapStruct will automatically map fields with matching names:
     * <ul>
     *   <li>username → username</li>
     *   <li>email → email</li>
     *   <li>password → password</li>
     * </ul>
     *
     * <p>Fields not in the DTO (id, createdAt, updatedAt, tasks, projects)
     * will be left null/empty and handled by JPA lifecycle callbacks.
     *
     * @param dto the creation DTO containing user data
     * @return a new AppUser entity (not persisted)
     */
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "projects", ignore = true)
    AppUser toEntity(AppUserCreateDto dto);

    /**
     * Converts an AppUser entity to a ResponseDto.
     *
     * <p>MapStruct will automatically map:
     * <ul>
     *   <li>id → id</li>
     *   <li>username → username</li>
     *   <li>email → email</li>
     * </ul>
     *
     * <p>Note: password is NOT included in ResponseDto (security - never expose passwords).
     * Collections (tasks, projects) are not included (to prevent circular references).
     * Timestamp fields (createdTimestamp, updatedTimestamp) are not included in this DTO.
     *
     * @param entity the AppUser entity
     * @return a DTO suitable for API responses
     */
    AppUserResponseDto toResponseDto(AppUser entity);

    /**
     * Converts a list of AppUser entities to ResponseDtos.
     *
     * <p>MapStruct automatically generates this implementation by calling
     * {@link #toResponseDto(AppUser)} for each element.
     *
     * @param entities list of AppUser entities
     * @return list of response DTOs
     */
    List<AppUserResponseDto> toResponseDtoList(List<AppUser> entities);

    /**
     * Converts an AppUser entity to a lightweight summary DTO.
     *
     * <p>This method is used by other mappers (TaskMapper, ProjectMapper) when they
     * need to embed user information in their response DTOs. By defining it here
     * and using the {@code uses} attribute, MapStruct automatically applies this
     * conversion when mapping nested AppUser relationships.
     *
     * <p>Maps: id → id, username → username
     *
     * @param entity the AppUser entity (can be null)
     * @return a lightweight summary DTO, or null if entity is null
     * @see TaskMapper
     * @see ProjectMapper
     */
    AppUserSummaryDto toSummary(AppUser entity);

    /**
     * Updates an existing AppUser entity from an UpdateDto.
     *
     * <p>This method modifies the target entity in-place rather than creating
     * a new one. Only non-null fields from the DTO will be mapped.
     *
     * <p>MapStruct will update:
     * <ul>
     *   <li>email (if provided)</li>
     *   <li>password (if provided)</li>
     * </ul>
     *
     * <p>Fields that cannot be updated are ignored:
     * <ul>
     *   <li>id (immutable - primary key)</li>
     *   <li>username (immutable - business rule)</li>
     *   <li>createdAt (immutable - audit field)</li>
     *   <li>updatedAt (managed by @PreUpdate)</li>
     *   <li>tasks, projects (managed separately)</li>
     * </ul>
     *
     * @param dto the update DTO containing new values
     * @param entity the existing entity to update (modified in-place)
     */
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchEntityFromDto(AppUserUpdateDto dto, @MappingTarget AppUser entity);
}
