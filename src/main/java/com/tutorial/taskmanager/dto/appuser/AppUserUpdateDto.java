package com.tutorial.taskmanager.dto.appuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing AppUser.
 *
 * <p>Used as input to the service layer. Conversion to entity is handled
 * by {@link com.tutorial.taskmanager.mapper.AppUserMapper#updateEntityFromDto}.
 *
 * <p><strong>Important:</strong> Username cannot be updated (immutable business rule).
 * Only email and password can be changed.
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>email - New email address (optional, validated for uniqueness in service layer)</li>
 *   <li>password - New password (optional, will be encoded before persistence)</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserUpdateDto {
    private String email;
    private String password;
}
