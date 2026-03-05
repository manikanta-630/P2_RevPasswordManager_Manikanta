package com.rev.app.repository;

import com.rev.app.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {
    Optional<SecurityQuestion> findByQuestionText(String questionText);
}
