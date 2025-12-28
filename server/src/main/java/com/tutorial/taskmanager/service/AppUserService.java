package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.appuser.AppUserUpdateDto;
import com.tutorial.taskmanager.exception.ResourceNotFoundException;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.repository.AppUserRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(
        AppUserRepository appUserRepository,
        AppUserMapper appUserMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUserResponseDto createAppUser(AppUserCreateDto appUserCreateDto) {
        AppUser appUser = createAppUserEntity(appUserCreateDto);
        return appUserMapper.toResponseDto(appUser);
    }

    public AppUser createAppUserEntity(AppUserCreateDto appUserCreateDto) {
        if (appUserCreateDto == null) {
            throw new IllegalArgumentException("appUserCreateDto cannot be null");
        }

        String username = appUserCreateDto.getUsername();
        String email = appUserCreateDto.getEmail();
        String password = appUserCreateDto.getPassword();

        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }

        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }

        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already exists");
        }

        AppUser appUser = appUserMapper.toEntity(appUserCreateDto);
        appUser.setPassword(passwordEncoder.encode(password));
        return appUserRepository.save(appUser);
    }

    @Transactional(readOnly = true)
    public Optional<AppUserResponseDto> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        return appUserRepository.findById(id)
            .map(appUserMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public AppUserResponseDto getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        AppUser appUser = appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));
        return appUserMapper.toResponseDto(appUser);
    }

    @Transactional(readOnly = true)
    AppUser getEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        return appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("appUser", id));
    }

    @Transactional(readOnly = true)
    public Optional<AppUserResponseDto> findByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.findByUsername(username)
            .map(appUserMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findEntityByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public AppUserResponseDto getByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        AppUser appUser = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("appUser with username '" + username + "' not found"));
        return appUserMapper.toResponseDto(appUser);
    }

    @Transactional(readOnly = true)
    public Optional<AppUserResponseDto> findByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        return appUserRepository.findByEmail(email)
            .map(appUserMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<AppUserResponseDto> findAll() {
        List<AppUser> appUsers = appUserRepository.findAll();
        return appUserMapper.toResponseDtoList(appUsers);
    }

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

        String password = appUserUpdateDto.getPassword();

        appUserMapper.patchEntityFromDto(appUserUpdateDto, existingAppUser);

        if (password != null) {
            existingAppUser.setPassword(passwordEncoder.encode(password));
        }

        AppUser savedAppUser = appUserRepository.save(existingAppUser);
        return appUserMapper.toResponseDto(savedAppUser);
    }

    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (!appUserRepository.existsById(id)) {
            throw new ResourceNotFoundException("appUser", id);
        }

        appUserRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return appUserRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        return appUserRepository.existsByEmail(email);
    }
}
