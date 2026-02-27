package com.rev.app.service;

import com.rev.app.dto.PasswordEntryDto;
import com.rev.app.entity.PasswordEntry;
import com.rev.app.entity.User;
import com.rev.app.mapper.PasswordEntryMapper;
import com.rev.app.repository.IPasswordEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordVaultServiceImpl implements IPasswordVaultService {

    private final IPasswordEntryRepository passwordEntryRepository;
    private final PasswordEntryMapper passwordEntryMapper;
    
    // In a real app, this should be an environment variable. Using a static key for P2 simplicity.
    private static final String ENCRYPTION_KEY = "RevatureSecureP2"; 

    @Transactional
    public PasswordEntry addPassword(User user, PasswordEntryDto dto) {
        PasswordEntry entry = PasswordEntry.builder()
                .user(user)
                .encryptedPassword(encrypt(dto.getPassword()))
                .isFavorite(false)
                .build();
        passwordEntryMapper.updateEntityFromDto(dto, entry);
        if (entry.getCategory() == null || entry.getCategory().isEmpty()) {
            entry.setCategory("Other");
        }
        return passwordEntryRepository.save(entry);
    }

    public List<PasswordEntry> getAllPasswords(User user) {
        return passwordEntryRepository.findByUser(user);
    }

    public List<PasswordEntry> getPasswordsByCategory(User user, String category) {
        return passwordEntryRepository.findByUserAndCategory(user, category);
    }

    public List<PasswordEntry> getFavoritePasswords(User user) {
        return passwordEntryRepository.findByUserAndIsFavoriteTrue(user);
    }

    public List<PasswordEntry> searchPasswords(User user, String query) {
        return passwordEntryRepository.findByUserAndAccountNameContainingIgnoreCaseOrWebsiteUrlContainingIgnoreCaseOrUsernameEmailContainingIgnoreCase(
                user, query, query, query
        );
    }

    @Transactional
    public PasswordEntry updatePassword(Long id, User user, PasswordEntryDto dto) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Password entry not found"));
                
        passwordEntryMapper.updateEntityFromDto(dto, entry);
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entry.setEncryptedPassword(encrypt(dto.getPassword()));
        }
        
        return passwordEntryRepository.save(entry);
    }

    @Transactional
    public void deletePassword(Long id, User user) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Password entry not found"));
        passwordEntryRepository.delete(entry);
    }

    @Transactional
    public PasswordEntry toggleFavorite(Long id, User user) {
        PasswordEntry entry = passwordEntryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Password entry not found"));
        entry.setFavorite(!entry.isFavorite());
        return passwordEntryRepository.save(entry);
    }
    
    public String decryptPassword(String encryptedPassword) {
        return decrypt(encryptedPassword);
    }

    // --- Basic AES Encryption Helper Methods ---
    private String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting password", e);
        }
    }

    private String decrypt(String encryptedText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting password", e);
        }
    }
}
