package com.manhnv.capstonebakerai.product.mappers;

import com.manhnv.capstonebakerai.product.dtos.ProductRequestDTO;
import com.manhnv.capstonebakerai.product.dtos.ProductResponseDTO;
import com.manhnv.capstonebakerai.product.entities.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toProduct(ProductRequestDTO productRequestDTO);
    ProductResponseDTO toProductResponseDto(Product product);
}
