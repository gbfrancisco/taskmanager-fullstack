package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ProjectService.
 * <p>
 * Tests all CRUD operations, validation logic, filtering, and business rules.
 * Uses Mockito to mock repository and mapper dependencies and AssertJ for assertions.
 * <p>
 * Test organization:
 * - Create operations (createProject) - returns ProjectResponseDto
 * - Read operations (findById, getById, findAll) - returns ProjectResponseDto
 * - Update operations (updateProject) - returns ProjectResponseDto
 * - Delete operations (deleteProject)
 * - Filter operations (findByAppUserId, findByStatus, search by name) - returns ProjectResponseDto
 * - Existence checks (existsByAppUserIdAndName)
 * <p>
 * Note: Services return DTOs for public API. Controllers pass DTOs to clients.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProjectService projectService;

    // Test data
    private AppUser testUser;
    private AppUserSummaryDto testUserSummary;
    private Project testProject;
    private ProjectResponseDto testProjectResponseDto;
    private ProjectCreateDto createDto;
    private ProjectUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");

        // Set up test user summary (for response DTOs)
        testUserSummary = AppUserSummaryDto.builder()
                .id(1L)
                .username("testuser")
                .build();

        // Set up test project
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setStatus(ProjectStatus.PLANNING);
        testProject.setAppUser(testUser);

        // Set up test project response DTO
        testProjectResponseDto = ProjectResponseDto.builder()
                .id(1L)
                .name("Test Project")
                .description("Test Description")
                .status(ProjectStatus.PLANNING)
                .appUser(testUserSummary)
                .taskCount(0L)
                .build();

        // Set up create DTO
        createDto = ProjectCreateDto.builder()
                .name("New Project")
                .description("New Description")
                .status(ProjectStatus.PLANNING)
                .appUserId(1L)
                .build();

        // Set up update DTO
        updateDto = ProjectUpdateDto.builder()
                .name("Updated Project")
                .description("Updated Description")
                .status(ProjectStatus.ACTIVE)
                .build();

        // Set up default stubs for task count (used by enrichWithTaskCount)
        lenient().when(taskRepository.countByProjectId(anyLong())).thenReturn(0L);
        lenient().when(taskRepository.countByProjectIds(anyList())).thenReturn(List.of());
    }

    // ==================== CREATE OPERATIONS ====================

    @Nested
    @DisplayName("Create Project Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project successfully")
        void shouldCreateProjectSuccessfully() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.existsByAppUserIdAndName(1L, createDto.getName())).thenReturn(false);
            when(projectMapper.toEntity(createDto)).thenReturn(testProject);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            ProjectResponseDto result = projectService.createProject(createDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(testProjectResponseDto.getName());
            assertThat(result.getDescription()).isEqualTo(testProjectResponseDto.getDescription());
            assertThat(result.getStatus()).isEqualTo(testProjectResponseDto.getStatus());
            assertThat(result.getAppUser().getId()).isEqualTo(1L);

            verify(appUserRepository).findById(1L);
            verify(projectRepository).existsByAppUserIdAndName(1L, createDto.getName());
            verify(projectMapper).toEntity(createDto);
            verify(projectRepository).save(any(Project.class));
            verify(projectMapper).toResponseDto(testProject);
        }

        @Test
        @DisplayName("Should create project with default status PLANNING")
        void shouldCreateProjectWithDefaultStatus() {
            // Arrange
            createDto.setStatus(null);
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.existsByAppUserIdAndName(1L, createDto.getName())).thenReturn(false);
            when(projectMapper.toEntity(createDto)).thenReturn(testProject);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
                Project saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(ProjectStatus.PLANNING);
                return testProject;
            });
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            projectService.createProject(createDto);

            // Assert
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            createDto.setAppUserId(99L);
            when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(createDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("appUser")
                    .hasMessageContaining("99");

            verify(appUserRepository).findById(99L);
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when project name already exists for user")
        void shouldThrowExceptionWhenProjectNameExistsForUser() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(projectRepository.existsByAppUserIdAndName(1L, createDto.getName())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(createDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("exists");

            verify(appUserRepository).findById(1L);
            verify(projectRepository).existsByAppUserIdAndName(1L, createDto.getName());
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when appUserId is null")
        void shouldThrowExceptionWhenAppUserIdIsNull() {
            // Arrange
            createDto.setAppUserId(null);

            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(createDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("appUserId");

            verify(appUserRepository, never()).findById(anyLong());
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when createDto is null")
        void shouldThrowExceptionWhenCreateDtoIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should allow same project name for different users")
        void shouldAllowSameProjectNameForDifferentUsers() {
            // Arrange - Different user (ID 2) can have same project name
            AppUser differentUser = new AppUser();
            differentUser.setId(2L);
            differentUser.setUsername("otheruser");

            createDto.setAppUserId(2L);

            when(appUserRepository.findById(2L)).thenReturn(Optional.of(differentUser));
            when(projectRepository.existsByAppUserIdAndName(2L, createDto.getName())).thenReturn(false);
            when(projectMapper.toEntity(createDto)).thenReturn(testProject);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            ProjectResponseDto result = projectService.createProject(createDto);

            // Assert
            assertThat(result).isNotNull();
            verify(projectRepository).existsByAppUserIdAndName(2L, createDto.getName());
        }
    }

    // ==================== READ OPERATIONS ====================

    @Nested
    @DisplayName("Read Project Tests")
    class ReadProjectTests {

        @Test
        @DisplayName("Should find project by ID (returns Optional)")
        void shouldFindProjectById() {
            // Arrange
            when(projectRepository.findWithAppUserById(1L)).thenReturn(Optional.of(testProject));
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            Optional<ProjectResponseDto> result = projectService.findById(1L);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getName()).isEqualTo(testProjectResponseDto.getName());

            verify(projectRepository).findWithAppUserById(1L);
            verify(projectMapper).toResponseDto(testProject);
        }

        @Test
        @DisplayName("Should return empty Optional when project not found")
        void shouldReturnEmptyWhenProjectNotFound() {
            // Arrange
            when(projectRepository.findWithAppUserById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<ProjectResponseDto> result = projectService.findById(99L);

            // Assert
            assertThat(result).isEmpty();
            verify(projectRepository).findWithAppUserById(99L);
            verify(projectMapper, never()).toResponseDto(any());
        }

        @Test
        @DisplayName("Should throw exception when findById with null id")
        void shouldThrowExceptionWhenFindByIdWithNullId() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.findById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");

            verify(projectRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should get project by ID (throws exception if not found)")
        void shouldGetProjectById() {
            // Arrange
            when(projectRepository.findWithAppUserById(1L)).thenReturn(Optional.of(testProject));
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            ProjectResponseDto result = projectService.getById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(projectRepository).findWithAppUserById(1L);
            verify(projectMapper).toResponseDto(testProject);
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent project")
        void shouldThrowExceptionWhenGettingNonExistentProject() {
            // Arrange
            when(projectRepository.findWithAppUserById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> projectService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("project")
                    .hasMessageContaining("99");

            verify(projectRepository).findWithAppUserById(99L);
        }

        @Test
        @DisplayName("Should throw exception when getById with null id")
        void shouldThrowExceptionWhenGetByIdWithNullId() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.getById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");

            verify(projectRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should find all projects")
        void shouldFindAllProjects() {
            // Arrange
            Project project2 = new Project();
            project2.setId(2L);
            project2.setName("Project 2");
            project2.setAppUser(testUser);

            ProjectResponseDto project2ResponseDto = ProjectResponseDto.builder()
                    .id(2L)
                    .name("Project 2")
                    .appUser(testUserSummary)
                    .build();

            List<Project> projects = List.of(testProject, project2);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto, project2ResponseDto);

            when(projectRepository.findAllWithAppUser()).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findAll();

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(ProjectResponseDto::getId)
                    .containsExactly(1L, 2L);

            verify(projectRepository).findAllWithAppUser();
            verify(projectMapper).toResponseDtoList(projects);
        }

        @Test
        @DisplayName("Should return empty list when no projects exist")
        void shouldReturnEmptyListWhenNoProjects() {
            // Arrange
            List<Project> emptyList = List.of();
            when(projectRepository.findAllWithAppUser()).thenReturn(emptyList);
            when(projectMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<ProjectResponseDto> results = projectService.findAll();

            // Assert
            assertThat(results).isEmpty();
            verify(projectRepository).findAllWithAppUser();
            verify(projectMapper).toResponseDtoList(emptyList);
        }
    }

    // ==================== UPDATE OPERATIONS ====================

    @Nested
    @DisplayName("Update Project Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update all project fields")
        void shouldUpdateAllProjectFields() {
            // Arrange
            ProjectResponseDto updatedResponseDto = ProjectResponseDto.builder()
                    .id(1L)
                    .name(updateDto.getName())
                    .description(updateDto.getDescription())
                    .status(updateDto.getStatus())
                    .appUser(testUserSummary)
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByAppUserIdAndName(1L, updateDto.getName())).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(updatedResponseDto);

            // Act
            ProjectResponseDto result = projectService.updateProject(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(updateDto.getName());
            assertThat(result.getDescription()).isEqualTo(updateDto.getDescription());
            assertThat(result.getStatus()).isEqualTo(updateDto.getStatus());

            verify(projectRepository).findById(1L);
            verify(projectMapper).patchEntityFromDto(updateDto, testProject);
            verify(projectRepository).save(testProject);
            verify(projectMapper).toResponseDto(testProject);
        }

        @Test
        @DisplayName("Should update only provided fields (partial update)")
        void shouldUpdateOnlyProvidedFields() {
            // Arrange
            updateDto.setName(null);  // Don't update name
            updateDto.setDescription(null);  // Don't update description

            ProjectResponseDto partiallyUpdatedDto = ProjectResponseDto.builder()
                    .id(1L)
                    .name(testProject.getName())  // Original name
                    .description(testProject.getDescription())  // Original description
                    .status(updateDto.getStatus())  // Updated status
                    .appUser(testUserSummary)
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            // No uniqueness check needed when name is not changing
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(partiallyUpdatedDto);

            // Act
            ProjectResponseDto result = projectService.updateProject(1L, updateDto);

            // Assert
            assertThat(result.getName()).isEqualTo(testProject.getName());  // Unchanged
            assertThat(result.getStatus()).isEqualTo(updateDto.getStatus());  // Changed

            verify(projectMapper).patchEntityFromDto(updateDto, testProject);
            verify(projectRepository).save(testProject);
            // Should NOT check uniqueness when name is null (not being updated)
            verify(projectRepository, never()).existsByAppUserIdAndName(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent project")
        void shouldThrowExceptionWhenUpdatingNonExistentProject() {
            // Arrange
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> projectService.updateProject(99L, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("project")
                    .hasMessageContaining("99");

            verify(projectRepository).findById(99L);
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when updating to duplicate name")
        void shouldThrowExceptionWhenUpdatingToDuplicateName() {
            // Arrange
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByAppUserIdAndName(1L, updateDto.getName())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> projectService.updateProject(1L, updateDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("exists");

            verify(projectRepository).findById(1L);
            verify(projectRepository).existsByAppUserIdAndName(1L, updateDto.getName());
            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should allow updating to same name (no actual change)")
        void shouldAllowUpdatingToSameName() {
            // Arrange - updating name to what it already is
            updateDto.setName(testProject.getName());

            ProjectResponseDto updatedResponseDto = ProjectResponseDto.builder()
                    .id(1L)
                    .name(testProject.getName())
                    .status(updateDto.getStatus())
                    .appUser(testUserSummary)
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            // When name is same as current, should skip uniqueness check OR handle it gracefully
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(updatedResponseDto);

            // Act
            ProjectResponseDto result = projectService.updateProject(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            verify(projectRepository).save(testProject);
        }

        @Test
        @DisplayName("Should throw exception when update id is null")
        void shouldThrowExceptionWhenUpdateIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.updateProject(null, updateDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");

            verify(projectRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when updateDto is null")
        void shouldThrowExceptionWhenUpdateDtoIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.updateProject(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("updateDto");

            verify(projectRepository, never()).save(any(Project.class));
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Nested
    @DisplayName("Delete Project Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project successfully")
        void shouldDeleteProject() {
            // Arrange
            when(projectRepository.existsById(1L)).thenReturn(true);
            doNothing().when(projectRepository).deleteById(1L);

            // Act
            projectService.deleteProject(1L);

            // Assert
            verify(projectRepository).existsById(1L);
            verify(projectRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent project")
        void shouldThrowExceptionWhenDeletingNonExistentProject() {
            // Arrange
            when(projectRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> projectService.deleteProject(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("project")
                    .hasMessageContaining("99");

            verify(projectRepository).existsById(99L);
            verify(projectRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when delete id is null")
        void shouldThrowExceptionWhenDeleteIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.deleteProject(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");

            verify(projectRepository, never()).existsById(anyLong());
            verify(projectRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== FILTER OPERATIONS ====================

    @Nested
    @DisplayName("Filter Projects Tests")
    class FilterProjectsTests {

        @Test
        @DisplayName("Should find projects by user ID")
        void shouldFindProjectsByUserId() {
            // Arrange
            Project project2 = new Project();
            project2.setId(2L);
            project2.setName("Project 2");
            project2.setAppUser(testUser);

            ProjectResponseDto project2ResponseDto = ProjectResponseDto.builder()
                    .id(2L)
                    .name("Project 2")
                    .appUser(testUserSummary)
                    .build();

            List<Project> projects = List.of(testProject, project2);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto, project2ResponseDto);

            when(projectRepository.findWithAppUserByAppUserId(1L)).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByAppUserId(1L);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).extracting(dto -> dto.getAppUser().getId())
                    .containsOnly(1L);

            verify(projectRepository).findWithAppUserByAppUserId(1L);
            verify(projectMapper).toResponseDtoList(projects);
        }

        @Test
        @DisplayName("Should find projects by status")
        void shouldFindProjectsByStatus() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByStatus(ProjectStatus.PLANNING)).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByStatus(ProjectStatus.PLANNING);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getStatus()).isEqualTo(ProjectStatus.PLANNING);

            verify(projectRepository).findWithAppUserByStatus(ProjectStatus.PLANNING);
            verify(projectMapper).toResponseDtoList(projects);
        }

        @Test
        @DisplayName("Should find projects by user ID and status")
        void shouldFindProjectsByUserIdAndStatus() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByAppUserIdAndStatus(1L, ProjectStatus.PLANNING)).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByAppUserIdAndStatus(1L, ProjectStatus.PLANNING);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getAppUser().getId()).isEqualTo(1L);
            assertThat(results.getFirst().getStatus()).isEqualTo(ProjectStatus.PLANNING);

            verify(projectRepository).findWithAppUserByAppUserIdAndStatus(1L, ProjectStatus.PLANNING);
            verify(projectMapper).toResponseDtoList(projects);
        }

        @Test
        @DisplayName("Should return empty list when no projects match filters")
        void shouldReturnEmptyListWhenNoProjectsMatchFilters() {
            // Arrange
            List<Project> emptyList = List.of();
            when(projectRepository.findWithAppUserByStatus(ProjectStatus.COMPLETED)).thenReturn(emptyList);
            when(projectMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<ProjectResponseDto> results = projectService.findByStatus(ProjectStatus.COMPLETED);

            // Assert
            assertThat(results).isEmpty();
            verify(projectRepository).findWithAppUserByStatus(ProjectStatus.COMPLETED);
            verify(projectMapper).toResponseDtoList(emptyList);
        }
    }

    // ==================== SEARCH OPERATIONS ====================

    @Nested
    @DisplayName("Search Projects Tests")
    class SearchProjectsTests {

        @Test
        @DisplayName("Should search projects by name (case-insensitive, partial match)")
        void shouldSearchProjectsByName() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByNameContainingIgnoreCase("test")).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByNameContaining("test");

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getName()).containsIgnoringCase("test");

            verify(projectRepository).findWithAppUserByNameContainingIgnoreCase("test");
            verify(projectMapper).toResponseDtoList(projects);
        }

        @Test
        @DisplayName("Should search projects by user ID and name")
        void shouldSearchProjectsByUserIdAndName() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByAppUserIdAndNameContainingIgnoreCase(1L, "test")).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByAppUserIdAndNameContaining(1L, "test");

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getAppUser().getId()).isEqualTo(1L);
            assertThat(results.getFirst().getName()).containsIgnoringCase("test");

            verify(projectRepository).findWithAppUserByAppUserIdAndNameContainingIgnoreCase(1L, "test");
            verify(projectMapper).toResponseDtoList(projects);
        }

        @Test
        @DisplayName("Should find project by exact name")
        void shouldFindProjectByExactName() {
            // Arrange
            when(projectRepository.findByName("Test Project")).thenReturn(Optional.of(testProject));
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            Optional<ProjectResponseDto> result = projectService.findByName("Test Project");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Test Project");

            verify(projectRepository).findByName("Test Project");
            verify(projectMapper).toResponseDto(testProject);
        }

        @Test
        @DisplayName("Should return empty when project name not found")
        void shouldReturnEmptyWhenProjectNameNotFound() {
            // Arrange
            when(projectRepository.findByName("Nonexistent")).thenReturn(Optional.empty());

            // Act
            Optional<ProjectResponseDto> result = projectService.findByName("Nonexistent");

            // Assert
            assertThat(result).isEmpty();
            verify(projectRepository).findByName("Nonexistent");
            verify(projectMapper, never()).toResponseDto(any());
        }

        @Test
        @DisplayName("Should return empty list when search finds nothing")
        void shouldReturnEmptyListWhenSearchFindsNothing() {
            // Arrange
            List<Project> emptyList = List.of();
            when(projectRepository.findWithAppUserByNameContainingIgnoreCase("xyz")).thenReturn(emptyList);
            when(projectMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<ProjectResponseDto> results = projectService.findByNameContaining("xyz");

            // Assert
            assertThat(results).isEmpty();
            verify(projectRepository).findWithAppUserByNameContainingIgnoreCase("xyz");
        }
    }

    // ==================== EXISTENCE CHECK OPERATIONS ====================

    @Nested
    @DisplayName("Existence Check Tests")
    class ExistenceCheckTests {

        @Test
        @DisplayName("Should return true when project name exists for user")
        void shouldReturnTrueWhenProjectNameExistsForUser() {
            // Arrange
            when(projectRepository.existsByAppUserIdAndName(1L, "Test Project")).thenReturn(true);

            // Act
            boolean result = projectService.existsByAppUserIdAndName(1L, "Test Project");

            // Assert
            assertThat(result).isTrue();
            verify(projectRepository).existsByAppUserIdAndName(1L, "Test Project");
        }

        @Test
        @DisplayName("Should return false when project name does not exist for user")
        void shouldReturnFalseWhenProjectNameDoesNotExistForUser() {
            // Arrange
            when(projectRepository.existsByAppUserIdAndName(1L, "Nonexistent")).thenReturn(false);

            // Act
            boolean result = projectService.existsByAppUserIdAndName(1L, "Nonexistent");

            // Assert
            assertThat(result).isFalse();
            verify(projectRepository).existsByAppUserIdAndName(1L, "Nonexistent");
        }

        @Test
        @DisplayName("Should throw exception when checking existence with null appUserId")
        void shouldThrowExceptionWhenCheckingExistenceWithNullAppUserId() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.existsByAppUserIdAndName(null, "Test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("appUserId");

            verify(projectRepository, never()).existsByAppUserIdAndName(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when checking existence with null name")
        void shouldThrowExceptionWhenCheckingExistenceWithNullName() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.existsByAppUserIdAndName(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");

            verify(projectRepository, never()).existsByAppUserIdAndName(anyLong(), anyString());
        }
    }
}
