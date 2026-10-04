package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.identity.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.identity.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.identity.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.identity.dto.UpdateStaffAccountRequest;
import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.enums.Role;
import com.manhnv.capstonebakerai.identity.enums.UserStatus;
import com.manhnv.capstonebakerai.identity.exception.IdentityErrorCode;
import com.manhnv.capstonebakerai.identity.repository.RefreshTokenRepository;
import com.manhnv.capstonebakerai.identity.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements  AdminAccountService{

    private static final Set<Role> MANAGED_ROLES = Set.of(Role.ADMIN, Role.STAFF, Role.BAKER_STAFF);
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffAccountResponse create(CreateStaffAccountRequest request) {
        validateRole(request.role());

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .phone(request.phone())
                .address(request.address())
                .bod(request.dateOfBirth())
                .gender(request.gender())
                .avatarUrl(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                throw new AppException(
                        IdentityErrorCode.EMAIL_ALREADY_EXISTS
                );
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffAccountResponse> getAccounts() {
        return userRepository
                .findByRoleInOrderByCreatedAtDescUserIdAsc(MANAGED_ROLES)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffAccountResponse getAccount(UUID id) {
        return toResponse(findManagedAccount(id));
    }

    @Override
    public StaffAccountResponse update(UUID id, UpdateStaffAccountRequest request) {
        validateRole(request.role());

        User user = findManagedAccount(id);
        boolean roleChanged = user.getRole() != request.role();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setBod(request.dateOfBirth());
        user.setGender(request.gender());
        user.setUpdatedAt(now);

        User savedUser = userRepository.saveAndFlush(user);

        if (roleChanged) {
            refreshTokenRepository.revokeAllActiveByUser(savedUser, now);
        }
        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public StaffAccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request) {
        User user = findManagedAccount(id);

        if (user.getStatus() == request.status()) {
            return toResponse(user);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setStatus(request.status());
        user.setUpdatedAt(now);

        User savedUser = userRepository.saveAndFlush(user);
        refreshTokenRepository.revokeAllActiveByUser(savedUser, now);

        return toResponse(savedUser);
    }

    private void validateRole(Role role) {
        if (role == null || !MANAGED_ROLES.contains(role)) {
            throw new AppException(IdentityErrorCode.INVALID_STAFF_ROLE);
        }
    }

    private User findManagedAccount(UUID id) {
        return userRepository.findById(id)
                .filter(user -> MANAGED_ROLES.contains(user.getRole()))
                .orElseThrow(() -> new AppException(
                        IdentityErrorCode.ACCOUNT_NOT_FOUND
                ));
    }

    private StaffAccountResponse toResponse(User user) {
        return new StaffAccountResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getPhone(),
                user.getAddress(),
                user.getBod(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
