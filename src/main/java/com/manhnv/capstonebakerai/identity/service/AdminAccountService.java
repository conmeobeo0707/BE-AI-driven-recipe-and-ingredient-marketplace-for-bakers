package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.identity.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.identity.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.identity.dto.UpdateStaffAccountRequest;

import java.util.List;
import java.util.UUID;

public interface AdminAccountService {

    StaffAccountResponse create(CreateStaffAccountRequest request);

    List<StaffAccountResponse> getAccounts();

    StaffAccountResponse getAccount(UUID id);

    StaffAccountResponse update(UUID id, UpdateStaffAccountRequest request);

    StaffAccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request);
}
