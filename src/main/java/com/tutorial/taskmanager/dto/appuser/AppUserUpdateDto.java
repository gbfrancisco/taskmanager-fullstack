package com.tutorial.taskmanager.dto.appuser;

import com.tutorial.taskmanager.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserUpdateDto {
    private String email;
    private String password;

    public AppUserUpdateDto(AppUser appUser) {
        this.email = appUser.getEmail();
        this.password = appUser.getPassword();
    }
}
