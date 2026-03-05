package com.rev.app.rest;

import com.rev.app.dto.LoginDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.SecurityQuestion;
import com.rev.app.entity.User;
import com.rev.app.exception.InvalidCredentialsException;
import com.rev.app.exception.UserAlreadyExistsException;
import com.rev.app.service.IUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final IUserService userService;
    private final com.rev.app.repository.IOtpVerificationRepository otpRepository;
    private final com.rev.app.repository.IUserRepository userRepository;
    private final com.rev.app.repository.IUserSecurityAnswerRepository userSecurityAnswerRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto registrationDto) {
        try {
            User user = userService.registerUser(registrationDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserAlreadyExistsException | IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginDto loginDto, HttpSession session) {
        try {
            User user = userService.loginUser(loginDto);

            if (user.isTwoFactorEnabled()) {
                session.setAttribute("tempUser", user);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "2FA_REQUIRED");
                response.put("username", user.getUsername());
                return ResponseEntity.ok(response);
            }

            session.setAttribute("loggedInUser", user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("sessionId", session.getId());
            return ResponseEntity.ok(response);

        } catch (InvalidCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            if (!userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email not found"));
            }

            // Generate 6-digit OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(999999));

            // Save OTP
            com.rev.app.entity.OtpVerification otpVerification = com.rev.app.entity.OtpVerification.builder()
                    .email(email)
                    .otp(otp)
                    .expiryTime(java.time.LocalDateTime.now().plusMinutes(10))
                    .build();

            otpRepository.deleteByEmail(email); // Remove any old OTPs for this email
            otpRepository.save(otpVerification);

            // Simulate sending email
            System.out.println("================================");
            System.out.println("OTP for " + email + ": " + otp);
            System.out.println("================================");

            return ResponseEntity.ok(Map.of("message", "OTP sent to your email (check console)"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and OTP are required"));
        }

        var otpOptional = otpRepository.findByEmailAndOtp(email, otp);
        if (otpOptional.isPresent()) {
            var otpVerification = otpOptional.get();
            if (otpVerification.getExpiryTime().isAfter(java.time.LocalDateTime.now())) {
                session.setAttribute("verifiedEmail", email);
                return ResponseEntity.ok(Map.of("message", "OTP verified successfully", "email", email));
            } else {
                return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "OTP has expired"));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid OTP"));
    }

    @GetMapping("/security-questions")
    public ResponseEntity<?> getSecurityQuestions(@RequestParam String email, HttpSession session) {
        String verifiedEmail = (String) session.getAttribute("verifiedEmail");
        if (verifiedEmail == null || !verifiedEmail.equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email not verified with OTP"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        var answers = userSecurityAnswerRepository.findByUser(user);
        var questions = answers.stream().map(a -> Map.of(
                "id", a.getSecurityQuestion().getId(),
                "question", a.getSecurityQuestion().getQuestionText())).toList();

        return ResponseEntity.ok(questions);
    }

    @PostMapping("/verify-security-answers")
    public ResponseEntity<?> verifySecurityAnswers(@RequestBody Map<String, Object> body, HttpSession session) {
        String email = (String) body.get("email");
        Map<String, String> providedAnswers = (Map<String, String>) body.get("answers");

        String verifiedEmail = (String) session.getAttribute("verifiedEmail");
        if (verifiedEmail == null || !verifiedEmail.equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email not verified with OTP"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "User not found"));
        }

        List<Long> questionIds = providedAnswers.keySet().stream().map(Long::parseLong).toList();
        List<String> answers = providedAnswers.values().stream().toList();

        if (userService.verifySecurityAnswers(email, questionIds, answers)) {
            session.setAttribute("securityVerified", true);
            return ResponseEntity.ok(Map.of("message", "Security questions verified successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Incorrect answer for one or more questions"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        String verifiedEmail = (String) session.getAttribute("verifiedEmail");
        Boolean securityVerified = (Boolean) session.getAttribute("securityVerified");

        if (email == null || newPassword == null || confirmPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }

        if (verifiedEmail == null || !verifiedEmail.equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email not verified or session expired"));
        }

        if (securityVerified == null || !securityVerified) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Security questions not verified"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        try {
            userService.resetMasterPassword(email, newPassword);
            session.removeAttribute("verifiedEmail");
            session.removeAttribute("securityVerified");
            otpRepository.deleteByEmail(email);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
}
