package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.RegisterUserRequest;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserResponse;
import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.enums.Role;
import com.manhnv.capstonebakerai.identity.exception.EmailAlreadyExistsException;
import com.manhnv.capstonebakerai.identity.exception.InvalidRegistrationRoleException;
import com.manhnv.capstonebakerai.identity.mapper.UserMapper;
import com.manhnv.capstonebakerai.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.manhnv.capstonebakerai.identity.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Set<Role> SELF_REGISTRATION_ROLES =
            EnumSet.of(Role.LEARNER, Role.STORE, Role.BAKER);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        if (!SELF_REGISTRATION_ROLES.contains(request.role())) {
            throw new InvalidRegistrationRoleException();
        }

        String normalizedEmail = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        User user = userMapper.toEntity(
                request,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                LocalDateTime.now(ZoneOffset.UTC)
        );
        user.setName(request.name().trim());

        User savedUser;

        // luu nguoi dung va handle truong hop 2 request dang ky cung email cung luc
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }

        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getUserId()));
        return userMapper.toRegisterResponse(savedUser);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
