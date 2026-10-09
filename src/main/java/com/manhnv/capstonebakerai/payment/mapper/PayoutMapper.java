package com.manhnv.capstonebakerai.payment.mapper;

import com.manhnv.capstonebakerai.payment.dto.response.PayoutResponse;
import com.manhnv.capstonebakerai.payment.entity.Payout;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayoutMapper {
    PayoutResponse toPayoutResponse(Payout payout);
}
