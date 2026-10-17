package com.manhnv.capstonebakerai.shipment.mapper;

import com.manhnv.capstonebakerai.shipment.dto.response.ShipmentResponse;
import com.manhnv.capstonebakerai.shipment.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {
    @Mapping(source = "order.id", target = "orderId")
    ShipmentResponse toResponse(Shipment shipment);
}
