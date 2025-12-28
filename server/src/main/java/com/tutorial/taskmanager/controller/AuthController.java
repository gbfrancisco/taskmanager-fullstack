package com.tutorial.taskmanager.controller;

import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import com.tutorial.taskmanager.dto.auth.AuthResponseDto;
import com.tutorial.taskmanager.dto.auth.LoginRequestDto;
import com.tutorial.taskmanager.dto.auth.RegisterRequestDto;
import com.tutorial.taskmanager.mapper.AppUserMapper;
import com.tutorial.taskmanager.security.AppUserDetails;
import com.tutorial.taskmanager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/auth/register - Create new account</li>
 *   <li>POST /api/auth/login - Authenticate and get token</li>
 *   <li>GET /api/auth/me - Get current user info (requires auth)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppUserMapper appUserMapper;

    public AuthController(AuthService authService, AppUserMapper appUserMapper) {
        this.authService = authService;
        this.appUserMapper = appUserMapper;
    }

    /**
     * Register a new user account.
     *
     * @param request Registration details
     * @return 201 Created with token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and return token.
     *
     * @param request Login credentials
     * @return 200 OK with token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Get current authenticated user info.
     *
     * @param userDetails Injected by Spring Security
     * @return 200 OK with user info
     */
    @GetMapping("/me")
    public ResponseEntity<AppUserResponseDto> getCurrentUser(@AuthenticationPrincipal AppUserDetails userDetails) {
        AppUserResponseDto response = appUserMapper.toResponseDto(userDetails.getAppUser());
        return ResponseEntity.ok(response);
    }

}
