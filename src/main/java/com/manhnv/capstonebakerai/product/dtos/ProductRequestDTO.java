package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDTO {
    UUID productId;

    String name;

    String description;

    String imgUrl;

    String price;

    int quantity;

    ProductStatus status;

    LocalDate createdAt;

    UUID storeId;
}
