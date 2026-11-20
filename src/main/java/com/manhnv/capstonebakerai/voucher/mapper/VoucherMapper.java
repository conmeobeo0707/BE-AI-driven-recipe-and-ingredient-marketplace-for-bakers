package com.manhnv.capstonebakerai.voucher.mapper;

import com.manhnv.capstonebakerai.voucher.dto.response.VoucherResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherSummaryResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherUsageResponse;
import com.manhnv.capstonebakerai.voucher.entity.Voucher;
import com.manhnv.capstonebakerai.voucher.entity.VoucherUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    @Mapping(target = "displayStatus", expression = "java(voucher.displayStatus(java.time.LocalDateTime.now()))")
    @Mapping(target = "remainingQuantity", expression = "java(voucher.remainingQuantity())")
    VoucherResponse toResponse(Voucher voucher);

    @Mapping(target = "remainingQuantity", expression = "java(voucher.remainingQuantity())")
    VoucherSummaryResponse toSummary(Voucher voucher);

    @Mapping(source = "voucher.id", target = "voucherId")
    VoucherUsageResponse toUsageResponse(VoucherUsage usage);
}
