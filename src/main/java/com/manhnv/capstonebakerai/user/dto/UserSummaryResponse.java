package com.manhnv.capstonebakerai.user.dto;

import java.util.UUID;

/** Lightweight actor info (who created/handled something) embedded in other modules' responses,
 * so callers don't need a separate lookup to show a name/avatar next to a raw user id. */
public record UserSummaryResponse(UUID id, String fullName, String avatarUrl) {
}
