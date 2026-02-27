package com.rev.app.service;

import com.rev.app.dto.PasswordGeneratorDto;

public interface IPasswordGeneratorService {
    String generatePassword(PasswordGeneratorDto config);
    String calculatePasswordStrength(String password);
}
