package com.manhnv.capstonebakerai.order.dto.response;

import java.math.BigDecimal;

public record ExpectedPointsResponse(
        int expectedPoints,
        BigDecimal pointBase
) {
}
