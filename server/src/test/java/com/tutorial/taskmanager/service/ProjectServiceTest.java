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
 * Comprehensive unit tests for ProjectService with user-scoped authorization.
 * <p>
 * All service methods now require a userId parameter for authorization.
 * Tests verify both happy paths and authorization failure scenarios.
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
    private AppUser otherUser;
    private AppUserSummaryDto testUserSummary;
    private Project testProject;
    private ProjectResponseDto testProjectResponseDto;
    private ProjectCreateDto createDto;
    private ProjectUpdateDto updateDto;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        // Set up test user (authenticated user)
        testUser = new AppUser();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");

        // Set up other user (for authorization failure tests)
        otherUser = new AppUser();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");

        // Set up test user summary (for response DTOs)
        testUserSummary = AppUserSummaryDto.builder()
                .id(USER_ID)
                .username("testuser")
                .build();

        // Set up test project (owned by testUser)
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

        // Set up create DTO (no appUserId - extracted from JWT)
        createDto = ProjectCreateDto.builder()
                .name("New Project")
                .description("New Description")
                .status(ProjectStatus.PLANNING)
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
        @DisplayName("Should create project for authenticated user")
        void shouldCreateProjectForAuthenticatedUser() {
            // Arrange
            when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(projectRepository.existsByAppUserIdAndNameIgnoreCase(USER_ID, createDto.getName())).thenReturn(false);
            when(projectMapper.toEntity(createDto)).thenReturn(testProject);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            ProjectResponseDto result = projectService.createProject(createDto, USER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(testProjectResponseDto.getName());
            assertThat(result.getAppUser().getId()).isEqualTo(USER_ID);

            verify(appUserRepository).findById(USER_ID);
            verify(projectRepository).existsByAppUserIdAndNameIgnoreCase(USER_ID, createDto.getName());
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when project name already exists for user")
        void shouldThrowExceptionWhenProjectNameExistsForUser() {
            // Arrange
            when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(projectRepository.existsByAppUserIdAndNameIgnoreCase(USER_ID, createDto.getName())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(createDto, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("exists");

            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(createDto, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId is null");
        }

        @Test
        @DisplayName("Should throw exception when createDto is null")
        void shouldThrowExceptionWhenCreateDtoIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.createProject(null, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== READ OPERATIONS ====================

    @Nested
    @DisplayName("Read Project Tests")
    class ReadProjectTests {

        @Test
        @DisplayName("Should find project by ID with ownership check")
        void shouldFindProjectByIdWithOwnershipCheck() {
            // Arrange
            when(projectRepository.findWithAppUserById(1L)).thenReturn(Optional.of(testProject));
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            Optional<ProjectResponseDto> result = projectService.findById(1L, USER_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            verify(projectRepository).findWithAppUserById(1L);
        }

        @Test
        @DisplayName("Should return empty when project belongs to different user")
        void shouldReturnEmptyWhenProjectBelongsToDifferentUser() {
            // Arrange
            testProject.setAppUser(otherUser); // Project belongs to different user
            when(projectRepository.findWithAppUserById(1L)).thenReturn(Optional.of(testProject));

            // Act
            Optional<ProjectResponseDto> result = projectService.findById(1L, USER_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(projectMapper, never()).toResponseDto(any());
        }

        @Test
        @DisplayName("Should get project by ID with ownership validation")
        void shouldGetProjectByIdWithOwnershipValidation() {
            // Arrange
            when(projectRepository.findWithAppUserById(1L)).thenReturn(Optional.of(testProject));
            when(projectMapper.toResponseDto(testProject)).thenReturn(testProjectResponseDto);

            // Act
            ProjectResponseDto result = projectService.getById(1L, USER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when project doesn't belong to user")
        void shouldThrowExceptionWhenProjectDoesntBelongToUser() {
            // Arrange
            testProject.setAppUser(otherUser);
            when(projectRepository.findWithAppUserById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> projectService.getById(1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Project does not belong to authenticated user");
        }

        @Test
        @DisplayName("Should find all projects for authenticated user only")
        void shouldFindAllProjectsForAuthenticatedUser() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByAppUserId(USER_ID)).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findAll(USER_ID);

            // Assert
            assertThat(results).hasSize(1);
            verify(projectRepository).findWithAppUserByAppUserId(USER_ID);
        }
    }

    // ==================== UPDATE OPERATIONS ====================

    @Nested
    @DisplayName("Update Project Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project with ownership check")
        void shouldUpdateProjectWithOwnershipCheck() {
            // Arrange
            ProjectResponseDto updatedResponseDto = ProjectResponseDto.builder()
                    .id(1L)
                    .name(updateDto.getName())
                    .status(updateDto.getStatus())
                    .appUser(testUserSummary)
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByAppUserIdAndNameIgnoreCase(USER_ID, updateDto.getName())).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(updatedResponseDto);

            // Act
            ProjectResponseDto result = projectService.updateProject(1L, updateDto, USER_ID);

            // Assert
            assertThat(result.getName()).isEqualTo(updateDto.getName());
            verify(projectMapper).patchEntityFromDto(updateDto, testProject);
            verify(projectRepository).save(testProject);
        }

        @Test
        @DisplayName("Should throw exception when updating project that doesn't belong to user")
        void shouldThrowExceptionWhenUpdatingUnownedProject() {
            // Arrange
            testProject.setAppUser(otherUser);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> projectService.updateProject(1L, updateDto, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Project does not belong to authenticated user");

            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("Should throw exception when updating to duplicate name")
        void shouldThrowExceptionWhenUpdatingToDuplicateName() {
            // Arrange
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(projectRepository.existsByAppUserIdAndNameIgnoreCase(USER_ID, updateDto.getName())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> projectService.updateProject(1L, updateDto, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("exists");

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
            when(projectRepository.save(any(Project.class))).thenReturn(testProject);
            when(projectMapper.toResponseDto(testProject)).thenReturn(updatedResponseDto);

            // Act
            ProjectResponseDto result = projectService.updateProject(1L, updateDto, USER_ID);

            // Assert
            assertThat(result).isNotNull();
            verify(projectRepository, never()).existsByAppUserIdAndNameIgnoreCase(anyLong(), anyString());
            verify(projectRepository).save(testProject);
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Nested
    @DisplayName("Delete Project Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project with ownership check")
        void shouldDeleteProjectWithOwnershipCheck() {
            // Arrange
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            doNothing().when(projectRepository).deleteById(1L);

            // Act
            projectService.deleteProject(1L, USER_ID);

            // Assert
            verify(projectRepository).findById(1L);
            verify(projectRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw when deleting project that doesn't belong to user")
        void shouldThrowWhenDeletingUnownedProject() {
            // Arrange
            testProject.setAppUser(otherUser);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

            // Act & Assert
            assertThatThrownBy(() -> projectService.deleteProject(1L, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Project does not belong to authenticated user");

            verify(projectRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void shouldThrowExceptionWhenProjectNotFound() {
            // Arrange
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> projectService.deleteProject(99L, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("project")
                    .hasMessageContaining("99");

            verify(projectRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== FILTER OPERATIONS ====================

    @Nested
    @DisplayName("Filter Projects Tests")
    class FilterProjectsTests {

        @Test
        @DisplayName("Should find projects by status for authenticated user")
        void shouldFindProjectsByStatusForAuthenticatedUser() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByAppUserIdAndStatus(USER_ID, ProjectStatus.PLANNING)).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByStatus(ProjectStatus.PLANNING, USER_ID);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getStatus()).isEqualTo(ProjectStatus.PLANNING);
            verify(projectRepository).findWithAppUserByAppUserIdAndStatus(USER_ID, ProjectStatus.PLANNING);
        }

        @Test
        @DisplayName("Should search projects by name for authenticated user")
        void shouldSearchProjectsByNameForAuthenticatedUser() {
            // Arrange
            List<Project> projects = List.of(testProject);
            List<ProjectResponseDto> responseDtos = List.of(testProjectResponseDto);

            when(projectRepository.findWithAppUserByAppUserIdAndNameContainingIgnoreCase(USER_ID, "test")).thenReturn(projects);
            when(projectMapper.toResponseDtoList(projects)).thenReturn(responseDtos);

            // Act
            List<ProjectResponseDto> results = projectService.findByNameContaining("test", USER_ID);

            // Assert
            assertThat(results).hasSize(1);
            verify(projectRepository).findWithAppUserByAppUserIdAndNameContainingIgnoreCase(USER_ID, "test");
        }

        @Test
        @DisplayName("Should return empty list when no projects match filters")
        void shouldReturnEmptyListWhenNoProjectsMatchFilters() {
            // Arrange
            List<Project> emptyList = List.of();
            when(projectRepository.findWithAppUserByAppUserIdAndStatus(USER_ID, ProjectStatus.COMPLETED)).thenReturn(emptyList);
            when(projectMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<ProjectResponseDto> results = projectService.findByStatus(ProjectStatus.COMPLETED, USER_ID);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    // ==================== EXISTENCE CHECK OPERATIONS ====================

    @Nested
    @DisplayName("Existence Check Tests")
    class ExistenceCheckTests {

        @Test
        @DisplayName("Should check if project name exists for authenticated user")
        void shouldCheckIfProjectNameExistsForAuthenticatedUser() {
            // Arrange
            when(projectRepository.existsByAppUserIdAndNameIgnoreCase(USER_ID, "Test Project")).thenReturn(true);

            // Act
            boolean result = projectService.existsByName("Test Project", USER_ID);

            // Assert
            assertThat(result).isTrue();
            verify(projectRepository).existsByAppUserIdAndNameIgnoreCase(USER_ID, "Test Project");
        }

        @Test
        @DisplayName("Should return false when project name does not exist for user")
        void shouldReturnFalseWhenProjectNameDoesNotExistForUser() {
            // Arrange
            when(projectRepository.existsByAppUserIdAndNameIgnoreCase(USER_ID, "Nonexistent")).thenReturn(false);

            // Act
            boolean result = projectService.existsByName("Nonexistent", USER_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when checking existence with null userId")
        void shouldThrowExceptionWhenCheckingExistenceWithNullUserId() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.existsByName("Test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("Should throw exception when checking existence with null name")
        void shouldThrowExceptionWhenCheckingExistenceWithNullName() {
            // Act & Assert
            assertThatThrownBy(() -> projectService.existsByName(null, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }
    }
}
