package com.tutorial.taskmanager.dto.appuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new AppUser.
 *
 * <p>Used as input to the service layer. Conversion to entity is handled
 * by {@link com.tutorial.taskmanager.mapper.AppUserMapper}.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>username - Unique username (validated in service layer)</li>
 *   <li>email - Unique email address (validated in service layer)</li>
 *   <li>password - Plain text password (will be encoded before persistence)</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;
}
