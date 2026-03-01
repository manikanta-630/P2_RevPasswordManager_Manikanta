package com.rev.app.Config;

import com.rev.app.entity.SecurityQuestion;
import com.rev.app.repository.ISecurityQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

    private final ISecurityQuestionRepository securityQuestionRepository;

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            if (securityQuestionRepository.count() == 0) {
                List<SecurityQuestion> questions = Arrays.asList(
                        SecurityQuestion.builder().questionText("What is your mother's maiden name?").build(),
                        SecurityQuestion.builder().questionText("What was the name of your first pet?").build(),
                        SecurityQuestion.builder().questionText("What was the name of your elementary school?").build(),
                        SecurityQuestion.builder().questionText("What city were you born in?").build(),
                        SecurityQuestion.builder().questionText("What is your favorite book?").build()
                );
                securityQuestionRepository.saveAll(questions);
                System.out.println("Default security questions seeded into the database.");
            }
        };
    }
}
