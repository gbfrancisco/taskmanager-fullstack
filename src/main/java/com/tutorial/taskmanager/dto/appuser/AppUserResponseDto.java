package com.tutorial.taskmanager.dto.appuser;

import com.tutorial.taskmanager.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppUserResponseDto {
    private Long id;
    private String username;
    private String email;

    public AppUserResponseDto(AppUser appUser) {
        this.id = appUser.getId();
        this.username = appUser.getUsername();
        this.email = appUser.getEmail();
    }
}
