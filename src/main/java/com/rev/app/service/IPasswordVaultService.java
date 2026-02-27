package com.rev.app.service;

import com.rev.app.dto.PasswordEntryDto;
import com.rev.app.entity.PasswordEntry;
import com.rev.app.entity.User;

import java.util.List;

public interface IPasswordVaultService {
    PasswordEntry addPassword(User user, PasswordEntryDto dto);
    List<PasswordEntry> getAllPasswords(User user);
    List<PasswordEntry> getPasswordsByCategory(User user, String category);
    List<PasswordEntry> getFavoritePasswords(User user);
    List<PasswordEntry> searchPasswords(User user, String query);
    PasswordEntry updatePassword(Long id, User user, PasswordEntryDto dto);
    void deletePassword(Long id, User user);
    PasswordEntry toggleFavorite(Long id, User user);
    String decryptPassword(String encryptedPassword);
}
