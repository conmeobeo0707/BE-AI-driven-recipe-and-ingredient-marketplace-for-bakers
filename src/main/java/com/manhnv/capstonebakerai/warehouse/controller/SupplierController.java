package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierRequest;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierResponse;
import com.manhnv.capstonebakerai.warehouse.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService service;
    public SupplierController(SupplierService service){this.service=service;}

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest request){return ApiResponse.success(service.create(request));}
    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(@PathVariable UUID id,@Valid @RequestBody SupplierRequest request){return ApiResponse.success(service.update(id,request));}
    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> get(@PathVariable UUID id){return ApiResponse.success(service.get(id));}
    @GetMapping
    public ApiResponse<Page<SupplierResponse>> list(@RequestParam(required=false) String keyword,
            @PageableDefault(size=20,sort="supplierName",direction=Sort.Direction.ASC) Pageable pageable){return ApiResponse.success(service.list(keyword,pageable));}
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id){service.delete(id);return ApiResponse.success(null);}
}
