package com.rev.app.Controller;

import com.rev.app.entity.PasswordEntry;
import com.rev.app.entity.User;
import com.rev.app.service.IPasswordVaultService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final IPasswordVaultService vaultService;

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model, @RequestParam(required = false) String search) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<PasswordEntry> entries;
        if (search != null && !search.trim().isEmpty()) {
            entries = vaultService.searchPasswords(user, search.trim());
            model.addAttribute("searchQuery", search);
        } else {
            entries = vaultService.getAllPasswords(user);
        }

        model.addAttribute("entries", entries);
        model.addAttribute("totalCount", entries.size());
        
        long favCount = entries.stream().filter(PasswordEntry::isFavorite).count();
        model.addAttribute("favoriteCount", favCount);

        return "dashboard";
    }
}
