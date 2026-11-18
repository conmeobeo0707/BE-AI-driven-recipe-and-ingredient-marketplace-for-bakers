package com.manhnv.capstonebakerai.user.service;

import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserSummaryServiceImpl implements UserSummaryService {
    private final UserRepository users;

    public UserSummaryServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSummaryResponse> getSummaries(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return users.findSummariesByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserSummaryResponse::id, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getSummary(UUID userId) {
        if (userId == null) return null;
        return getSummaries(java.util.List.of(userId)).getOrDefault(userId, new UserSummaryResponse(userId, null, null));
    }
}
