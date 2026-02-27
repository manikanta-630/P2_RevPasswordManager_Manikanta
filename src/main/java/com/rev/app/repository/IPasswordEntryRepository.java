package com.rev.app.repository;

import com.rev.app.entity.PasswordEntry;
import com.rev.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {
    List<PasswordEntry> findByUser(User user);
    List<PasswordEntry> findByUserAndCategory(User user, String category);
    List<PasswordEntry> findByUserAndIsFavoriteTrue(User user);
    Optional<PasswordEntry> findByIdAndUser(Long id, User user);
    List<PasswordEntry> findByUserAndAccountNameContainingIgnoreCaseOrWebsiteUrlContainingIgnoreCaseOrUsernameEmailContainingIgnoreCase(
            User user, String accountName, String websiteUrl, String usernameEmail
    );
}
