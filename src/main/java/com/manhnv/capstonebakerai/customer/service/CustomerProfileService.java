package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.CustomerProfileResponse;
import com.manhnv.capstonebakerai.customer.dto.UpdateCustomerHobbiesRequest;
import com.manhnv.capstonebakerai.customer.dto.UpdateCustomerProfileRequest;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponse getProfile(UUID userId);

    CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request);

    CustomerProfileResponse updateHobbies(UUID userId, UpdateCustomerHobbiesRequest request);
}
