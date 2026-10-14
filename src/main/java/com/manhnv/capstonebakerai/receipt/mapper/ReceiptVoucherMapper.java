package com.manhnv.capstonebakerai.receipt.mapper;

import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptVoucherResponse;
import com.manhnv.capstonebakerai.receipt.entity.ReceiptVoucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReceiptVoucherMapper {
    @Mapping(source = "receiptCategory.id", target = "receiptCategoryId")
    @Mapping(source = "receiptCategory.name", target = "receiptCategoryName")
    @Mapping(source = "receiptCategory.type", target = "receiptCategoryType")
    ReceiptVoucherResponse toResponse(ReceiptVoucher entity);
}
