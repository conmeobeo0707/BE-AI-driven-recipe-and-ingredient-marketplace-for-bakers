package com.manhnv.capstonebakerai.identity.mapper;

import com.manhnv.capstonebakerai.identity.dto.RegisterUserRequest;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserResponse;
import com.manhnv.capstonebakerai.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "email", source = "normalizedEmail")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isConfirmed", constant = "false")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", source = "registeredAt")
    @Mapping(target = "updatedAt", source = "registeredAt")
    User toEntity(
            RegisterUserRequest request,
            String normalizedEmail,
            String encodedPassword,
            LocalDateTime registeredAt
    );

    RegisterUserResponse toRegisterResponse(User user);
}
