package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    // TODO: Create AppUser
    // - Method: createAppUser(AppUser appUser)
    // - Validate username and email are unique (throw exception if exists)
    // - Save and return the created user

    // TODO: Find AppUser by ID
    // - Method: findById(Long id)
    // - Return Optional<AppUser>
    // - Use repository.findById()

    // TODO: Find AppUser by ID (with exception if not found)
    // - Method: getById(Long id)
    // - Throw custom exception if user not found
    // - This is useful for other services that need a user to exist

    // TODO: Find AppUser by username
    // - Method: findByUsername(String username)
    // - Return Optional<AppUser>
    // - Use repository.findByUsername()

    // TODO: Get AppUser by username (with exception if not found)
    // - Method: getByUsername(String username)
    // - Throw custom exception if user not found

    // TODO: Find AppUser by email
    // - Method: findByEmail(String email)
    // - Return Optional<AppUser>

    // TODO: Get all AppUsers
    // - Method: findAll()
    // - Return List<AppUser>

    // TODO: Update AppUser
    // - Method: updateAppUser(Long id, AppUser updatedUser)
    // - Find existing user by ID (throw exception if not found)
    // - Update only allowed fields (email, password - NOT username as it's immutable)
    // - Check email uniqueness if changed
    // - Save and return updated user

    // TODO: Delete AppUser by ID
    // - Method: deleteById(Long id)
    // - Check if user exists first (throw exception if not found)
    // - Consider cascade: deleting user will delete their tasks and projects (due to cascade)
    // - Delete the user

    // TODO: Check if username exists
    // - Method: existsByUsername(String username)
    // - Return boolean
    // - Useful for validation

    // TODO: Check if email exists
    // - Method: existsByEmail(String email)
    // - Return boolean
    // - Useful for validation
}
