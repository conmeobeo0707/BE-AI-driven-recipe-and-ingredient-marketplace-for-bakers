package com.manhnv.capstonebakerai.receipt.mapper;

import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptCategoryResponse;
import com.manhnv.capstonebakerai.receipt.entity.ReceiptCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReceiptCategoryMapper {
    ReceiptCategoryResponse toResponse(ReceiptCategory entity);
}
