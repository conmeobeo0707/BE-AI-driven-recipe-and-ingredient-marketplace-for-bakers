package com.manhnv.capstonebakerai.payment.mapper;

import com.manhnv.capstonebakerai.payment.dto.response.WalletResponse;
import com.manhnv.capstonebakerai.payment.entity.Wallet;
import org.mapstruct.Mapper;

@Mapper
public interface WalletMapper {
    WalletResponse toWalletResponse(Wallet wallet);
}
