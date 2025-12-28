package com.tutorial.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
import com.tutorial.taskmanager.dto.project.ProjectCreateDto;
import com.tutorial.taskmanager.dto.project.ProjectResponseDto;
import com.tutorial.taskmanager.dto.project.ProjectUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.exception.ValidationException;
import com.tutorial.taskmanager.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tutorial.taskmanager.config.SecurityConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for ProjectController using @WebMvcTest.
 *
 * <p><strong>Test Organization:</strong>
 * <ul>
 *   <li>CreateProjectTests - POST /api/projects</li>
 *   <li>ReadProjectTests - GET /api/projects, GET /api/projects/{id}</li>
 *   <li>FilterProjectsTests - GET /api/projects with query params</li>
 *   <li>UpdateProjectTests - PUT /api/projects/{id}</li>
 *   <li>DeleteProjectTests - DELETE /api/projects/{id}</li>
 * </ul>
 */
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@DisplayName("ProjectController Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    private ProjectCreateDto createDto;
    private ProjectUpdateDto updateDto;
    private ProjectResponseDto responseDto;

    @BeforeEach
    void setUp() {
        createDto = ProjectCreateDto.builder()
                .name("Test Project")
                .description("Test Description")
                .status(ProjectStatus.PLANNING)
                .appUserId(1L)
                .build();

        updateDto = ProjectUpdateDto.builder()
                .name("Updated Project")
                .description("Updated Description")
                .status(ProjectStatus.ACTIVE)
                .build();

        AppUserSummaryDto userSummary = AppUserSummaryDto.builder()
                .id(1L)
                .username("testuser")
                .build();

        responseDto = ProjectResponseDto.builder()
                .id(1L)
                .name("Test Project")
                .description("Test Description")
                .status(ProjectStatus.PLANNING)
                .appUser(userSummary)
                .build();
    }

    // ========================================================================
    // CREATE TESTS - POST /api/projects
    // ========================================================================

    @Nested
    @DisplayName("POST /api/projects - Create Project Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project and return 201 Created")
        void createProject_Success_Returns201() throws Exception {
            when(projectService.createProject(any(ProjectCreateDto.class))).thenReturn(responseDto);

            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Test Project"))
                    .andExpect(jsonPath("$.status").value("PLANNING"))
                    .andExpect(jsonPath("$.appUser.id").value(1L));

            verify(projectService).createProject(any(ProjectCreateDto.class));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void createProject_UserNotFound_Returns404() throws Exception {
            when(projectService.createProject(any(ProjectCreateDto.class)))
                    .thenThrow(new ResourceNotFoundException("appUser", 999L));

            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when project name already exists for user")
        void createProject_DuplicateName_Returns400() throws Exception {
            when(projectService.createProject(any(ProjectCreateDto.class)))
                    .thenThrow(new ValidationException("user with project name already exists"));

            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // READ TESTS - GET /api/projects, GET /api/projects/{id}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/projects - Read Project Tests")
    class ReadProjectTests {

        @Test
        @DisplayName("Should return project by ID with 200 OK")
        void getProjectById_Found_Returns200() throws Exception {
            when(projectService.getById(1L)).thenReturn(responseDto);

            mockMvc.perform(get("/api/projects/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Test Project"));

            verify(projectService).getById(1L);
        }

        @Test
        @DisplayName("Should return 404 when project ID not found")
        void getProjectById_NotFound_Returns404() throws Exception {
            when(projectService.getById(999L))
                    .thenThrow(new ResourceNotFoundException("project", 999L));

            mockMvc.perform(get("/api/projects/{id}", 999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return all projects with 200 OK")
        void getAllProjects_Success_Returns200() throws Exception {
            AppUserSummaryDto userSummary = AppUserSummaryDto.builder()
                    .id(1L)
                    .username("testuser")
                    .build();
            ProjectResponseDto project2 = ProjectResponseDto.builder()
                    .id(2L)
                    .name("Another Project")
                    .status(ProjectStatus.ACTIVE)
                    .appUser(userSummary)
                    .build();
            List<ProjectResponseDto> projects = Arrays.asList(responseDto, project2);
            when(projectService.findAll()).thenReturn(projects);

            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[1].id").value(2L));

            verify(projectService).findAll();
        }

        @Test
        @DisplayName("Should return empty list with 200 OK when no projects exist")
        void getAllProjects_EmptyList_Returns200() throws Exception {
            when(projectService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ========================================================================
    // FILTER TESTS - GET /api/projects with query params
    // ========================================================================

    @Nested
    @DisplayName("GET /api/projects?params - Filter Project Tests")
    class FilterProjectsTests {

        @Test
        @DisplayName("Should filter projects by userId")
        void getProjects_FilterByUserId_Returns200() throws Exception {
            when(projectService.findByAppUserId(1L)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].appUser.id").value(1L));

            verify(projectService).findByAppUserId(1L);
        }

        @Test
        @DisplayName("Should filter projects by status")
        void getProjects_FilterByStatus_Returns200() throws Exception {
            when(projectService.findByStatus(ProjectStatus.PLANNING)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects").param("status", "PLANNING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("PLANNING"));

            verify(projectService).findByStatus(ProjectStatus.PLANNING);
        }

        @Test
        @DisplayName("Should filter projects by userId and status")
        void getProjects_FilterByUserIdAndStatus_Returns200() throws Exception {
            when(projectService.findByAppUserIdAndStatus(1L, ProjectStatus.PLANNING))
                    .thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects")
                            .param("userId", "1")
                            .param("status", "PLANNING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(projectService).findByAppUserIdAndStatus(1L, ProjectStatus.PLANNING);
        }

        @Test
        @DisplayName("Should filter projects by name search")
        void getProjects_FilterByName_Returns200() throws Exception {
            when(projectService.findByNameContaining("Test")).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects").param("name", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Test Project"));

            verify(projectService).findByNameContaining("Test");
        }

        @Test
        @DisplayName("Should filter projects by userId and name search")
        void getProjects_FilterByUserIdAndName_Returns200() throws Exception {
            when(projectService.findByAppUserIdAndNameContaining(1L, "Test"))
                    .thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects")
                            .param("userId", "1")
                            .param("name", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(projectService).findByAppUserIdAndNameContaining(1L, "Test");
        }
    }

    // ========================================================================
    // UPDATE TESTS - PUT /api/projects/{id}
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/projects/{id} - Update Project Tests")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should update project and return 200 OK")
        void updateProject_Success_Returns200() throws Exception {
            AppUserSummaryDto userSummary = AppUserSummaryDto.builder()
                    .id(1L)
                    .username("testuser")
                    .build();
            ProjectResponseDto updatedResponse = ProjectResponseDto.builder()
                    .id(1L)
                    .name("Updated Project")
                    .description("Updated Description")
                    .status(ProjectStatus.ACTIVE)
                    .appUser(userSummary)
                    .build();
            when(projectService.updateProject(eq(1L), any(ProjectUpdateDto.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/projects/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Project"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(projectService).updateProject(eq(1L), any(ProjectUpdateDto.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent project")
        void updateProject_NotFound_Returns404() throws Exception {
            when(projectService.updateProject(eq(999L), any(ProjectUpdateDto.class)))
                    .thenThrow(new ResourceNotFoundException("project", 999L));

            mockMvc.perform(put("/api/projects/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when updating with duplicate name")
        void updateProject_DuplicateName_Returns400() throws Exception {
            when(projectService.updateProject(eq(1L), any(ProjectUpdateDto.class)))
                    .thenThrow(new ValidationException("name already exists"));

            mockMvc.perform(put("/api/projects/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // DELETE TESTS - DELETE /api/projects/{id}
    // ========================================================================

    @Nested
    @DisplayName("DELETE /api/projects/{id} - Delete Project Tests")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should delete project and return 204 No Content")
        void deleteProject_Success_Returns204() throws Exception {
            doNothing().when(projectService).deleteProject(1L);

            mockMvc.perform(delete("/api/projects/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(projectService).deleteProject(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent project")
        void deleteProject_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("project", 999L))
                    .when(projectService).deleteProject(999L);

            mockMvc.perform(delete("/api/projects/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}
