package com.firomsa.maaedBackend.v1.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.firomsa.maaedBackend.model.User;
import com.firomsa.maaedBackend.v1.dto.RegisterRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterResponseDTO;

@Mapper(componentModel = "spring")
public interface RegisterMapper {
    @Mapping(source = "role.name", target = "role")
    @Mapping(target = "createdAt", dateFormat = "dd.MM.yyyy")
    RegisterResponseDTO toDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toModel(RegisterRequestDTO requestDTO);
}
