package com.rev.app.dto;

import lombok.Data;

@Data
public class PasswordGeneratorDto {
    private int length = 16;
    private boolean includeUppercase = true;
    private boolean includeLowercase = true;
    private boolean includeNumbers = true;
    private boolean includeSpecialCharacters = true;
    private boolean excludeSimilarCharacters = false;
}
