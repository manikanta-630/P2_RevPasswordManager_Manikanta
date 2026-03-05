package com.rev.app.repository;

import com.rev.app.entity.OtpVerification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface IOtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByEmailAndOtp(String email, String otp);
    
    @Modifying
    @Transactional
    void deleteByEmail(String email);
}
