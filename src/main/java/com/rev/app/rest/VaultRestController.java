package com.rev.app.rest;

import com.rev.app.dto.PasswordEntryDto;
import com.rev.app.entity.PasswordEntry;
import com.rev.app.entity.User;
import com.rev.app.service.IPasswordVaultService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rev.app.service.IUserService;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultRestController {

    private final IPasswordVaultService vaultService;
    private final IUserService userService;

    // Helper method to retrieve logged-in user from session
    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    @PostMapping("/add")
    public ResponseEntity<?> addPassword(@RequestBody PasswordEntryDto dto, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        try {
            PasswordEntry entry = vaultService.addPassword(user, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(entry);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllPasswords(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        List<PasswordEntry> entries = vaultService.getAllPasswords(user);
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/search")
    public ResponseEntity<?> searchPasswords(@RequestParam String q, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        List<PasswordEntry> entries = vaultService.searchPasswords(user, q);
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/decrypt/{id}")
    public ResponseEntity<?> getDecryptedPassword(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        
        String masterPassword = body.get("masterPassword");
        if (masterPassword == null || masterPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Master password is required");
        }

        try {
            // Verify master password
            if (!user.getMasterPasswordHash().equals(userService.hashPassword(masterPassword))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid master password");
            }

            // Need to fetch entry to get encrypted password and ensure user owns it
            PasswordEntry entry = vaultService.getAllPasswords(user).stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));
                
            String plainText = vaultService.decryptPassword(entry.getEncryptedPassword());
            Map<String, String> response = new HashMap<>();
            response.put("password", plainText);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/favorite/{id}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long id, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        try {
            PasswordEntry entry = vaultService.toggleFavorite(id, user);
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @RequestBody PasswordEntryDto dto, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }

        String masterPassword = dto.getMasterPassword();
        if (masterPassword == null || masterPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Master password is required for editing");
        }

        try {
            // Verify master password
            if (!user.getMasterPasswordHash().equals(userService.hashPassword(masterPassword))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid master password");
            }

            PasswordEntry entry = vaultService.updatePassword(id, user, dto);
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePassword(@PathVariable Long id, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        try {
            vaultService.deletePassword(id, user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
