package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.baker.entity.Baker;
import com.manhnv.capstonebakerai.baker.repository.BakerRepository;
import com.manhnv.capstonebakerai.learner.entity.Learner;
import com.manhnv.capstonebakerai.learner.repository.LearnerRepository;
import com.manhnv.capstonebakerai.store.entity.Store;
import com.manhnv.capstonebakerai.store.enums.StoreStatus;
import com.manhnv.capstonebakerai.store.repository.StoreRepository;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserRequest;
import com.manhnv.capstonebakerai.identity.dto.RegisterUserResponse;
import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.enums.Role;
import com.manhnv.capstonebakerai.identity.event.UserRegisteredEvent;
import com.manhnv.capstonebakerai.identity.exception.EmailAlreadyExistsException;
import com.manhnv.capstonebakerai.identity.exception.InvalidRegistrationRoleException;
import com.manhnv.capstonebakerai.identity.mapper.UserMapper;
import com.manhnv.capstonebakerai.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class RegistrationServiceImpl implements RegistrationService {

    private static final Set<Role> SELF_REGISTRATION_ROLES =
            EnumSet.of(Role.LEARNER, Role.STORE, Role.BAKER);

    private final UserRepository userRepository;
    private final BakerRepository bakerRepository;
    private final LearnerRepository learnerRepository;
    private final StoreRepository storeRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        if (!SELF_REGISTRATION_ROLES.contains(request.role())) {
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
        user.setName(request.name().trim());

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }

        if (savedUser.getRole() == Role.BAKER) {
            Baker baker = new Baker();
            baker.setUser(savedUser);
            bakerRepository.save(baker);
        } else if (savedUser.getRole() == Role.LEARNER) {
            Learner learner = new Learner();
            learner.setUser(savedUser);
            learnerRepository.save(learner);
        } else if (savedUser.getRole() == Role.STORE) {
            Store store = new Store();
            store.setUser(savedUser);
            store.setStatus(StoreStatus.CLOSED); //mac dinh khi tao se la CLOSED
            store.setCreatedAt(savedUser.getCreatedAt());
            storeRepository.save(store);
        }

        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getUserId()));
        return userMapper.toRegisterResponse(savedUser);
    }
}
