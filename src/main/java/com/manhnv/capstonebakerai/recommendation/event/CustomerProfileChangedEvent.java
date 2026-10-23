package com.manhnv.capstonebakerai.recommendation.event;

import java.util.UUID;

public record CustomerProfileChangedEvent(UUID customerId) {
}
