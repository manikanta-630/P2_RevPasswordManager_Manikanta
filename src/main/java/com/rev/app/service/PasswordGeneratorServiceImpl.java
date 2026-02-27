package com.rev.app.service;

import com.rev.app.dto.PasswordGeneratorDto;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PasswordGeneratorServiceImpl implements IPasswordGeneratorService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?/";
    
    private static final String SIMILAR_CHARS = "Il1O0";

    private final SecureRandom random = new SecureRandom();

    public String generatePassword(PasswordGeneratorDto config) {
        if (config.getLength() < 8 || config.getLength() > 64) {
            throw new IllegalArgumentException("Password length must be between 8 and 64 characters");
        }
        
        if (!config.isIncludeUppercase() && !config.isIncludeLowercase() && 
            !config.isIncludeNumbers() && !config.isIncludeSpecialCharacters()) {
            throw new IllegalArgumentException("At least one character type must be selected");
        }

        String uppercasePool = config.isExcludeSimilarCharacters() ? removeSimilar(UPPERCASE) : UPPERCASE;
        String lowercasePool = config.isExcludeSimilarCharacters() ? removeSimilar(LOWERCASE) : LOWERCASE;
        String numbersPool = config.isExcludeSimilarCharacters() ? removeSimilar(NUMBERS) : NUMBERS;
        String specialPool = SPECIAL; // No standard similar chars in specials usually

        StringBuilder validChars = new StringBuilder();
        StringBuilder generatedPassword = new StringBuilder();

        // Ensure at least one from each selected category to guarantee inclusion
        if (config.isIncludeUppercase()) {
            validChars.append(uppercasePool);
            generatedPassword.append(uppercasePool.charAt(random.nextInt(uppercasePool.length())));
        }
        if (config.isIncludeLowercase()) {
            validChars.append(lowercasePool);
            generatedPassword.append(lowercasePool.charAt(random.nextInt(lowercasePool.length())));
        }
        if (config.isIncludeNumbers()) {
            validChars.append(numbersPool);
            generatedPassword.append(numbersPool.charAt(random.nextInt(numbersPool.length())));
        }
        if (config.isIncludeSpecialCharacters()) {
            validChars.append(specialPool);
            generatedPassword.append(specialPool.charAt(random.nextInt(specialPool.length())));
        }

        String validCharsStr = validChars.toString();

        // Fill the rest of the password
        for (int i = generatedPassword.length(); i < config.getLength(); i++) {
            generatedPassword.append(validCharsStr.charAt(random.nextInt(validCharsStr.length())));
        }

        // Shuffle the characters so the guaranteed ones aren't always at the start
        return shuffleString(generatedPassword.toString());
    }

    private String removeSimilar(String pool) {
        StringBuilder result = new StringBuilder();
        for (char c : pool.toCharArray()) {
            if (SIMILAR_CHARS.indexOf(c) == -1) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            int randomIndex = random.nextInt(characters.length);
            char temp = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temp;
        }
        return new String(characters);
    }
    
    public String calculatePasswordStrength(String password) {
        if (password == null || password.length() == 0) return "Weak";
        
        int score = 0;
        
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;
        
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;
        
        if (score <= 3) return "Weak";
        if (score <= 5) return "Medium";
        if (score <= 6) return "Strong";
        return "Very Strong";
    }
}
