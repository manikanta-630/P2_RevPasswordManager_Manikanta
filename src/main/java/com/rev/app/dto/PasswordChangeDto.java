package com.rev.app.dto;

import lombok.Data;

@Data
public class PasswordChangeDto {
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}
