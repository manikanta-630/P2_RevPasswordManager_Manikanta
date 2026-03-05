package com.rev.app.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserRegistrationDto {
    private String username;
    private String email;
    private String masterPassword;
    private String confirmPassword;
    
    // Simulating selecting 3 security questions by ID and providing answers
    private List<Long> securityQuestionIds;
    private List<String> securityAnswers;
}
