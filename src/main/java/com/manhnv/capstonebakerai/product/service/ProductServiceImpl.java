package com.manhnv.capstonebakerai.product.service;

import com.manhnv.capstonebakerai.product.dtos.ProductRequestDTO;
import com.manhnv.capstonebakerai.product.dtos.ProductResponseDTO;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.mappers.ProductMapper;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public ProductResponseDTO save(ProductRequestDTO productRequestDTO) {
        Product product = productMapper.toProduct(productRequestDTO);

        if (product.getCreatedAt() == null) {
            product.setCreatedAt(java.time.LocalDate.now());
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponseDto(savedProduct);
    }

    @Override
    public ProductResponseDTO update(UUID id, ProductRequestDTO productRequestDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        existingProduct.setName(productRequestDTO.getName());
        existingProduct.setDescription(productRequestDTO.getDescription());
        existingProduct.setImgUrl(productRequestDTO.getImgUrl());
        existingProduct.setPrice(productRequestDTO.getPrice());
        existingProduct.setQuantity(productRequestDTO.getQuantity());
        existingProduct.setStatus(productRequestDTO.getStatus());
        existingProduct.setStoreId(productRequestDTO.getStoreId());

        Product updatedProduct = productRepository.save(existingProduct);
        return productMapper.toProductResponseDto(updatedProduct);
    }

    @Override
    public ProductResponseDTO findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return productMapper.toProductResponseDto(product);
    }

    @Override
    public Page<ProductResponseDTO> findAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toProductResponseDto);
    }

}
