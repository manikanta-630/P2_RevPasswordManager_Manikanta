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
        if (config.getLength() < 1 || config.getLength() > 12) {
            throw new IllegalArgumentException("Password length must be between 1 and 12 characters");
        }

        if (!config.isIncludeUppercase() && !config.isIncludeLowercase() &&
                !config.isIncludeNumbers() && !config.isIncludeSpecialCharacters()) {
            throw new IllegalArgumentException("At least one character type must be selected");
        }

        String uppercasePool = config.isExcludeSimilarCharacters() ? removeSimilar(UPPERCASE) : UPPERCASE;
        String lowercasePool = config.isExcludeSimilarCharacters() ? removeSimilar(LOWERCASE) : LOWERCASE;
        String numbersPool = config.isExcludeSimilarCharacters() ? removeSimilar(NUMBERS) : NUMBERS;
        String specialPool = SPECIAL;

        StringBuilder validChars = new StringBuilder();
        StringBuilder generatedPassword = new StringBuilder();

        if (config.isIncludeUppercase() && generatedPassword.length() < config.getLength()) {
            validChars.append(uppercasePool);
            generatedPassword.append(uppercasePool.charAt(random.nextInt(uppercasePool.length())));
        }
        if (config.isIncludeLowercase() && generatedPassword.length() < config.getLength()) {
            validChars.append(lowercasePool);
            generatedPassword.append(lowercasePool.charAt(random.nextInt(lowercasePool.length())));
        }
        if (config.isIncludeNumbers() && generatedPassword.length() < config.getLength()) {
            validChars.append(numbersPool);
            generatedPassword.append(numbersPool.charAt(random.nextInt(numbersPool.length())));
        }
        if (config.isIncludeSpecialCharacters() && generatedPassword.length() < config.getLength()) {
            validChars.append(specialPool);
            generatedPassword.append(specialPool.charAt(random.nextInt(specialPool.length())));
        }

        String validCharsStr = validChars.toString();

        for (int i = generatedPassword.length(); i < config.getLength(); i++) {
            generatedPassword.append(validCharsStr.charAt(random.nextInt(validCharsStr.length())));
        }

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
        if (password == null || password.isEmpty()) {
            return "Weak";
        }

        int length = password.length();
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        boolean hasSimilar = password.matches(".*[Il1O0].*");

        boolean allEnabled = hasUpper && hasLower && hasDigit && hasSpecial && !hasSimilar;

        if (length <= 5) {
            return "Weak";
        } else if (length <= 7) {
            return allEnabled ? "Medium" : "Weak";
        } else if (length <= 9) {
            return allEnabled ? "Strong" : "Weak";
        } else {
            return allEnabled ? "Very Strong" : "Strong";
        }
    }
}
