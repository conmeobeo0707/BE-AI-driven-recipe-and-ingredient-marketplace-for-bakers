package com.manhnv.capstonebakerai.product.controller;

import com.manhnv.capstonebakerai.product.dtos.ProductRequestDTO;
import com.manhnv.capstonebakerai.product.dtos.ProductResponseDTO;
import com.manhnv.capstonebakerai.product.service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductServiceImpl productService;

    // API Thêm mới sản phẩm
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO createdProduct = productService.save (productRequestDTO);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    // API Cập nhật thông tin sản phẩm theo ID
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable UUID id,
            @RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO updatedProduct = productService.update(id, productRequestDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    // API Lấy thông tin chi tiết một sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable UUID id) {
        ProductResponseDTO product = productService.findById(id);
        return ResponseEntity.ok(product);
    }

    // API Lấy danh sách sản phẩm có phân trang
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @PageableDefault(page = 0, size = 10,
                    sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponseDTO> products = productService.findAll(pageable);
        return ResponseEntity.ok(products);
    }
}