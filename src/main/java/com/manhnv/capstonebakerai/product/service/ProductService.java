package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.product.dtos.ProductRequestDTO;
import com.manhnv.capstonebakerai.product.dtos.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

interface ProductService {
    ProductResponseDTO save(ProductRequestDTO productRequestDTO);
    ProductResponseDTO update(UUID id, ProductRequestDTO productRequestDTO);
    ProductResponseDTO findById(UUID id);
    Page<ProductResponseDTO> findAll(Pageable pageable);
}
