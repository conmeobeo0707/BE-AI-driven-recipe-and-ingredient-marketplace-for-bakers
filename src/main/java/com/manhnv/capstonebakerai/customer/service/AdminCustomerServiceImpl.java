package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.dto.request.AdminUpdateCustomerRequest;
import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import com.manhnv.capstonebakerai.user.exception.UserErrorCode;
import com.manhnv.capstonebakerai.user.port.StaffAccountIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepository;
    private final StaffAccountIdentityPort identityPort;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminCustomerResponse> getCustomers(UserStatus status, String search, Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new AppException(UserErrorCode.INVALID_PAGE_REQUEST);
        }
        Specification<Customer> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("status"), status));
        }
        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("user").get("fullName")), keyword),
                    cb.like(cb.lower(root.get("user").get("email")), keyword)
            ));
        }
        return customerRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public AdminCustomerResponse updateCustomer(UUID customerId, AdminUpdateCustomerRequest request) {
        Customer customer = findCustomer(customerId);
        User user = customer.getUser();

        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setBod(request.dateOfBirth());
        user.setGender(request.gender());
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return toResponse(customer);
    }

    @Override
    @Transactional
    public AdminCustomerResponse updateCustomerStatus(UUID customerId, UpdateAccountStatusRequest request) {
        Customer customer = findCustomer(customerId);
        User user = customer.getUser();

        if (user.getStatus() == request.status()) {
            return toResponse(customer);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setStatus(request.status());
        user.setUpdatedAt(now);
        identityPort.revokeActiveSessions(user, now);

        return toResponse(customer);
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository.findByIdWithUser(customerId)
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    private AdminCustomerResponse toResponse(Customer customer) {
        var user = customer.getUser();
        return new AdminCustomerResponse(
                customer.getId(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getStatus(),
                user.getPhone(),
                user.getAddress(),
                user.getBod(),
                user.getGender(),
                user.getAvatarUrl(),
                customer.getSkillLevel(),
                customer.isConfirmed(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
