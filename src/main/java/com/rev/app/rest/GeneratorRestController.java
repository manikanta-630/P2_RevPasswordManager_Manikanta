package com.rev.app.rest;

import com.rev.app.dto.PasswordGeneratorDto;
import com.rev.app.service.IPasswordGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class GeneratorRestController {

    private final IPasswordGeneratorService generatorService;

    @PostMapping("/generate")
    public ResponseEntity<?> generatePassword(@RequestBody PasswordGeneratorDto config) {
        try {
            String password = generatorService.generatePassword(config);
            String strength = generatorService.calculatePasswordStrength(password);
            
            Map<String, String> response = new HashMap<>();
            response.put("password", password);
            response.put("strength", strength);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeStrength(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null) {
            return ResponseEntity.badRequest().body("Password is required");
        }
        
        String strength = generatorService.calculatePasswordStrength(password);
        Map<String, String> response = new HashMap<>();
        response.put("strength", strength);
        
        return ResponseEntity.ok(response);
    }
}
