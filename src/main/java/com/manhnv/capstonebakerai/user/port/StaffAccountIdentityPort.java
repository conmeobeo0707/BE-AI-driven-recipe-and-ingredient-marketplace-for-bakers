package com.manhnv.capstonebakerai.user.port;

import com.manhnv.capstonebakerai.user.entity.User;

import java.time.LocalDateTime;

/** Identity operations needed by the user-account administration use case. */
public interface StaffAccountIdentityPort {

    void initializeCredentials(User user);

    void revokeActiveSessions(User user, LocalDateTime revokedAt);
}
