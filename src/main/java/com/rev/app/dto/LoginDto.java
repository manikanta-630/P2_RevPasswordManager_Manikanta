package com.rev.app.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String usernameOrEmail;
    private String masterPassword;
}
