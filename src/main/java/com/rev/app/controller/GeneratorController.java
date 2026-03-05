package com.rev.app.Controller;

import com.rev.app.dto.PasswordEntryDto;
import com.rev.app.dto.PasswordGeneratorDto;
import com.rev.app.entity.User;
import com.rev.app.service.IPasswordGeneratorService;
import com.rev.app.service.IPasswordVaultService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final IPasswordGeneratorService generatorService;
    private final IPasswordVaultService vaultService;

    @GetMapping
    public String showGenerator(Model model) {
        model.addAttribute("genConfig", new PasswordGeneratorDto());
        model.addAttribute("newEntry", new PasswordEntryDto());
        return "generator";
    }

    @PostMapping("/generate")
    public String generatePassword(@ModelAttribute("genConfig") PasswordGeneratorDto genConfig, Model model) {
        try {
            String generatedPwd = generatorService.generatePassword(genConfig);
            String strength = generatorService.calculatePasswordStrength(generatedPwd);
            
            model.addAttribute("generatedPassword", generatedPwd);
            model.addAttribute("strength", strength);
            
            PasswordEntryDto newEntry = new PasswordEntryDto();
            newEntry.setPassword(generatedPwd);
            model.addAttribute("newEntry", newEntry);
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("newEntry", new PasswordEntryDto());
        }
        
        // Keep config state
        model.addAttribute("genConfig", genConfig);
        return "generator";
    }
    
    @PostMapping("/save")
    public String saveToVault(@ModelAttribute("newEntry") PasswordEntryDto newEntry, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        try {
            vaultService.addPassword(user, newEntry);
            return "redirect:/dashboard?success=true";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to save to vault: " + e.getMessage());
            model.addAttribute("genConfig", new PasswordGeneratorDto());
            return "generator";
        }
    }
}
