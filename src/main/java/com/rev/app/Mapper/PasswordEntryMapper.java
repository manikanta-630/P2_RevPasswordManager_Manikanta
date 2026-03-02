package com.rev.app.mapper;

import com.rev.app.dto.PasswordEntryDto;
import com.rev.app.entity.PasswordEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PasswordEntryMapper {

    public PasswordEntryDto toDto(PasswordEntry entity) {
        if (entity == null) {
            return null;
        }
        PasswordEntryDto dto = new PasswordEntryDto();
        dto.setAccountName(entity.getAccountName());
        dto.setWebsiteUrl(entity.getWebsiteUrl());
        dto.setUsernameEmail(entity.getUsernameEmail());
        dto.setCategory(entity.getCategory());
        dto.setNotes(entity.getNotes());
        // For security, we usually don't map the encrypted password directly to the DTO password field here unless needed.
        return dto;
    }

    public void updateEntityFromDto(PasswordEntryDto dto, PasswordEntry entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setAccountName(dto.getAccountName());
        entity.setWebsiteUrl(dto.getWebsiteUrl());
        entity.setUsernameEmail(dto.getUsernameEmail());
        entity.setCategory(dto.getCategory());
        entity.setNotes(dto.getNotes());
    }
    
    public List<PasswordEntryDto> toDtoList(List<PasswordEntry> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
