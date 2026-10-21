package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;

import java.util.List;

public interface AdminCustomerService {
    List<AdminCustomerResponse> getCustomers();
}
