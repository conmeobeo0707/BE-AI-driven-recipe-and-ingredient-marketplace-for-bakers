package com.manhnv.capstonebakerai.warehouse.controller;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.*;
import com.manhnv.capstonebakerai.warehouse.entity.*;
import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import com.manhnv.capstonebakerai.warehouse.service.StocktakeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import java.util.*;
@RestController @RequestMapping("/api/warehouse/stocktakes")
public class StocktakeController {
 private final StocktakeService service;public StocktakeController(StocktakeService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<StocktakeNote> create(@Valid @RequestBody StocktakeCreateRequest r){return ApiResponse.success(service.create(r));}
 @PutMapping("/{id}/counts") public ApiResponse<List<StocktakeItem>> count(@PathVariable UUID id,@Valid @RequestBody StocktakeCountRequest r){return ApiResponse.success(service.count(id,r));}
 @PostMapping("/{id}/complete") public ApiResponse<StocktakeNote> complete(@PathVariable UUID id){return ApiResponse.success(service.complete(id));}
 @PostMapping("/{id}/cancel") public ApiResponse<StocktakeNote> cancel(@PathVariable UUID id){return ApiResponse.success(service.cancel(id));}
 @GetMapping public ApiResponse<Page<StocktakeNote>> list(@RequestParam(required=false) StocktakeStatus status,
         @PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){return ApiResponse.success(service.list(status,pageable));}
 @GetMapping("/{id}") public ApiResponse<StocktakeDetailResponse> get(@PathVariable UUID id){return ApiResponse.success(service.get(id));}
}
