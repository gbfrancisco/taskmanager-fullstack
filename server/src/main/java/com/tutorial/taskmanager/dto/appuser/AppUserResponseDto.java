package com.tutorial.taskmanager.dto.appuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AppUser responses.
 *
 * <p>Used as output from the service layer. Conversion from entity is handled
 * by {@link com.tutorial.taskmanager.mapper.AppUserMapper}.
 *
 * <p><strong>Security Note:</strong> Password is NEVER included in response DTOs.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - User's unique identifier</li>
 *   <li>username - User's username</li>
 *   <li>email - User's email address</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserResponseDto {
    private Long id;
    private String username;
    private String email;
}
