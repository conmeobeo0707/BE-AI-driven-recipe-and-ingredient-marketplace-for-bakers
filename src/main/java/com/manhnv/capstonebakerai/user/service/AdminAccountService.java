package com.manhnv.capstonebakerai.user.service;

import com.manhnv.capstonebakerai.user.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.dto.UpdateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.enums.UserStatus;

import java.util.List;
import java.util.UUID;

public interface AdminAccountService {

    StaffAccountResponse create(CreateStaffAccountRequest request);

    List<StaffAccountResponse> getAccounts(UserStatus status);

    StaffAccountResponse getAccount(UUID id);

    StaffAccountResponse update(UUID id, UpdateStaffAccountRequest request);

    StaffAccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request);
}
