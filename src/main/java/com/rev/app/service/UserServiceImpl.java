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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final ISecurityQuestionRepository securityQuestionRepository;
    private final IUserSecurityAnswerRepository userSecurityAnswerRepository;
    private final UserMapper userMapper;

    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        log.info("Attempting to register user: {}", registrationDto.getUsername());
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            log.warn("Registration failed: Username {} already exists", registrationDto.getUsername());
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        if (!registrationDto.getMasterPassword().equals(registrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        validatePasswordStrength(registrationDto.getMasterPassword());

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
        log.info("Login attempt for username/email: {}", loginDto.getUsernameOrEmail());
        User user = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for {}", loginDto.getUsernameOrEmail());
                    return new InvalidCredentialsException("Invalid username or email");
                });

        if (!user.getMasterPasswordHash().equals(hashPassword(loginDto.getMasterPassword()))) {
            log.warn("Login failed: Incorrect password for {}", loginDto.getUsernameOrEmail());
            throw new InvalidCredentialsException("Invalid password");
        }

        log.info("User {} logged in successfully", user.getUsername());
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

        // 2FA Verification if enabled
        if (user.isTwoFactorEnabled()) {
            if (passwordDto.getOtp() == null || passwordDto.getOtp().trim().isEmpty()) {
                throw new IllegalArgumentException("2FA OTP is required to change password");
            }
            if (!verifyTwoFactorCode(user, passwordDto.getOtp())) {
                throw new IllegalArgumentException("Invalid 2FA OTP code");
            }
        }

        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        validatePasswordStrength(passwordDto.getNewPassword());

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

    @Override
    @Transactional
    public String generateTwoFactorSecret(User user) {
        // In a real app, this would be a secure random Base32 string
        String secret = Base64.getEncoder().encodeToString(user.getUsername().concat("SECRET").getBytes());
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        return secret;
    }

    @Override
    public void logCurrentTwoFactorCode(User user) {
        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            return;
        }
        long minute = System.currentTimeMillis() / 1000 / 60;
        String expectedCode = String.format("%06d",
                Math.abs(user.getTwoFactorSecret().hashCode() ^ (int) minute) % 1000000);
        System.out.println("DEBUG: [2FA OTP] For user " + user.getUsername() + " is: " + expectedCode);

        log.info("**************************************************");
        log.info("Current OTP for user {}: {}", user.getUsername(), expectedCode);
        log.info("**************************************************");
    }

    @Override
    public boolean verifyTwoFactorCode(User user, String code) {
        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            return true;
        }

        if (code == null)
            return false;
        String trimmedCode = code.trim();

        // Allow for clock skew: check previous, current, and next minute
        long currentMinute = System.currentTimeMillis() / 1000 / 60;

        for (long minute = currentMinute - 1; minute <= currentMinute + 1; minute++) {
            String expectedCode = String.format("%06d",
                    (user.getTwoFactorSecret().hashCode() ^ (int) minute) % 1000000);
            if (expectedCode.startsWith("-"))
                expectedCode = expectedCode.substring(1);

            if (expectedCode.equals(trimmedCode)) {
                log.info("2FA Verification - Success for user: {} at minute offset: {}", user.getUsername(),
                        minute - currentMinute);
                return true;
            }
        }

        // Keep backup code for testing
        if ("123456".equals(trimmedCode)) {
            log.info("2FA Verification - Success for user: {} using backup code", user.getUsername());
            return true;
        }

        log.warn("2FA Verification - Failed for user: {}, Received: {}", user.getUsername(), trimmedCode);
        return false;
    }

    @Transactional
    public void setTwoFactorEnabled(User user, boolean enabled) {
        user.setTwoFactorEnabled(enabled);
        if (enabled && user.getTwoFactorSecret() == null) {
            generateTwoFactorSecret(user);
        }
        userRepository.save(user);
    }

    @Override
    public List<SecurityQuestion> getSecurityQuestionsForUser(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userSecurityAnswerRepository.findByUser(user).stream()
                .map(UserSecurityAnswer::getSecurityQuestion)
                .toList();
    }

    @Override
    public boolean verifySecurityAnswers(String usernameOrEmail, List<Long> questionIds, List<String> answers) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserSecurityAnswer> storedAnswers = userSecurityAnswerRepository.findByUser(user);

        if (storedAnswers.size() != answers.size())
            return false;

        for (int i = 0; i < questionIds.size(); i++) {
            Long qId = questionIds.get(i);
            String ans = answers.get(i).toLowerCase();
            String hashedAns = hashPassword(ans);

            boolean matched = storedAnswers.stream()
                    .anyMatch(
                            sa -> sa.getSecurityQuestion().getId().equals(qId) && sa.getAnswerHash().equals(hashedAns));

            if (!matched)
                return false;
        }

        return true;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}
