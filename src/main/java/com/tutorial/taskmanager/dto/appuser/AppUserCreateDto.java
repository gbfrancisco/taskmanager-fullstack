package com.tutorial.taskmanager.dto.appuser;

import com.tutorial.taskmanager.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppUserCreateDto {
    private String username;
    private String email;
    private String password;

    public AppUser convert() {
        return AppUser.builder()
            .username(username)
            .email(email)
            .password(password)
            .build();
    }
}
