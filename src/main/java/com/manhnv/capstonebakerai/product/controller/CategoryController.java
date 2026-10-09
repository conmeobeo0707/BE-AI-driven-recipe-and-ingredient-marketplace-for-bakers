package com.manhnv.capstonebakerai.product.controller;

import com.manhnv.capstonebakerai.product.dtos.CategoryRequest;
import com.manhnv.capstonebakerai.product.dtos.CategoryResponse;
import com.manhnv.capstonebakerai.product.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/product-categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@RequestBody CategoryRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @RequestBody CategoryRequest request) { return service.update(id, request); }
    @GetMapping public List<CategoryResponse> list() { return service.list(); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
