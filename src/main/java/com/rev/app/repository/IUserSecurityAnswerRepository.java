package com.rev.app.repository;

import com.rev.app.entity.User;
import com.rev.app.entity.UserSecurityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserSecurityAnswerRepository extends JpaRepository<UserSecurityAnswer, Long> {
    List<UserSecurityAnswer> findByUser(User user);
}
