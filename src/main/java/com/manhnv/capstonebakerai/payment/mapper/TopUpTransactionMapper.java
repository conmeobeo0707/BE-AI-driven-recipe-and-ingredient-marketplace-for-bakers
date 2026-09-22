package com.manhnv.capstonebakerai.payment.mapper;

import com.manhnv.capstonebakerai.payment.dto.response.TopUpTransactionResponse;
import com.manhnv.capstonebakerai.payment.entity.TopUpTransaction;
import org.mapstruct.Mapper;


@Mapper
public interface TopUpTransactionMapper {
    TopUpTransactionResponse toResponse(TopUpTransaction topUpTransaction);
}
