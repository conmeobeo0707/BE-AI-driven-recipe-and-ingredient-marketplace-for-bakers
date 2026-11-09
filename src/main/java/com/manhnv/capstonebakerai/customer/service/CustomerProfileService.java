package com.manhnv.capstonebakerai.customer.service;

import com.manhnv.capstonebakerai.customer.dto.response.CustomerProfileResponse;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerPublicProfileResponse;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerHobbiesRequest;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerProfileRequest;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponse getProfile(UUID userId);

    CustomerPublicProfileResponse getPublicProfileByUserId(UUID userId);

    CustomerProfileResponse updateProfile(UUID userId, UpdateCustomerProfileRequest request);

    CustomerProfileResponse updateHobbies(UUID userId, UpdateCustomerHobbiesRequest request);
}
