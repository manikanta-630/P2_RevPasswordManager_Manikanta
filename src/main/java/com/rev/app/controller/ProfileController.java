package com.rev.app.Controller;

import com.rev.app.dto.PasswordChangeDto;
import com.rev.app.dto.UserProfileUpdateDto;
import com.rev.app.entity.User;
import com.rev.app.service.IUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final IUserService userService;

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        UserProfileUpdateDto profileDto = new UserProfileUpdateDto();
        profileDto.setName(user.getName());
        profileDto.setEmail(user.getEmail());
        profileDto.setPhoneNumber(user.getPhoneNumber());

        model.addAttribute("profileDto", profileDto);
        model.addAttribute("passwordDto", new PasswordChangeDto());

        if (user.isTwoFactorEnabled()) {
            userService.logCurrentTwoFactorCode(user);
        }

        return "profile";
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public String sendOtp(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isTwoFactorEnabled()) {
            userService.logCurrentTwoFactorCode(user);
            return "OTP sent successfully (Check console)";
        }
        return "2FA not enabled";
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public boolean verifyOtp(@RequestParam String otp, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.isTwoFactorEnabled()) {
            return userService.verifyTwoFactorCode(user, otp);
        }
        return true; // Not required if 2FA is off
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("profileDto") UserProfileUpdateDto profileDto,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            User updatedUser = userService.updateProfile(user, profileDto);
            session.setAttribute("loggedInUser", updatedUser);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute("passwordDto") PasswordChangeDto passwordDto,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            userService.changeMasterPassword(user, passwordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Master password changed successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/toggle-2fa")
    public String toggleTwoFactor(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)
            return "redirect:/login";

        boolean newState = !user.isTwoFactorEnabled();
        userService.setTwoFactorEnabled(user, newState);

        // Update user in session
        session.setAttribute("loggedInUser", user);

        String msg = newState ? "Two-Factor Authentication enabled!" : "Two-Factor Authentication disabled.";
        redirectAttributes.addFlashAttribute("successMessage", msg);
        return "redirect:/profile";
    }
}
