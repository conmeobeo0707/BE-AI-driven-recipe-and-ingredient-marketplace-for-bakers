package com.manhnv.capstonebakerai.cart.mapper;

import com.manhnv.capstonebakerai.cart.dto.response.CartItemResponse;
import com.manhnv.capstonebakerai.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "uomId", source = "uom.unitId")
    @Mapping(target = "unitName", source = "uom.unitName")
    @Mapping(target = "unitPrice", source = "uom.sellingPrice")
    @Mapping(target = "subtotal",
    expression = "java(item.getUom().getSellingPrice().multiply(item.getQuantity()))")
    CartItemResponse toItemResponse(CartItem item);
}