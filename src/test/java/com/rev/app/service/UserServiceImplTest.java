package com.rev.app.service;

import com.rev.app.dto.LoginDto;
import com.rev.app.dto.UserRegistrationDto;
import com.rev.app.entity.SecurityQuestion;
import com.rev.app.entity.User;
import com.rev.app.exception.InvalidCredentialsException;
import com.rev.app.exception.UserAlreadyExistsException;
import com.rev.app.mapper.UserMapper;
import com.rev.app.repository.ISecurityQuestionRepository;
import com.rev.app.repository.IUserRepository;
import com.rev.app.repository.IUserSecurityAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ISecurityQuestionRepository securityQuestionRepository;

    @Mock
    private IUserSecurityAnswerRepository userSecurityAnswerRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private User user;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setMasterPassword("password123");
        registrationDto.setConfirmPassword("password123");
        registrationDto.setSecurityQuestionIds(Arrays.asList(1L, 2L, 3L));
        registrationDto.setSecurityAnswers(Arrays.asList("answer1", "answer2", "answer3"));

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .masterPasswordHash(userService.hashPassword("password123"))
                .build();
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(securityQuestionRepository.findById(anyLong())).thenReturn(Optional.of(new SecurityQuestion()));

        User registeredUser = userService.registerUser(registrationDto);

        assertNotNull(registeredUser);
        assertEquals("testuser", registeredUser.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userSecurityAnswerRepository, times(3)).save(any());
    }

    @Test
    void registerUser_AlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDto);
        });
    }

    @Test
    void loginUser_Success() {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setMasterPassword("password123");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));

        User loggedInUser = userService.loginUser(loginDto);

        assertNotNull(loggedInUser);
        assertEquals("testuser", loggedInUser.getUsername());
    }

    @Test
    void loginUser_InvalidCredentials() {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsernameOrEmail("wronguser");
        loginDto.setMasterPassword("wrongpassword");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> {
            userService.loginUser(loginDto);
        });
    }

    @Test
    void loginUser_WrongPassword() {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setMasterPassword("wrongpassword");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> {
            userService.loginUser(loginDto);
        });
    }
}
