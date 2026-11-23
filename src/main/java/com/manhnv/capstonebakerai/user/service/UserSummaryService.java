package com.manhnv.capstonebakerai.user.service;

import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface UserSummaryService {
    /** One batched query for the whole set — call this once per list, never per row. */
    Map<UUID, UserSummaryResponse> getSummaries(Collection<UUID> userIds);

    /** Convenience for a single id (e.g. one record's create/update endpoint). */
    UserSummaryResponse getSummary(UUID userId);
}
