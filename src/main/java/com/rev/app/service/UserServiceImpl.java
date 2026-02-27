package com.rev.app.service;

import com.rev.app.dto.LoginDto;
import com.rev.app.dto.PasswordChangeDto;
import com.rev.app.dto.UserProfileUpdateDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.SecurityQuestion;
import com.rev.app.entity.User;
import com.rev.app.entity.UserSecurityAnswer;
import com.rev.app.exception.InvalidCredentialsException;
import com.rev.app.exception.UserAlreadyExistsException;
import com.rev.app.mapper.UserMapper;
import com.rev.app.repository.ISecurityQuestionRepository;
import com.rev.app.repository.IUserRepository;
import com.rev.app.repository.IUserSecurityAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final ISecurityQuestionRepository securityQuestionRepository;
    private final IUserSecurityAnswerRepository userSecurityAnswerRepository;
    private final UserMapper userMapper;

    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        if (!registrationDto.getMasterPassword().equals(registrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (registrationDto.getSecurityQuestionIds() == null || registrationDto.getSecurityQuestionIds().size() < 3) {
            throw new IllegalArgumentException("At least 3 security questions must be answered");
        }

        User user = User.builder()
                .username(registrationDto.getUsername())
                .email(registrationDto.getEmail())
                .masterPasswordHash(hashPassword(registrationDto.getMasterPassword()))
                .twoFactorEnabled(false)
                .build();
        
        user = userRepository.save(user);

        // Save security answers
        for (int i = 0; i < registrationDto.getSecurityQuestionIds().size(); i++) {
            Long questionId = registrationDto.getSecurityQuestionIds().get(i);
            String answer = registrationDto.getSecurityAnswers().get(i);
            
            SecurityQuestion question = securityQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid security question ID"));
                    
            UserSecurityAnswer securityAnswer = UserSecurityAnswer.builder()
                    .user(user)
                    .securityQuestion(question)
                    .answerHash(hashPassword(answer.toLowerCase())) // hash answer for security
                    .build();
            userSecurityAnswerRepository.save(securityAnswer);
        }

        return user;
    }

    public User loginUser(LoginDto loginDto) {
        User user = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or email"));

        if (!user.getMasterPasswordHash().equals(hashPassword(loginDto.getMasterPassword()))) {
            throw new InvalidCredentialsException("Invalid password");
        }

        return user;
    }

    @Transactional
    public User updateProfile(User user, UserProfileUpdateDto profileDto) {
        // check if email is taken by someone else
        if (!user.getEmail().equals(profileDto.getEmail()) && userRepository.existsByEmail(profileDto.getEmail())) {
            throw new IllegalArgumentException("Email is already taken");
        }
        
        userMapper.updateEntityFromProfileDto(profileDto, user);
        return userRepository.save(user);
    }

    @Transactional
    public void changeMasterPassword(User user, PasswordChangeDto passwordDto) {
        if (!user.getMasterPasswordHash().equals(hashPassword(passwordDto.getCurrentPassword()))) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        user.setMasterPasswordHash(hashPassword(passwordDto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void resetMasterPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email not found"));
        user.setMasterPasswordHash(hashPassword(newPassword));
        userRepository.save(user);
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
