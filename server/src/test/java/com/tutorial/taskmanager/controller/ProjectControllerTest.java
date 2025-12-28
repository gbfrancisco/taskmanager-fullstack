package com.tutorial.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
import com.tutorial.taskmanager.dto.project.ProjectCreateDto;
import com.tutorial.taskmanager.dto.project.ProjectResponseDto;
import com.tutorial.taskmanager.dto.project.ProjectUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.exception.ValidationException;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tutorial.taskmanager.security.JwtService;
import com.tutorial.taskmanager.security.AppUserDetailsService;

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
 * <p>
 * Tests now include authentication mocking as all endpoints require
 * an authenticated user via @AuthenticationPrincipal.
 */
@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProjectController Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    private ProjectCreateDto createDto;
    private ProjectUpdateDto updateDto;
    private ProjectResponseDto responseDto;
    private AppUserDetails userDetails;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Set up authenticated user
        AppUser testUser = new AppUser();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        userDetails = new AppUserDetails(testUser);

        // Set up security context
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // No appUserId in createDto anymore
        createDto = ProjectCreateDto.builder()
                .name("Test Project")
                .description("Test Description")
                .status(ProjectStatus.PLANNING)
                .build();

        updateDto = ProjectUpdateDto.builder()
                .name("Updated Project")
                .description("Updated Description")
                .status(ProjectStatus.ACTIVE)
                .build();

        AppUserSummaryDto userSummary = AppUserSummaryDto.builder()
                .id(USER_ID)
                .username("testuser")
                .build();

        responseDto = ProjectResponseDto.builder()
                .id(1L)
                .name("Test Project")
                .description("Test Description")
                .status(ProjectStatus.PLANNING)
                .appUser(userSummary)
                .taskCount(0L)
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
            when(projectService.createProject(any(ProjectCreateDto.class), eq(USER_ID))).thenReturn(responseDto);

            mockMvc.perform(post("/api/projects")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Test Project"))
                    .andExpect(jsonPath("$.status").value("PLANNING"));

            verify(projectService).createProject(any(ProjectCreateDto.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Should return 400 when project name already exists for user")
        void createProject_DuplicateName_Returns400() throws Exception {
            when(projectService.createProject(any(ProjectCreateDto.class), eq(USER_ID)))
                    .thenThrow(new ValidationException("user with project name already exists"));

            mockMvc.perform(post("/api/projects")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
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
            when(projectService.getById(1L, USER_ID)).thenReturn(responseDto);

            mockMvc.perform(get("/api/projects/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Test Project"));

            verify(projectService).getById(1L, USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when project ID not found")
        void getProjectById_NotFound_Returns404() throws Exception {
            when(projectService.getById(999L, USER_ID))
                    .thenThrow(new ResourceNotFoundException("project", 999L));

            mockMvc.perform(get("/api/projects/{id}", 999L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return all projects for user with 200 OK")
        void getAllProjects_Success_Returns200() throws Exception {
            AppUserSummaryDto userSummary = AppUserSummaryDto.builder()
                    .id(USER_ID)
                    .username("testuser")
                    .build();
            ProjectResponseDto project2 = ProjectResponseDto.builder()
                    .id(2L)
                    .name("Another Project")
                    .status(ProjectStatus.ACTIVE)
                    .appUser(userSummary)
                    .build();
            List<ProjectResponseDto> projects = Arrays.asList(responseDto, project2);
            when(projectService.findAll(USER_ID)).thenReturn(projects);

            mockMvc.perform(get("/api/projects")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2));

            verify(projectService).findAll(USER_ID);
        }

        @Test
        @DisplayName("Should return empty list with 200 OK when no projects exist")
        void getAllProjects_EmptyList_Returns200() throws Exception {
            when(projectService.findAll(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/projects")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
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
        @DisplayName("Should filter projects by status")
        void getProjects_FilterByStatus_Returns200() throws Exception {
            when(projectService.findByStatus(ProjectStatus.PLANNING, USER_ID)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects")
                            .param("status", "PLANNING")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(projectService).findByStatus(ProjectStatus.PLANNING, USER_ID);
        }

        @Test
        @DisplayName("Should filter projects by name search")
        void getProjects_FilterByName_Returns200() throws Exception {
            when(projectService.findByNameContaining("Test", USER_ID)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/projects")
                            .param("name", "Test")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(projectService).findByNameContaining("Test", USER_ID);
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
                    .id(USER_ID)
                    .username("testuser")
                    .build();
            ProjectResponseDto updatedResponse = ProjectResponseDto.builder()
                    .id(1L)
                    .name("Updated Project")
                    .description("Updated Description")
                    .status(ProjectStatus.ACTIVE)
                    .appUser(userSummary)
                    .build();
            when(projectService.updateProject(eq(1L), any(ProjectUpdateDto.class), eq(USER_ID)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/projects/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Project"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(projectService).updateProject(eq(1L), any(ProjectUpdateDto.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent project")
        void updateProject_NotFound_Returns404() throws Exception {
            when(projectService.updateProject(eq(999L), any(ProjectUpdateDto.class), eq(USER_ID)))
                    .thenThrow(new ResourceNotFoundException("project", 999L));

            mockMvc.perform(put("/api/projects/{id}", 999L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when updating with duplicate name")
        void updateProject_DuplicateName_Returns400() throws Exception {
            when(projectService.updateProject(eq(1L), any(ProjectUpdateDto.class), eq(USER_ID)))
                    .thenThrow(new ValidationException("name already exists"));

            mockMvc.perform(put("/api/projects/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
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
            doNothing().when(projectService).deleteProject(1L, USER_ID);

            mockMvc.perform(delete("/api/projects/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isNoContent());

            verify(projectService).deleteProject(1L, USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent project")
        void deleteProject_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("project", 999L))
                    .when(projectService).deleteProject(999L, USER_ID);

            mockMvc.perform(delete("/api/projects/{id}", 999L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isNotFound());
        }
    }
}
