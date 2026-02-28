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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final IUserService userService;
    private final ISecurityQuestionRepository securityQuestionRepository;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginDto", new LoginDto());
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@ModelAttribute("loginDto") LoginDto loginDto, HttpSession session, Model model) {
        try {
            User user = userService.loginUser(loginDto);
            // Store user in session
            session.setAttribute("loggedInUser", user);
            return "redirect:/dashboard";
        } catch (InvalidCredentialsException e) {
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
        try {
            userService.registerUser(registrationDto);
            return "redirect:/login?registered=true";
        } catch (UserAlreadyExistsException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            List<SecurityQuestion> questions = securityQuestionRepository.findAll();
            model.addAttribute("questions", questions);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }
}
