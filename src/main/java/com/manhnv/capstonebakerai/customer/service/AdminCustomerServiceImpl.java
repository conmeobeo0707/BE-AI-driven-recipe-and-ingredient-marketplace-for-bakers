package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> getCustomers() {
        List<Customer> customers = customerRepository.findAllWithUserOrderByCreatedAtDesc();
        return customers
                .stream()
                .map(this::toResponse)
                .toList();
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
