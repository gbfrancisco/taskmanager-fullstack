package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AppUserService
 *
 * Testing Strategy:
 * - Use Mockito to mock the AppUserRepository and AppUserMapper dependencies
 * - Test business logic and validation in isolation
 * - Verify interactions with the repository and mapper
 * - Test both success and failure scenarios
 *
 * Test organization:
 * - Create operations (createAppUser)
 * - Read operations (findById, getById, findByUsername, getByUsername, findByEmail, findAll)
 * - Update operations (updateAppUser)
 * - Delete operations (deleteById)
 * - Existence checks (existsByUsername, existsByEmail)
 *
 * Key Annotations:
 * - @ExtendWith(MockitoExtension.class): Enables Mockito in JUnit 5
 * - @Mock: Creates mock instances of dependencies
 * - @InjectMocks: Creates AppUserService and injects the mocked dependencies
 * - @Nested: Groups related tests for better organization
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppUserService Tests")
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    private AppUser testUser;
    private AppUserCreateDto createDto;
    private AppUserUpdateDto updateDto;
    private AppUserResponseDto testUserResponseDto;

    /**
     * Setup method that runs before each test
     * Creates test data that can be reused across tests
     */
    @BeforeEach
    void setUp() {
        // Stub password encoder to return encoded password (lenient - not all tests use it)
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
            "encoded_" + invocation.getArgument(0));

        // Create a test user entity
        testUser = AppUser.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();
        // Simulate that this user has been saved (has an ID)
        testUser.setId(1L);

        // Create response DTO for the test user
        testUserResponseDto = AppUserResponseDto.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();

        // Create DTOs for testing
        createDto = AppUserCreateDto.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password123")
            .build();
        updateDto = AppUserUpdateDto.builder()
            .email("updated@example.com")
            .password("newPassword123")
            .build();
    }

    // ==================== CREATE OPERATIONS ====================

    @Nested
    @DisplayName("Create AppUser Tests")
    class CreateAppUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            // Arrange: Setup mock behavior
            when(appUserRepository.existsByUsername(anyString())).thenReturn(false);
            when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
            when(appUserMapper.toEntity(createDto)).thenReturn(testUser);
            when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act: Call the method under test
            AppUserResponseDto result = appUserService.createAppUser(createDto);

            // Assert: Verify the result and interactions
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
            verify(appUserRepository).existsByUsername("newuser");
            verify(appUserRepository).existsByEmail("new@example.com");
            verify(appUserMapper).toEntity(createDto);
            verify(appUserRepository).save(any(AppUser.class));
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when DTO is null")
        void shouldThrowExceptionWhenDtoIsNull() {
            // Act & Assert: Verify exception is thrown
            assertThatThrownBy(() -> appUserService.createAppUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appUserCreateDto cannot be null");

            // Verify no repository interactions occurred
            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when username is empty")
        void shouldThrowExceptionWhenUsernameIsEmpty() {
            // Arrange: Create DTO with empty username
            AppUserCreateDto emptyUsernameDto = new AppUserCreateDto("", "test@example.com", "password");

            // Act & Assert
            assertThatThrownBy(() -> appUserService.createAppUser(emptyUsernameDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when username is null")
        void shouldThrowExceptionWhenUsernameIsNull() {
            // Arrange
            AppUserCreateDto nullUsernameDto = new AppUserCreateDto(null, "test@example.com", "password");

            // Act & Assert
            assertThatThrownBy(() -> appUserService.createAppUser(nullUsernameDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when email is empty")
        void shouldThrowExceptionWhenEmailIsEmpty() {
            // Arrange
            AppUserCreateDto emptyEmailDto = new AppUserCreateDto("testuser", "", "password");

            // Act & Assert
            assertThatThrownBy(() -> appUserService.createAppUser(emptyEmailDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() {
            // Arrange: Mock that username already exists
            when(appUserRepository.existsByUsername("newuser")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> appUserService.createAppUser(createDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username already exists");

            // Verify that we checked username but never tried to save
            verify(appUserRepository).existsByUsername("newuser");
            verify(appUserRepository, never()).save(any(AppUser.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Arrange: Username is unique but email exists
            when(appUserRepository.existsByUsername("newuser")).thenReturn(false);
            when(appUserRepository.existsByEmail("new@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> appUserService.createAppUser(createDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email already exists");

            verify(appUserRepository).existsByUsername("newuser");
            verify(appUserRepository).existsByEmail("new@example.com");
            verify(appUserRepository, never()).save(any(AppUser.class));
        }
    }

    // ==================== READ OPERATIONS ====================

    @Nested
    @DisplayName("Read AppUser Tests")
    class ReadAppUserTests {

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            Optional<AppUserResponseDto> result = appUserService.findById(1L);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getUsername()).isEqualTo("testuser");
            verify(appUserRepository).findById(1L);
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should return empty Optional when user not found by ID")
        void shouldReturnEmptyWhenUserNotFoundById() {
            // Arrange
            when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            Optional<AppUserResponseDto> result = appUserService.findById(999L);

            // Assert
            assertThat(result).isEmpty();
            verify(appUserRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw exception when finding by null ID")
        void shouldThrowExceptionWhenFindingByNullId() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id cannot be null");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should get user by ID")
        void shouldGetUserById() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            AppUserResponseDto result = appUserService.getById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(appUserRepository).findById(1L);
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent user by ID")
        void shouldThrowExceptionWhenGettingNonExistentUserById() {
            // Arrange
            when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> appUserService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("appUser with id '999' not found");

            verify(appUserRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw exception when getting by null ID")
        void shouldThrowExceptionWhenGettingByNullId() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.getById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id cannot be null");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUserByUsername() {
            // Arrange
            when(appUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            Optional<AppUserResponseDto> result = appUserService.findByUsername("testuser");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
            verify(appUserRepository).findByUsername("testuser");
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should return empty Optional when user not found by username")
        void shouldReturnEmptyWhenUserNotFoundByUsername() {
            // Arrange
            when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act
            Optional<AppUserResponseDto> result = appUserService.findByUsername("nonexistent");

            // Assert
            assertThat(result).isEmpty();
            verify(appUserRepository).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should throw exception when finding by empty username")
        void shouldThrowExceptionWhenFindingByEmptyUsername() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.findByUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should get user by username")
        void shouldGetUserByUsername() {
            // Arrange
            when(appUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            AppUserResponseDto result = appUserService.getByUsername("testuser");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(appUserRepository).findByUsername("testuser");
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent user by username")
        void shouldThrowExceptionWhenGettingNonExistentUserByUsername() {
            // Arrange
            when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> appUserService.getByUsername("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("appUser with username 'nonexistent' not found");

            verify(appUserRepository).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should throw exception when getting by empty username")
        void shouldThrowExceptionWhenGettingByEmptyUsername() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.getByUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            // Arrange
            when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            Optional<AppUserResponseDto> result = appUserService.findByEmail("test@example.com");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
            verify(appUserRepository).findByEmail("test@example.com");
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should return empty Optional when user not found by email")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            // Arrange
            when(appUserRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act
            Optional<AppUserResponseDto> result = appUserService.findByEmail("notfound@example.com");

            // Assert
            assertThat(result).isEmpty();
            verify(appUserRepository).findByEmail("notfound@example.com");
        }

        @Test
        @DisplayName("Should throw exception when finding by empty email")
        void shouldThrowExceptionWhenFindingByEmptyEmail() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.findByEmail(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should find all users")
        void shouldFindAllUsers() {
            // Arrange
            AppUser user2 = AppUser.builder()
                .username("user2")
                .email("user2@example.com")
                .password("pass")
                .build();
            user2.setId(2L);

            AppUserResponseDto user2ResponseDto = AppUserResponseDto.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .build();

            List<AppUser> users = List.of(testUser, user2);
            List<AppUserResponseDto> responseDtos = List.of(testUserResponseDto, user2ResponseDto);

            when(appUserRepository.findAll()).thenReturn(users);
            when(appUserMapper.toResponseDtoList(users)).thenReturn(responseDtos);

            // Act
            List<AppUserResponseDto> result = appUserService.findAll();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AppUserResponseDto::getUsername)
                .containsExactly("testuser", "user2");
            verify(appUserRepository).findAll();
            verify(appUserMapper).toResponseDtoList(users);
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsersExist() {
            // Arrange
            List<AppUser> emptyList = List.of();
            when(appUserRepository.findAll()).thenReturn(emptyList);
            when(appUserMapper.toResponseDtoList(emptyList)).thenReturn(List.of());

            // Act
            List<AppUserResponseDto> result = appUserService.findAll();

            // Assert
            assertThat(result).isEmpty();
            verify(appUserRepository).findAll();
            verify(appUserMapper).toResponseDtoList(emptyList);
        }
    }

    // ==================== UPDATE OPERATIONS ====================

    @Nested
    @DisplayName("Update AppUser Tests")
    class UpdateAppUserTests {

        @Test
        @DisplayName("Should update email and password successfully")
        void shouldUpdateEmailAndPasswordSuccessfully() {
            // Arrange
            AppUserResponseDto updatedResponseDto = AppUserResponseDto.builder()
                .id(1L)
                .username("testuser")
                .email("updated@example.com")
                .build();

            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserRepository.existsByEmail("updated@example.com")).thenReturn(false);
            when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
            when(appUserMapper.toResponseDto(testUser)).thenReturn(updatedResponseDto);

            // Act
            AppUserResponseDto result = appUserService.updateAppUser(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            verify(appUserRepository).findById(1L);
            verify(appUserRepository).existsByEmail("updated@example.com");
            verify(appUserMapper).patchEntityFromDto(updateDto, testUser);
            verify(appUserRepository).save(testUser);
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should allow keeping same email (case-insensitive)")
        void shouldAllowKeepingSameEmailCaseInsensitive() {
            // Arrange: User wants to keep their email (different case)
            updateDto.setEmail("TEST@EXAMPLE.COM"); // Same email, different case
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            AppUserResponseDto result = appUserService.updateAppUser(1L, updateDto);

            // Assert: Should not check existence (same email)
            assertThat(result).isNotNull();
            verify(appUserRepository).findById(1L);
            verify(appUserRepository, never()).existsByEmail(anyString());
            verify(appUserMapper).patchEntityFromDto(updateDto, testUser);
            verify(appUserRepository).save(testUser);
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when new email already exists")
        void shouldThrowExceptionWhenNewEmailAlreadyExists() {
            // Arrange
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserRepository.existsByEmail("updated@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> appUserService.updateAppUser(1L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email already exists");

            verify(appUserRepository).findById(1L);
            verify(appUserRepository).existsByEmail("updated@example.com");
            verify(appUserRepository, never()).save(any(AppUser.class));
        }

        @Test
        @DisplayName("Should update only email when password is null")
        void shouldUpdateOnlyEmailWhenPasswordIsNull() {
            // Arrange: Only update email, not password
            updateDto.setPassword(null);
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserRepository.existsByEmail("updated@example.com")).thenReturn(false);
            when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            AppUserResponseDto result = appUserService.updateAppUser(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            verify(appUserMapper).patchEntityFromDto(updateDto, testUser);
            verify(appUserRepository).save(testUser);
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should update only password when email is null")
        void shouldUpdateOnlyPasswordWhenEmailIsNull() {
            // Arrange: Only update password, not email
            updateDto.setEmail(null);
            when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);
            when(appUserMapper.toResponseDto(testUser)).thenReturn(testUserResponseDto);

            // Act
            AppUserResponseDto result = appUserService.updateAppUser(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            verify(appUserRepository).findById(1L);
            verify(appUserRepository, never()).existsByEmail(anyString());
            verify(appUserMapper).patchEntityFromDto(updateDto, testUser);
            verify(appUserRepository).save(testUser);
            verify(appUserMapper).toResponseDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when ID is null")
        void shouldThrowExceptionWhenIdIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.updateAppUser(null, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id cannot be null");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when update DTO is null")
        void shouldThrowExceptionWhenUpdateDtoIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.updateAppUser(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appUserUpdateDto cannot be null");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent user")
        void shouldThrowExceptionWhenUpdatingNonExistentUser() {
            // Arrange
            when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> appUserService.updateAppUser(999L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("appUser with id '999' not found");

            verify(appUserRepository).findById(999L);
            verify(appUserRepository, never()).save(any(AppUser.class));
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Nested
    @DisplayName("Delete AppUser Tests")
    class DeleteAppUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Arrange
            when(appUserRepository.existsById(1L)).thenReturn(true);
            doNothing().when(appUserRepository).deleteById(1L);

            // Act
            appUserService.deleteById(1L);

            // Assert
            verify(appUserRepository).existsById(1L);
            verify(appUserRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting with null ID")
        void shouldThrowExceptionWhenDeletingWithNullId() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.deleteById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id cannot be null");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            // Arrange
            when(appUserRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> appUserService.deleteById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("appUser with id '999' not found");

            verify(appUserRepository).existsById(999L);
            verify(appUserRepository, never()).deleteById(anyLong());
        }
    }

    // ==================== EXISTENCE CHECKS ====================

    @Nested
    @DisplayName("Existence Check Tests")
    class ExistenceCheckTests {

        @Test
        @DisplayName("Should return true when username exists")
        void shouldReturnTrueWhenUsernameExists() {
            // Arrange
            when(appUserRepository.existsByUsername("testuser")).thenReturn(true);

            // Act
            boolean result = appUserService.existsByUsername("testuser");

            // Assert
            assertThat(result).isTrue();
            verify(appUserRepository).existsByUsername("testuser");
        }

        @Test
        @DisplayName("Should return false when username does not exist")
        void shouldReturnFalseWhenUsernameDoesNotExist() {
            // Arrange
            when(appUserRepository.existsByUsername("nonexistent")).thenReturn(false);

            // Act
            boolean result = appUserService.existsByUsername("nonexistent");

            // Assert
            assertThat(result).isFalse();
            verify(appUserRepository).existsByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should throw exception when checking existence with empty username")
        void shouldThrowExceptionWhenCheckingExistenceWithEmptyUsername() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.existsByUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username cannot be empty");

            verifyNoInteractions(appUserRepository);
        }

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            // Arrange
            when(appUserRepository.existsByEmail("test@example.com")).thenReturn(true);

            // Act
            boolean result = appUserService.existsByEmail("test@example.com");

            // Assert
            assertThat(result).isTrue();
            verify(appUserRepository).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            // Arrange
            when(appUserRepository.existsByEmail("notfound@example.com")).thenReturn(false);

            // Act
            boolean result = appUserService.existsByEmail("notfound@example.com");

            // Assert
            assertThat(result).isFalse();
            verify(appUserRepository).existsByEmail("notfound@example.com");
        }

        @Test
        @DisplayName("Should throw exception when checking existence with empty email")
        void shouldThrowExceptionWhenCheckingExistenceWithEmptyEmail() {
            // Act & Assert
            assertThatThrownBy(() -> appUserService.existsByEmail(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email cannot be empty");

            verifyNoInteractions(appUserRepository);
        }
    }
}
