package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/**
 * Unit tests for AppUserService
 *
 * Testing Strategy:
 * - Use Mockito to mock the AppUserRepository dependency
 * - Test business logic and validation in isolation
 * - Verify interactions with the repository
 * - Test both success and failure scenarios
 *
 * Key Annotations:
 * - @ExtendWith(MockitoExtension.class): Enables Mockito in JUnit 5
 * - @Mock: Creates a mock instance of AppUserRepository
 * - @InjectMocks: Creates AppUserService and injects the mocked repository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppUserService Unit Tests")
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserService appUserService;

    private AppUser testUser;
    private AppUserCreateDto createDto;
    private AppUserUpdateDto updateDto;

    /**
     * Setup method that runs before each test
     * Creates test data that can be reused across tests
     */
    @BeforeEach
    void setUp() {
        // Create a test user entity
        testUser = AppUser.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();
        // Simulate that this user has been saved (has an ID)
        testUser.setId(1L);

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

    // ========================================
    // createAppUser() Tests
    // ========================================

    @Test
    @DisplayName("createAppUser - should create user successfully")
    void createAppUser_Success() {
        // Arrange: Setup mock behavior
        when(appUserRepository.existsByUsername(anyString())).thenReturn(false);
        when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);

        // Act: Call the method under test
        AppUser result = appUserService.createAppUser(createDto);

        // Assert: Verify the result and interactions
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
        verify(appUserRepository).existsByUsername("newuser");
        verify(appUserRepository).existsByEmail("new@example.com");
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    @DisplayName("createAppUser - should throw exception when DTO is null")
    void createAppUser_NullDto_ThrowsException() {
        // Act & Assert: Verify exception is thrown
        assertThatThrownBy(() -> appUserService.createAppUser(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("appUserCreateDto cannot be null");

        // Verify no repository interactions occurred
        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("createAppUser - should throw exception when username is empty")
    void createAppUser_EmptyUsername_ThrowsException() {
        // Arrange: Create DTO with empty username
        AppUserCreateDto emptyUsernameDto = new AppUserCreateDto("", "test@example.com", "password");

        // Act & Assert
        assertThatThrownBy(() -> appUserService.createAppUser(emptyUsernameDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("username cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("createAppUser - should throw exception when username is null")
    void createAppUser_NullUsername_ThrowsException() {
        // Arrange
        AppUserCreateDto nullUsernameDto = new AppUserCreateDto(null, "test@example.com", "password");

        // Act & Assert
        assertThatThrownBy(() -> appUserService.createAppUser(nullUsernameDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("username cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("createAppUser - should throw exception when email is empty")
    void createAppUser_EmptyEmail_ThrowsException() {
        // Arrange
        AppUserCreateDto emptyEmailDto = new AppUserCreateDto("testuser", "", "password");

        // Act & Assert
        assertThatThrownBy(() -> appUserService.createAppUser(emptyEmailDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("createAppUser - should throw exception when username already exists")
    void createAppUser_DuplicateUsername_ThrowsException() {
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
    @DisplayName("createAppUser - should throw exception when email already exists")
    void createAppUser_DuplicateEmail_ThrowsException() {
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

    // ========================================
    // findById() Tests
    // ========================================

    @Test
    @DisplayName("findById - should return user when found")
    void findById_UserExists_ReturnsOptionalWithUser() {
        // Arrange
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<AppUser> result = appUserService.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(appUserRepository).findById(1L);
    }

    @Test
    @DisplayName("findById - should return empty Optional when not found")
    void findById_UserNotFound_ReturnsEmptyOptional() {
        // Arrange
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(appUserRepository).findById(999L);
    }

    @Test
    @DisplayName("findById - should throw exception when id is null")
    void findById_NullId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.findById(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("id cannot be null");

        verifyNoInteractions(appUserRepository);
    }

    // ========================================
    // getById() Tests
    // ========================================

    @Test
    @DisplayName("getById - should return user when found")
    void getById_UserExists_ReturnsUser() {
        // Arrange
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        AppUser result = appUserService.getById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(appUserRepository).findById(1L);
    }

    @Test
    @DisplayName("getById - should throw exception when user not found")
    void getById_UserNotFound_ThrowsException() {
        // Arrange
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appUserService.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("appUser with id '999' not found");

        verify(appUserRepository).findById(999L);
    }

    @Test
    @DisplayName("getById - should throw exception when id is null")
    void getById_NullId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.getById(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("id cannot be null");

        verifyNoInteractions(appUserRepository);
    }

    // ========================================
    // findByUsername() Tests
    // ========================================

    @Test
    @DisplayName("findByUsername - should return user when found")
    void findByUsername_UserExists_ReturnsOptionalWithUser() {
        // Arrange
        when(appUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<AppUser> result = appUserService.findByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(appUserRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("findByUsername - should return empty Optional when not found")
    void findByUsername_UserNotFound_ReturnsEmptyOptional() {
        // Arrange
        when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findByUsername("nonexistent");

        // Assert
        assertThat(result).isEmpty();
        verify(appUserRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("findByUsername - should throw exception when username is empty")
    void findByUsername_EmptyUsername_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.findByUsername(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("username cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    // ========================================
    // getByUsername() Tests
    // ========================================

    @Test
    @DisplayName("getByUsername - should return user when found")
    void getByUsername_UserExists_ReturnsUser() {
        // Arrange
        when(appUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        AppUser result = appUserService.getByUsername("testuser");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(appUserRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("getByUsername - should throw exception when user not found")
    void getByUsername_UserNotFound_ThrowsException() {
        // Arrange
        when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appUserService.getByUsername("nonexistent"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("appUser with username 'nonexistent' not found");

        verify(appUserRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("getByUsername - should throw exception when username is empty")
    void getByUsername_EmptyUsername_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.getByUsername(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("username cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    // ========================================
    // findByEmail() Tests
    // ========================================

    @Test
    @DisplayName("findByEmail - should return user when found")
    void findByEmail_UserExists_ReturnsOptionalWithUser() {
        // Arrange
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<AppUser> result = appUserService.findByEmail("test@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(appUserRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("findByEmail - should return empty Optional when not found")
    void findByEmail_UserNotFound_ReturnsEmptyOptional() {
        // Arrange
        when(appUserRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<AppUser> result = appUserService.findByEmail("notfound@example.com");

        // Assert
        assertThat(result).isEmpty();
        verify(appUserRepository).findByEmail("notfound@example.com");
    }

    @Test
    @DisplayName("findByEmail - should throw exception when email is empty")
    void findByEmail_EmptyEmail_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.findByEmail(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    // ========================================
    // findAll() Tests
    // ========================================

    @Test
    @DisplayName("findAll - should return all users")
    void findAll_ReturnsAllUsers() {
        // Arrange
        AppUser user2 = AppUser.builder()
            .username("user2")
            .email("user2@example.com")
            .password("pass")
            .build();
        user2.setId(2L);

        when(appUserRepository.findAll()).thenReturn(List.of(testUser, user2));

        // Act
        List<AppUser> result = appUserService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AppUser::getUsername)
            .containsExactly("testuser", "user2");
        verify(appUserRepository).findAll();
    }

    @Test
    @DisplayName("findAll - should return empty list when no users exist")
    void findAll_NoUsers_ReturnsEmptyList() {
        // Arrange
        when(appUserRepository.findAll()).thenReturn(List.of());

        // Act
        List<AppUser> result = appUserService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verify(appUserRepository).findAll();
    }

    // ========================================
    // updateAppUser() Tests
    // ========================================

    @Test
    @DisplayName("updateAppUser - should update email and password successfully")
    void updateAppUser_ValidUpdate_ReturnsUpdatedUser() {
        // Arrange
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(appUserRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);

        // Act
        AppUser result = appUserService.updateAppUser(1L, updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(appUserRepository).findById(1L);
        verify(appUserRepository).existsByEmail("updated@example.com");
        verify(appUserRepository).save(testUser);
    }

    @Test
    @DisplayName("updateAppUser - should allow keeping same email (case-insensitive)")
    void updateAppUser_SameEmail_Success() {
        // Arrange: User wants to keep their email (different case)
        updateDto.setEmail("TEST@EXAMPLE.COM"); // Same email, different case
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);

        // Act
        AppUser result = appUserService.updateAppUser(1L, updateDto);

        // Assert: Should not check existence (same email)
        assertThat(result).isNotNull();
        verify(appUserRepository).findById(1L);
        verify(appUserRepository, never()).existsByEmail(anyString());
        verify(appUserRepository).save(testUser);
    }

    @Test
    @DisplayName("updateAppUser - should throw exception when new email already exists")
    void updateAppUser_DuplicateEmail_ThrowsException() {
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
    @DisplayName("updateAppUser - should update only email when password is null")
    void updateAppUser_OnlyEmailUpdate_Success() {
        // Arrange: Only update email, not password
        updateDto.setPassword(null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(appUserRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);

        // Act
        AppUser result = appUserService.updateAppUser(1L, updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(appUserRepository).save(testUser);
    }

    @Test
    @DisplayName("updateAppUser - should update only password when email is null")
    void updateAppUser_OnlyPasswordUpdate_Success() {
        // Arrange: Only update password, not email
        updateDto.setEmail(null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(testUser);

        // Act
        AppUser result = appUserService.updateAppUser(1L, updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(appUserRepository).findById(1L);
        verify(appUserRepository, never()).existsByEmail(anyString());
        verify(appUserRepository).save(testUser);
    }

    @Test
    @DisplayName("updateAppUser - should throw exception when id is null")
    void updateAppUser_NullId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.updateAppUser(null, updateDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("id cannot be null");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("updateAppUser - should throw exception when DTO is null")
    void updateAppUser_NullDto_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.updateAppUser(1L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("appUserUpdateDto cannot be null");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("updateAppUser - should throw exception when user not found")
    void updateAppUser_UserNotFound_ThrowsException() {
        // Arrange
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appUserService.updateAppUser(999L, updateDto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("appUser with id '999' not found");

        verify(appUserRepository).findById(999L);
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    // ========================================
    // deleteById() Tests
    // ========================================

    @Test
    @DisplayName("deleteById - should delete user successfully")
    void deleteById_UserExists_DeletesUser() {
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
    @DisplayName("deleteById - should throw exception when id is null")
    void deleteById_NullId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.deleteById(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("id cannot be null");

        verifyNoInteractions(appUserRepository);
    }

    @Test
    @DisplayName("deleteById - should throw exception when user not found")
    void deleteById_UserNotFound_ThrowsException() {
        // Arrange
        when(appUserRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> appUserService.deleteById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("appUser with id '999' not found");

        verify(appUserRepository).existsById(999L);
        verify(appUserRepository, never()).deleteById(anyLong());
    }

    // ========================================
    // existsByUsername() Tests
    // ========================================

    @Test
    @DisplayName("existsByUsername - should return true when username exists")
    void existsByUsername_UsernameExists_ReturnsTrue() {
        // Arrange
        when(appUserRepository.existsByUsername("testuser")).thenReturn(true);

        // Act
        boolean result = appUserService.existsByUsername("testuser");

        // Assert
        assertThat(result).isTrue();
        verify(appUserRepository).existsByUsername("testuser");
    }

    @Test
    @DisplayName("existsByUsername - should return false when username does not exist")
    void existsByUsername_UsernameDoesNotExist_ReturnsFalse() {
        // Arrange
        when(appUserRepository.existsByUsername("nonexistent")).thenReturn(false);

        // Act
        boolean result = appUserService.existsByUsername("nonexistent");

        // Assert
        assertThat(result).isFalse();
        verify(appUserRepository).existsByUsername("nonexistent");
    }

    @Test
    @DisplayName("existsByUsername - should throw exception when username is empty")
    void existsByUsername_EmptyUsername_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.existsByUsername(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("username cannot be empty");

        verifyNoInteractions(appUserRepository);
    }

    // ========================================
    // existsByEmail() Tests
    // ========================================

    @Test
    @DisplayName("existsByEmail - should return true when email exists")
    void existsByEmail_EmailExists_ReturnsTrue() {
        // Arrange
        when(appUserRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act
        boolean result = appUserService.existsByEmail("test@example.com");

        // Assert
        assertThat(result).isTrue();
        verify(appUserRepository).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("existsByEmail - should return false when email does not exist")
    void existsByEmail_EmailDoesNotExist_ReturnsFalse() {
        // Arrange
        when(appUserRepository.existsByEmail("notfound@example.com")).thenReturn(false);

        // Act
        boolean result = appUserService.existsByEmail("notfound@example.com");

        // Assert
        assertThat(result).isFalse();
        verify(appUserRepository).existsByEmail("notfound@example.com");
    }

    @Test
    @DisplayName("existsByEmail - should throw exception when email is empty")
    void existsByEmail_EmptyEmail_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> appUserService.existsByEmail(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("email cannot be empty");

        verifyNoInteractions(appUserRepository);
    }
}
