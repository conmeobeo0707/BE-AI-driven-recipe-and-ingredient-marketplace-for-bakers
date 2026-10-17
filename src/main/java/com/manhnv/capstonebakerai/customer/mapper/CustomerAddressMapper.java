package com.manhnv.capstonebakerai.customer.mapper;

import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;
import com.manhnv.capstonebakerai.customer.entity.CustomerAddress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerAddressMapper {
    CustomerAddressResponse toResponse(CustomerAddress entity);
}
