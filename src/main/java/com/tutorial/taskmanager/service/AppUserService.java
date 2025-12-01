package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;

    public AppUserService(AppUserRepository appUserRepository, AppUserMapper appUserMapper) {
        this.appUserRepository = appUserRepository;
        this.appUserMapper = appUserMapper;
    }

    // Create AppUser
    // - Method: createAppUser(AppUserCreateDto appUserCreateDto)
    // - Validate username and email are unique (throw exception if exists)
    // - Save and return the created user as ResponseDto
    public AppUserResponseDto createAppUser(AppUserCreateDto appUserCreateDto) {
        if (appUserCreateDto == null) {
            throw new IllegalArgumentException("appUserCreateDto cannot be null");
        }

        String username = appUserCreateDto.getUsername();
        String email = appUserCreateDto.getEmail();

        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }

        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already exists");
        }

        AppUser appUser = appUserMapper.toEntity(appUserCreateDto);
        appUser = appUserRepository.save(appUser);
        return appUserMapper.toResponseDto(appUser);
    }

    // Find AppUser by ID
    // - Method: findById(Long id)
    // - Return Optional<AppUserResponseDto>
    // - Use repository.findById() and map to DTO
    public Optional<AppUserResponseDto> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        return appUserRepository.findById(id)
            .map(appUserMapper::toResponseDto);
    }

    // Find AppUser by ID (with exception if not found)
    // - Method: getById(Long id)
    // - Throw custom exception if user not found
    // - This is useful for controllers and returns DTO
    public AppUserResponseDto getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        AppUser appUser = appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));
        return appUserMapper.toResponseDto(appUser);
    }

    // Internal helper method to get entity (for use by other services)
    // - This returns the entity, not a DTO
    // - Other services (like TaskService) need the actual entity to set relationships
    AppUser getEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        return appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));
    }

    // Find AppUser by username
    // - Method: findByUsername(String username)
    // - Return Optional<AppUserResponseDto>
    // - Use repository.findByUsername() and map to DTO
    public Optional<AppUserResponseDto> findByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.findByUsername(username)
            .map(appUserMapper::toResponseDto);
    }

    // Get AppUser by username (with exception if not found)
    // - Method: getByUsername(String username)
    // - Throw custom exception if user not found
    public AppUserResponseDto getByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        AppUser appUser = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("appUser with username '" + username + "' not found"));
        return appUserMapper.toResponseDto(appUser);
    }

    // Find AppUser by email
    // - Method: findByEmail(String email)
    // - Return Optional<AppUserResponseDto>
    public Optional<AppUserResponseDto> findByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        return appUserRepository.findByEmail(email)
            .map(appUserMapper::toResponseDto);
    }

    // Get all AppUsers
    // - Method: findAll()
    // - Return List<AppUserResponseDto>
    public List<AppUserResponseDto> findAll() {
        List<AppUser> appUsers = appUserRepository.findAll();
        return appUserMapper.toResponseDtoList(appUsers);
    }

    // Update AppUser
    // - Method: updateAppUser(Long id, AppUserUpdateDto appUserUpdateDto)
    // - Find existing user by ID (throw exception if not found)
    // - Update only allowed fields (email, password - NOT username as it's immutable)
    // - Check email uniqueness if changed
    // - Save and return updated user as ResponseDto
    public AppUserResponseDto updateAppUser(Long id, AppUserUpdateDto appUserUpdateDto) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (appUserUpdateDto == null) {
            throw new IllegalArgumentException("appUserUpdateDto cannot be null");
        }

        AppUser existingAppUser = getEntityById(id);

        String updatedEmail = appUserUpdateDto.getEmail();
        if (updatedEmail != null) {
            if (!Strings.CI.equals(updatedEmail, existingAppUser.getEmail()) && existsByEmail(updatedEmail)) {
                throw new IllegalArgumentException("email already exists");
            }
        }

        // Use MapStruct to update the entity from the DTO
        appUserMapper.updateEntityFromDto(appUserUpdateDto, existingAppUser);

        AppUser savedAppUser = appUserRepository.save(existingAppUser);
        return appUserMapper.toResponseDto(savedAppUser);
    }

    // Delete AppUser by ID
    // - Method: deleteById(Long id)
    // - Check if user exists first (throw exception if not found)
    // - Consider cascade: deleting user will delete their tasks and projects (due to cascade)
    // - Delete the user
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (!appUserRepository.existsById(id)) {
            throw new ResourceNotFoundException("appUser", id);
        }

        appUserRepository.deleteById(id);
    }

    // Check if username exists
    // - Method: existsByUsername(String username)
    // - Return boolean
    // - Useful for validation
    public boolean existsByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.existsByUsername(username);
    }

    // Check if email exists
    // - Method: existsByEmail(String email)
    // - Return boolean
    // - Useful for validation
    public boolean existsByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        return appUserRepository.existsByEmail(email);
    }
}
