package com.rev.app.service;

import com.rev.app.dto.LoginDto;
import com.rev.app.dto.PasswordChangeDto;
import com.rev.app.dto.UserProfileUpdateDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.User;

public interface IUserService {
    User registerUser(UserRegistrationDto registrationDto);
    User loginUser(LoginDto loginDto);
    User updateProfile(User user, UserProfileUpdateDto profileDto);
    void changeMasterPassword(User user, PasswordChangeDto passwordDto);
    void resetMasterPassword(String email, String newPassword);
    String hashPassword(String password);
}
