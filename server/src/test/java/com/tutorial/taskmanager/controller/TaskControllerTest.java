package com.tutorial.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.taskmanager.dto.appuser.AppUserSummaryDto;
import com.tutorial.taskmanager.dto.project.ProjectSummaryDto;
import com.tutorial.taskmanager.dto.task.TaskCreateDto;
import com.tutorial.taskmanager.dto.task.TaskResponseDto;
import com.tutorial.taskmanager.dto.task.TaskUpdateDto;
import com.tutorial.taskmanager.enums.ProjectStatus;
import com.tutorial.taskmanager.enums.TaskStatus;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.exception.ValidationException;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.service.TaskService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for TaskController using @WebMvcTest.
 * <p>
 * Tests now include authentication mocking as all endpoints require
 * an authenticated user via @AuthenticationPrincipal.
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TaskController Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    private TaskCreateDto createDto;
    private TaskUpdateDto updateDto;
    private TaskResponseDto responseDto;
    private LocalDateTime dueDate;
    private AppUserDetails userDetails;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        dueDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

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
        createDto = TaskCreateDto.builder()
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .dueDate(dueDate)
                .projectId(1L)
                .build();

        updateDto = TaskUpdateDto.builder()
                .title("Updated Task")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(dueDate)
                .build();

        AppUserSummaryDto userSummary = AppUserSummaryDto.builder()
                .id(USER_ID)
                .username("testuser")
                .build();

        ProjectSummaryDto projectSummary = ProjectSummaryDto.builder()
                .id(1L)
                .name("Test Project")
                .status(ProjectStatus.PLANNING)
                .build();

        responseDto = TaskResponseDto.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .dueDate(dueDate)
                .appUser(userSummary)
                .project(projectSummary)
                .build();
    }

    // ========================================================================
    // CREATE TESTS - POST /api/tasks
    // ========================================================================

    @Nested
    @DisplayName("POST /api/tasks - Create Task Tests")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task and return 201 Created")
        void createTask_Success_Returns201() throws Exception {
            when(taskService.createTask(any(TaskCreateDto.class), eq(USER_ID))).thenReturn(responseDto);

            mockMvc.perform(post("/api/tasks")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("Test Task"))
                    .andExpect(jsonPath("$.status").value("TODO"));

            verify(taskService).createTask(any(TaskCreateDto.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Should return 404 when project not found")
        void createTask_ProjectNotFound_Returns404() throws Exception {
            when(taskService.createTask(any(TaskCreateDto.class), eq(USER_ID)))
                    .thenThrow(new ResourceNotFoundException("project", 999L));

            mockMvc.perform(post("/api/tasks")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when project doesn't belong to user")
        void createTask_ProjectNotBelongToUser_Returns400() throws Exception {
            when(taskService.createTask(any(TaskCreateDto.class), eq(USER_ID)))
                    .thenThrow(new ValidationException("Project does not belong to authenticated user"));

            mockMvc.perform(post("/api/tasks")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // READ TESTS - GET /api/tasks, GET /api/tasks/{id}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/tasks - Read Task Tests")
    class ReadTaskTests {

        @Test
        @DisplayName("Should return task by ID with 200 OK")
        void getTaskById_Found_Returns200() throws Exception {
            when(taskService.getById(1L, USER_ID)).thenReturn(responseDto);

            mockMvc.perform(get("/api/tasks/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("Test Task"));

            verify(taskService).getById(1L, USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when task ID not found")
        void getTaskById_NotFound_Returns404() throws Exception {
            when(taskService.getById(999L, USER_ID))
                    .thenThrow(new ResourceNotFoundException("task", 999L));

            mockMvc.perform(get("/api/tasks/{id}", 999L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return all tasks for user with 200 OK")
        void getAllTasks_Success_Returns200() throws Exception {
            TaskResponseDto task2 = TaskResponseDto.builder()
                    .id(2L)
                    .title("Another Task")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();
            List<TaskResponseDto> tasks = Arrays.asList(responseDto, task2);
            when(taskService.findAll(USER_ID)).thenReturn(tasks);

            mockMvc.perform(get("/api/tasks")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2));

            verify(taskService).findAll(USER_ID);
        }

        @Test
        @DisplayName("Should return empty list with 200 OK when no tasks exist")
        void getAllTasks_EmptyList_Returns200() throws Exception {
            when(taskService.findAll(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/tasks")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ========================================================================
    // FILTER TESTS - GET /api/tasks with query params
    // ========================================================================

    @Nested
    @DisplayName("GET /api/tasks?params - Filter Task Tests")
    class FilterTasksTests {

        @Test
        @DisplayName("Should filter tasks by projectId")
        void getTasks_FilterByProjectId_Returns200() throws Exception {
            when(taskService.findByProjectId(1L, USER_ID)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/tasks")
                            .param("projectId", "1")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(taskService).findByProjectId(1L, USER_ID);
        }

        @Test
        @DisplayName("Should filter tasks by status")
        void getTasks_FilterByStatus_Returns200() throws Exception {
            when(taskService.findByStatus(TaskStatus.TODO, USER_ID)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/tasks")
                            .param("status", "TODO")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(taskService).findByStatus(TaskStatus.TODO, USER_ID);
        }

        @Test
        @DisplayName("Should filter tasks by projectId and status")
        void getTasks_FilterByProjectIdAndStatus_Returns200() throws Exception {
            when(taskService.findByProjectIdAndStatus(1L, TaskStatus.TODO, USER_ID))
                    .thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/tasks")
                            .param("projectId", "1")
                            .param("status", "TODO")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(taskService).findByProjectIdAndStatus(1L, TaskStatus.TODO, USER_ID);
        }

        @Test
        @DisplayName("Should return overdue tasks")
        void getTasks_FilterByOverdue_Returns200() throws Exception {
            when(taskService.findOverdueTasks(USER_ID)).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/tasks")
                            .param("overdue", "true")
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(taskService).findOverdueTasks(USER_ID);
        }
    }

    // ========================================================================
    // UPDATE TESTS - PUT /api/tasks/{id}
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/tasks/{id} - Update Task Tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task and return 200 OK")
        void updateTask_Success_Returns200() throws Exception {
            TaskResponseDto updatedResponse = TaskResponseDto.builder()
                    .id(1L)
                    .title("Updated Task")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();
            when(taskService.updateTask(eq(1L), any(TaskUpdateDto.class), eq(USER_ID)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/tasks/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Task"));

            verify(taskService).updateTask(eq(1L), any(TaskUpdateDto.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent task")
        void updateTask_NotFound_Returns404() throws Exception {
            when(taskService.updateTask(eq(999L), any(TaskUpdateDto.class), eq(USER_ID)))
                    .thenThrow(new ResourceNotFoundException("task", 999L));

            mockMvc.perform(put("/api/tasks/{id}", 999L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // PROJECT ASSIGNMENT TESTS
    // ========================================================================

    @Nested
    @DisplayName("Project Assignment Tests")
    class ProjectAssignmentTests {

        @Test
        @DisplayName("Should assign task to project and return 200 OK")
        void assignToProject_Success_Returns200() throws Exception {
            ProjectSummaryDto project2Summary = ProjectSummaryDto.builder()
                    .id(2L)
                    .name("Project 2")
                    .status(ProjectStatus.ACTIVE)
                    .build();
            TaskResponseDto assignedTask = TaskResponseDto.builder()
                    .id(1L)
                    .title("Test Task")
                    .project(project2Summary)
                    .build();
            when(taskService.assignToProject(1L, 2L, USER_ID)).thenReturn(assignedTask);

            mockMvc.perform(put("/api/tasks/{id}/project/{projectId}", 1L, 2L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.project.id").value(2L));

            verify(taskService).assignToProject(1L, 2L, USER_ID);
        }

        @Test
        @DisplayName("Should return 400 when project doesn't belong to user")
        void assignToProject_ProjectNotBelongToUser_Returns400() throws Exception {
            when(taskService.assignToProject(1L, 2L, USER_ID))
                    .thenThrow(new ValidationException("Project does not belong to authenticated user"));

            mockMvc.perform(put("/api/tasks/{id}/project/{projectId}", 1L, 2L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should remove task from project and return 200 OK")
        void removeFromProject_Success_Returns200() throws Exception {
            TaskResponseDto taskWithoutProject = TaskResponseDto.builder()
                    .id(1L)
                    .title("Test Task")
                    .project(null)
                    .build();
            when(taskService.removeFromProject(1L, USER_ID)).thenReturn(taskWithoutProject);

            mockMvc.perform(delete("/api/tasks/{id}/project", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.project").doesNotExist());

            verify(taskService).removeFromProject(1L, USER_ID);
        }
    }

    // ========================================================================
    // DELETE TESTS - DELETE /api/tasks/{id}
    // ========================================================================

    @Nested
    @DisplayName("DELETE /api/tasks/{id} - Delete Task Tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task and return 204 No Content")
        void deleteTask_Success_Returns204() throws Exception {
            doNothing().when(taskService).deleteTask(1L, USER_ID);

            mockMvc.perform(delete("/api/tasks/{id}", 1L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isNoContent());

            verify(taskService).deleteTask(1L, USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent task")
        void deleteTask_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("task", 999L))
                    .when(taskService).deleteTask(999L, USER_ID);

            mockMvc.perform(delete("/api/tasks/{id}", 999L)
                            .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())))
                    .andExpect(status().isNotFound());
        }
    }
}
