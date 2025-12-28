package com.tutorial.taskmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Strings;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService appUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService appUserDetailsService) {
        this.jwtService = jwtService;
        this.appUserDetailsService = appUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. Check if it's a Bearer token
        if (!Strings.CI.startsWith(authHeader, "Bearer ")) {
            // No token - continue filter chain (might be public endpoint)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        // 4. Extract username from token
        final String username = jwtService.extractUsername(jwt);

        // 5. If username exists and not already authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from the database
            UserDetails userDetails = appUserDetailsService.loadUserByUsername(username);

            // 7. Validate token
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 8. Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,   // credentials (password) - not needed after auth
                    userDetails.getAuthorities()
                );

                // 9. Add request details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 10. Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 11. Continue filter chain
        filterChain.doFilter(request, response);
    }
}
