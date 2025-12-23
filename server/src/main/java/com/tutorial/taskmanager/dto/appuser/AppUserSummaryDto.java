package com.tutorial.taskmanager.dto.appuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for embedding user info in other responses.
 *
 * <p>Used when we need to include user information in Task or Project
 * responses without returning the full user object.
 *
 * <p><strong>Why a separate DTO?</strong>
 * <ul>
 *   <li>Lighter payload - only essential identification fields</li>
 *   <li>Avoids circular references when embedding</li>
 *   <li>Single responsibility - different DTOs for different purposes</li>
 * </ul>
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>id - User's unique identifier</li>
 *   <li>username - User's display name</li>
 * </ul>
 *
 * @see AppUserResponseDto for full user responses
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserSummaryDto {
    private Long id;
    private String username;
}
