package com.tutorial.taskmanager.service;

import com.tutorial.taskmanager.config.JwtProperties;
import com.tutorial.taskmanager.dto.appuser.AppUserCreateDto;
import com.tutorial.taskmanager.dto.auth.AuthResponseDto;
import com.tutorial.taskmanager.dto.auth.LoginRequestDto;
import com.tutorial.taskmanager.dto.auth.RegisterRequestDto;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.model.AppUser;
import com.tutorial.taskmanager.repository.AppUserRepository;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        AppUserService appUserService,
        AppUserRepository appUserRepository,
        AppUserMapper appUserMapper,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        JwtProperties jwtProperties,
        AuthenticationManager authenticationManager
    ) {
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user.
     *
     * @param request Registration details
     * @return Auth response with token and user info
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        // Validate uniqueness
        if (appUserService.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (appUserService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // Create user with encoded password
        AppUserCreateDto user = AppUserCreateDto.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(request.getPassword())
            .build();

        AppUser savedUser = appUserService.createAppUserEntity(user);

        // Generate token
        AppUserDetails userDetails = new AppUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, savedUser);
    }

    /**
     * Authenticate user and return token.
     *
     * Accepts either username or email as the identifier.
     * Uses @ symbol to distinguish: contains @ = email, otherwise = username.
     *
     * @param request Login credentials (usernameOrEmail + password)
     * @return Auth response with token and user info
     * @throws BadCredentialsException if credentials are invalid
     */
    public AuthResponseDto login(LoginRequestDto request) {
        String identifier = request.getUsernameOrEmail();

        // Authenticate (throws BadCredentialsException if invalid)
        // The AuthenticationManager uses AppUserDetailsService which handles both cases
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
            identifier,
            request.getPassword()
        ));

        // Load user - need to check both fields since auth already validated
        AppUser user;
        if (identifier.contains("@")) {
            user = appUserRepository.findByEmail(identifier)
                .orElseThrow(); // Won't throw - auth already validated
        } else {
            user = appUserService.findEntityByUsername(identifier)
                .orElseThrow(); // Won't throw - auth already validated
        }

        AppUserDetails userDetails = new AppUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    private AuthResponseDto buildAuthResponse(String token, AppUser user) {
        return AuthResponseDto.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresIn(jwtProperties.getExpirationMs() / 1000)  // Convert to seconds
            .user(appUserMapper.toResponseDto(user))
            .build();
    }
}
