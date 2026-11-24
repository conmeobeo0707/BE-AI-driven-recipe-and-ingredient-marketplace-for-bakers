package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.dto.request.AdminUpdateCustomerRequest;
import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.port.StaffAccountIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepository;
    private final StaffAccountIdentityPort identityPort;

    @Override
    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> getCustomers() {
        List<Customer> customers = customerRepository.findAllWithUserOrderByCreatedAtDesc();
        return customers
                .stream()
                .map(this::toResponse)
                .toList();
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
