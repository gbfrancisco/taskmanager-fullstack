package com.tutorial.taskmanager.dto.auth;

import com.tutorial.taskmanager.dto.appuser.AppUserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private AppUserResponseDto user;
}
