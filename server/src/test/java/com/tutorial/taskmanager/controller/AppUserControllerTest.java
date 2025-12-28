package com.tutorial.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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
 * Slice tests for AppUserController using @WebMvcTest.
 *
 * <p><strong>What is @WebMvcTest?</strong>
 * <p>A Spring Boot test slice that only loads the web layer (controllers, filters, etc.)
 * without loading the full application context. This makes tests fast and focused.
 *
 * <p><strong>Key Components:</strong>
 * <ul>
 *   <li>{@code @WebMvcTest(AppUserController.class)} - Only loads AppUserController</li>
 *   <li>{@code MockMvc} - Simulates HTTP requests without starting a real server</li>
 *   <li>{@code @MockitoBean} - Creates mock of AppUserService and puts it in Spring context</li>
 *   <li>{@code ObjectMapper} - Converts objects to/from JSON</li>
 * </ul>
 *
 * <p><strong>Testing Strategy:</strong>
 * <ul>
 *   <li>Test HTTP request handling (path, method, content type)</li>
 *   <li>Test response status codes (200, 201, 204, 400, 404)</li>
 *   <li>Test JSON request/response serialization</li>
 *   <li>Verify service method invocations</li>
 * </ul>
 *
 * <p><strong>Test Organization:</strong>
 * <ul>
 *   <li>CreateUserTests - POST /api/users</li>
 *   <li>ReadUserTests - GET /api/users, GET /api/users/{id}, GET /api/users/username/{username}</li>
 *   <li>UpdateUserTests - PUT /api/users/{id}</li>
 *   <li>DeleteUserTests - DELETE /api/users/{id}</li>
 * </ul>
 */
@WebMvcTest(AppUserController.class)
@AutoConfigureMockMvc(addFilters = false)  // Disable security filters for unit testing
@DisplayName("AppUserController Tests")
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppUserService appUserService;

    // Security beans required for component scanning (filters disabled via addFilters=false)
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    private AppUserCreateDto createDto;
    private AppUserUpdateDto updateDto;
    private AppUserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Test data setup
        createDto = AppUserCreateDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        updateDto = AppUserUpdateDto.builder()
                .email("updated@example.com")
                .password("newpassword123")
                .build();

        responseDto = AppUserResponseDto.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    // ========================================================================
    // CREATE TESTS - POST /api/users
    // ========================================================================

    @Nested
    @DisplayName("POST /api/users - Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user and return 201 Created")
        void createUser_Success_Returns201() throws Exception {
            // Arrange
            when(appUserService.createAppUser(any(AppUserCreateDto.class))).thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(appUserService).createAppUser(any(AppUserCreateDto.class));
        }

        @Test
        @DisplayName("Should return 400 when username already exists")
        void createUser_DuplicateUsername_Returns400() throws Exception {
            // Arrange
            when(appUserService.createAppUser(any(AppUserCreateDto.class)))
                    .thenThrow(new IllegalArgumentException("username already exists"));

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isBadRequest());

            verify(appUserService).createAppUser(any(AppUserCreateDto.class));
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void createUser_DuplicateEmail_Returns400() throws Exception {
            // Arrange
            when(appUserService.createAppUser(any(AppUserCreateDto.class)))
                    .thenThrow(new IllegalArgumentException("email already exists"));

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // READ TESTS - GET /api/users, GET /api/users/{id}, GET /api/users/username/{username}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/users - Read User Tests")
    class ReadUserTests {

        @Test
        @DisplayName("Should return user by ID with 200 OK")
        void getUserById_Found_Returns200() throws Exception {
            // Arrange
            when(appUserService.getById(1L)).thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get("/api/users/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(appUserService).getById(1L);
        }

        @Test
        @DisplayName("Should return 404 when user ID not found")
        void getUserById_NotFound_Returns404() throws Exception {
            // Arrange
            when(appUserService.getById(999L))
                    .thenThrow(new ResourceNotFoundException("appUser", 999L));

            // Act & Assert
            mockMvc.perform(get("/api/users/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(appUserService).getById(999L);
        }

        @Test
        @DisplayName("Should return all users with 200 OK")
        void getAllUsers_Success_Returns200() throws Exception {
            // Arrange
            AppUserResponseDto user2 = AppUserResponseDto.builder()
                    .id(2L)
                    .username("anotheruser")
                    .email("another@example.com")
                    .build();
            List<AppUserResponseDto> users = Arrays.asList(responseDto, user2);
            when(appUserService.findAll()).thenReturn(users);

            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].username").value("testuser"))
                    .andExpect(jsonPath("$[1].id").value(2L))
                    .andExpect(jsonPath("$[1].username").value("anotheruser"));

            verify(appUserService).findAll();
        }

        @Test
        @DisplayName("Should return empty list with 200 OK when no users exist")
        void getAllUsers_EmptyList_Returns200() throws Exception {
            // Arrange
            when(appUserService.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(0));

            verify(appUserService).findAll();
        }

        @Test
        @DisplayName("Should return user by username with 200 OK")
        void getUserByUsername_Found_Returns200() throws Exception {
            // Arrange
            when(appUserService.getByUsername("testuser")).thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get("/api/users/username/{username}", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(appUserService).getByUsername("testuser");
        }

        @Test
        @DisplayName("Should return 404 when username not found")
        void getUserByUsername_NotFound_Returns404() throws Exception {
            // Arrange
            when(appUserService.getByUsername("unknownuser"))
                    .thenThrow(new ResourceNotFoundException("appUser with username 'unknownuser' not found"));

            // Act & Assert
            mockMvc.perform(get("/api/users/username/{username}", "unknownuser"))
                    .andExpect(status().isNotFound());

            verify(appUserService).getByUsername("unknownuser");
        }
    }

    // ========================================================================
    // UPDATE TESTS - PUT /api/users/{id}
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/users/{id} - Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user and return 200 OK")
        void updateUser_Success_Returns200() throws Exception {
            // Arrange
            AppUserResponseDto updatedResponse = AppUserResponseDto.builder()
                    .id(1L)
                    .username("testuser")
                    .email("updated@example.com")
                    .build();
            when(appUserService.updateAppUser(eq(1L), any(AppUserUpdateDto.class)))
                    .thenReturn(updatedResponse);

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.email").value("updated@example.com"));

            verify(appUserService).updateAppUser(eq(1L), any(AppUserUpdateDto.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent user")
        void updateUser_NotFound_Returns404() throws Exception {
            // Arrange
            when(appUserService.updateAppUser(eq(999L), any(AppUserUpdateDto.class)))
                    .thenThrow(new ResourceNotFoundException("appUser", 999L));

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());

            verify(appUserService).updateAppUser(eq(999L), any(AppUserUpdateDto.class));
        }

        @Test
        @DisplayName("Should return 400 when new email already exists")
        void updateUser_DuplicateEmail_Returns400() throws Exception {
            // Arrange
            when(appUserService.updateAppUser(eq(1L), any(AppUserUpdateDto.class)))
                    .thenThrow(new IllegalArgumentException("email already exists"));

            // Act & Assert
            mockMvc.perform(put("/api/users/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest());

            verify(appUserService).updateAppUser(eq(1L), any(AppUserUpdateDto.class));
        }
    }

    // ========================================================================
    // DELETE TESTS - DELETE /api/users/{id}
    // ========================================================================

    @Nested
    @DisplayName("DELETE /api/users/{id} - Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user and return 204 No Content")
        void deleteUser_Success_Returns204() throws Exception {
            // Arrange
            doNothing().when(appUserService).deleteById(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/users/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(appUserService).deleteById(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        void deleteUser_NotFound_Returns404() throws Exception {
            // Arrange
            doThrow(new ResourceNotFoundException("appUser", 999L))
                    .when(appUserService).deleteById(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/users/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(appUserService).deleteById(999L);
        }
    }
}
