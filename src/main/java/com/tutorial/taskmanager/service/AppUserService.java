package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.repository.AppUserRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    // Create AppUser
    // - Method: createAppUser(AppUserCreateDto appUserCreateDto)
    // - Validate username and email are unique (throw exception if exists)
    // - Save and return the created user
    public AppUser createAppUser(AppUserCreateDto appUserCreateDto) {
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

        return appUserRepository.save(appUserCreateDto.convert());
    }

    // Find AppUser by ID
    // - Method: findById(Long id)
    // - Return Optional<AppUser>
    // - Use repository.findById()
    public Optional<AppUser> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        return appUserRepository.findById(id);
    }

    // Find AppUser by ID (with exception if not found)
    // - Method: getById(Long id)
    // - Throw custom exception if user not found
    // - This is useful for other services that need a user to exist
    public AppUser getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        return appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));
    }

    // Find AppUser by username
    // - Method: findByUsername(String username)
    // - Return Optional<AppUser>
    // - Use repository.findByUsername()
    public Optional<AppUser> findByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.findByUsername(username);
    }

    // Get AppUser by username (with exception if not found)
    // - Method: getByUsername(String username)
    // - Throw custom exception if user not found
    public AppUser getByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("appUser with username '" + username + "' not found"));
    }

    // Find AppUser by email
    // - Method: findByEmail(String email)
    // - Return Optional<AppUser>
    public Optional<AppUser> findByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        return appUserRepository.findByEmail(email);
    }

    // Get all AppUsers
    // - Method: findAll()
    // - Return List<AppUser>
    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    // Update AppUser
    // - Method: updateAppUser(Long id, AppUser updatedUser)
    // - Find existing user by ID (throw exception if not found)
    // - Update only allowed fields (email, password - NOT username as it's immutable)
    // - Check email uniqueness if changed
    // - Save and return updated user
    public AppUser updateAppUser(Long id, AppUserUpdateDto appUserUpdateDto) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (appUserUpdateDto == null) {
            throw new IllegalArgumentException("appUserUpdateDto cannot be null");
        }

        AppUser existingAppUser = getById(id);

        String updatedEmail = appUserUpdateDto.getEmail();
        if (updatedEmail != null) {
            if (!Strings.CI.equals(updatedEmail, existingAppUser.getEmail()) && existsByEmail(updatedEmail)) {
                throw new IllegalArgumentException("email already exists");
            }
            existingAppUser.setEmail(updatedEmail);
        }

        if (appUserUpdateDto.getPassword() != null) {
            existingAppUser.setPassword(appUserUpdateDto.getPassword());
        }

        return appUserRepository.save(existingAppUser);
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
