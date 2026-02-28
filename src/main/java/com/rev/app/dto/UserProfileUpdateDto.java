package com.rev.app.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDto {
    private String name;
    private String email;
    private String phoneNumber;
}
