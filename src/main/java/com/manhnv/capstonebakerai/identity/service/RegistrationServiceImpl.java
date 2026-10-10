package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserRequest;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserResponse;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.identity.event.UserRegisteredEvent;
import com.manhnv.capstonebakerai.identity.exception.EmailAlreadyExistsException;
import com.manhnv.capstonebakerai.identity.exception.InvalidRegistrationRoleException;
import com.manhnv.capstonebakerai.identity.mapper.UserMapper;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        if (request.role() != Role.CUSTOMER) {
            throw new InvalidRegistrationRoleException();
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        User user = userMapper.toEntity(
                request,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                LocalDateTime.now(ZoneOffset.UTC)
        );
        user.setFullName(request.fullName().trim());

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }

        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setHobbyId(null);
        customer.setSkillLevel(null);
        customer.setConfirmed(false);
        customerRepository.saveAndFlush(customer);

        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getUserId()));
        return userMapper.toRegisterResponse(savedUser);
    }
}
