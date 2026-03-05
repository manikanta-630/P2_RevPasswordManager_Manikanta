package com.rev.app.mapper;

import com.rev.app.dto.UserProfileUpdateDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserRegistrationDto toRegistrationDto(User user) {
        if (user == null) {
            return null;
        }
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        // Note: Passwords and security questions aren't usually mapped from entity back to registration DTO
        return dto;
    }

    public UserProfileUpdateDto toProfileDto(User user) {
        if (user == null) {
            return null;
        }
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        return dto;
    }

    public void updateEntityFromProfileDto(UserProfileUpdateDto dto, User user) {
        if (dto == null || user == null) {
            return;
        }
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
    }
}
