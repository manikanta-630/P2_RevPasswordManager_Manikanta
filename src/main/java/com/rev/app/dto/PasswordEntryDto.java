package com.rev.app.dto;

import lombok.Data;

@Data
public class PasswordEntryDto {
    private String accountName;
    private String websiteUrl;
    private String usernameEmail;
    private String password;
    private String category;
    private String notes;
    private String masterPassword;
}
