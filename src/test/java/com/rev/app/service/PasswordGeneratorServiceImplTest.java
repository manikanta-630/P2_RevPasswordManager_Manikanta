package com.rev.app.service;

import com.rev.app.dto.PasswordGeneratorDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordGeneratorServiceImplTest {

    private PasswordGeneratorServiceImpl generatorService;
    private PasswordGeneratorDto config;

    @BeforeEach
    void setUp() {
        generatorService = new PasswordGeneratorServiceImpl();
        config = new PasswordGeneratorDto();
        config.setLength(8);
        config.setIncludeUppercase(true);
        config.setIncludeLowercase(true);
        config.setIncludeNumbers(true);
        config.setIncludeSpecialCharacters(true);
        config.setExcludeSimilarCharacters(true);
    }

    @Test
    void generatePassword_ValidLengths() {
        config.setLength(1);
        assertNotNull(generatorService.generatePassword(config));

        config.setLength(12);
        assertNotNull(generatorService.generatePassword(config));
    }

    @Test
    void generatePassword_ShortLengthWithAllCategories() {
        config.setLength(3);
        config.setIncludeUppercase(true);
        config.setIncludeLowercase(true);
        config.setIncludeNumbers(true);
        config.setIncludeSpecialCharacters(true);

        String password = generatorService.generatePassword(config);
        assertNotNull(password);
        assertEquals(3, password.length(), "Password length should be exactly 3");
    }

    @Test
    void generatePassword_InvalidLengths() {
        config.setLength(0);
        assertThrows(IllegalArgumentException.class, () -> generatorService.generatePassword(config));

        config.setLength(13);
        assertThrows(IllegalArgumentException.class, () -> generatorService.generatePassword(config));
    }

    @Test
    void calculatePasswordStrength_Weak_Short() {
        // Length 1-5 is always Weak
        assertEquals("Weak", generatorService.calculatePasswordStrength("A1!b2"));
    }

    @Test
    void calculatePasswordStrength_Medium_6To7() {
        // Length 6-7: All types + No similar = Medium
        assertEquals("Medium", generatorService.calculatePasswordStrength("Ab2!c3"));
        // Disabled type or similar char = Weak
        assertEquals("Weak", generatorService.calculatePasswordStrength("abcdef"));
        assertEquals("Weak", generatorService.calculatePasswordStrength("Ab1!c2")); // '1' is similar
    }

    @Test
    void calculatePasswordStrength_Strong_8To9() {
        // Length 8-9: All types + No similar = Strong
        assertEquals("Strong", generatorService.calculatePasswordStrength("Ab2!Cd3@"));
        // Disabled type = Weak
        assertEquals("Weak", generatorService.calculatePasswordStrength("abcdefgh"));
    }

    @Test
    void calculatePasswordStrength_VeryStrong_10Plus() {
        // Length 10+: All types + No similar = Very Strong
        assertEquals("Very Strong", generatorService.calculatePasswordStrength("Ab2!Cd3@Ef4#"));
        // Disabled types = Strong
        assertEquals("Strong", generatorService.calculatePasswordStrength("abcdefghij"));
    }
}
