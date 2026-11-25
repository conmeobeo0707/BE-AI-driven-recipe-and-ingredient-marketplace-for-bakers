package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.request.AdminUpdateCustomerRequest;
import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminCustomerService {
    Page<AdminCustomerResponse> getCustomers(UserStatus status, String search, Pageable pageable);

    AdminCustomerResponse updateCustomer(UUID customerId, AdminUpdateCustomerRequest request);

    AdminCustomerResponse updateCustomerStatus(UUID customerId, UpdateAccountStatusRequest request);
}
