package com.rev.app.Controller;

import com.rev.app.dto.LoginDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.SecurityQuestion;
import com.rev.app.entity.User;
import com.rev.app.exception.InvalidCredentialsException;
import com.rev.app.exception.UserAlreadyExistsException;
import com.rev.app.repository.ISecurityQuestionRepository;
import com.rev.app.service.IUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final IUserService userService;
    private final ISecurityQuestionRepository securityQuestionRepository;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("currentUser", user);
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/";
        }
        model.addAttribute("loginDto", new LoginDto());
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@ModelAttribute("loginDto") LoginDto loginDto, HttpSession session, Model model) {
        log.info("Received login request for: {}", loginDto.getUsernameOrEmail());
        try {
            User user = userService.loginUser(loginDto);

            if (user.isTwoFactorEnabled()) {
                session.setAttribute("tempUser", user);
                userService.logCurrentTwoFactorCode(user);
                log.info("User {} has 2FA enabled. Redirecting to verification.", user.getUsername());
                return "redirect:/verify-2fa";
            }

            // Store user in session
            session.setAttribute("loggedInUser", user);
            log.info("User {} successfully logged in and session started.", user.getUsername());
            return "redirect:/";
        } catch (InvalidCredentialsException e) {
            log.warn("Login failed for {}: {}", loginDto.getUsernameOrEmail(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDto", new UserRegistrationDto());
        List<SecurityQuestion> questions = securityQuestionRepository.findAll();
        model.addAttribute("questions", questions);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationDto") UserRegistrationDto registrationDto, Model model) {
        log.info("Received registration request for: {}", registrationDto.getUsername());
        try {
            userService.registerUser(registrationDto);
            log.info("User {} successfully registered.", registrationDto.getUsername());
            return "redirect:/login?registered=true";
        } catch (UserAlreadyExistsException | IllegalArgumentException e) {
            log.warn("Registration failed for {}: {}", registrationDto.getUsername(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            List<SecurityQuestion> questions = securityQuestionRepository.findAll();
            model.addAttribute("questions", questions);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            log.info("User {} is logging out.", user.getUsername());
        }
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @GetMapping("/verify-2fa")
    public String showVerify2faForm(HttpSession session, Model model) {
        if (session.getAttribute("tempUser") == null) {
            return "redirect:/login";
        }
        return "verify-2fa";
    }

    @PostMapping("/verify-2fa")
    public String verify2fa(@RequestParam("code") String code, HttpSession session, Model model) {
        User user = (User) session.getAttribute("tempUser");
        if (user == null) {
            log.warn("2FA verification attempt without tempUser in session");
            return "redirect:/login";
        }

        log.info("Processing 2FA verification for user: {}", user.getUsername());
        if (userService.verifyTwoFactorCode(user, code)) {
            session.removeAttribute("tempUser");
            session.setAttribute("loggedInUser", user);
            log.info("2FA verified successfully for user: {}. Session started.", user.getUsername());
            return "redirect:/";
        } else {
            log.warn("2FA verification failed for user: {}. Code entered: {}", user.getUsername(), code);
            model.addAttribute("error", "Invalid verification code");
            return "verify-2fa";
        }
    }
}
