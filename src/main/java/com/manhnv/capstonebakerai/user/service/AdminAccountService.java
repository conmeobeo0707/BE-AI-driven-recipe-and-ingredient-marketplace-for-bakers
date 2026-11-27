package com.manhnv.capstonebakerai.user.service;

import com.manhnv.capstonebakerai.user.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.dto.UpdateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminAccountService {

    StaffAccountResponse create(CreateStaffAccountRequest request);

    Page<StaffAccountResponse> getAccounts(UserStatus status, Role role, String search, Pageable pageable);

    StaffAccountResponse getAccount(UUID id);

    StaffAccountResponse update(UUID id, UpdateStaffAccountRequest request);

    StaffAccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request);
}
