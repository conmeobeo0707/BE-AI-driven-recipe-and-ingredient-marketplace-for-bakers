package com.manhnv.capstonebakerai.identity.event;

import com.manhnv.capstonebakerai.user.enums.Role;

import java.util.UUID;

public record StaffAccountCreatedEvent(UUID userId, String email, String fullName, Role role) {
}
