package com.manhnv.capstonebakerai.identity.event;

import java.util.UUID;

public record EmailVerifiedEvent(UUID userId, UUID customerId) {
}
