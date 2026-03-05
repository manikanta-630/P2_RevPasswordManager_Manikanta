package com.rev.app.service;

import com.rev.app.dto.LoginDto;
import com.rev.app.dto.PasswordChangeDto;
import com.rev.app.dto.UserProfileUpdateDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.SecurityQuestion;
import com.rev.app.entity.User;

import java.util.List;

public interface IUserService {
    User registerUser(UserRegistrationDto registrationDto);

    User loginUser(LoginDto loginDto);

    User updateProfile(User user, UserProfileUpdateDto profileDto);

    void changeMasterPassword(User user, PasswordChangeDto passwordDto);

    void resetMasterPassword(String email, String newPassword);

    String hashPassword(String password);

    // 2FA methods
    String generateTwoFactorSecret(User user);

    void logCurrentTwoFactorCode(User user);

    boolean verifyTwoFactorCode(User user, String code);

    void setTwoFactorEnabled(User user, boolean enabled);

    // Security Question methods
    List<SecurityQuestion> getSecurityQuestionsForUser(String usernameOrEmail);

    boolean verifySecurityAnswers(String usernameOrEmail, List<Long> questionIds, List<String> answers);
}
