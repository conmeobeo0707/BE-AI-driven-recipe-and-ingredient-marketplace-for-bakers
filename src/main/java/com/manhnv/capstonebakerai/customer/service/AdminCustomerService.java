package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.request.AdminUpdateCustomerRequest;
import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;

import java.util.List;
import java.util.UUID;

public interface AdminCustomerService {
    List<AdminCustomerResponse> getCustomers();

    AdminCustomerResponse updateCustomer(UUID customerId, AdminUpdateCustomerRequest request);

    AdminCustomerResponse updateCustomerStatus(UUID customerId, UpdateAccountStatusRequest request);
}
