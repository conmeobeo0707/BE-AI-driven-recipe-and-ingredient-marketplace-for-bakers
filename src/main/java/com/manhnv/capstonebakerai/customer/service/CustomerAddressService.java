package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.request.CustomerAddressRequest;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerAddressService {
    List<CustomerAddressResponse> getMyAddresses(UUID userId);
    CustomerAddressResponse create(UUID userId, CustomerAddressRequest request);
    CustomerAddressResponse update(UUID userId, UUID addressId, CustomerAddressRequest request);
    void delete(UUID userId, UUID addressId);
    CustomerAddressResponse setDefault(UUID userId, UUID addressId);

    List<CustomerAddressResponse> getByCustomerId(UUID customerId);
}
