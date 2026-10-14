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
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<StocktakeDetailResponse> create(@Valid @RequestBody StocktakeCreateRequest request){return ApiResponse.success(service.create(request));}
 @PutMapping("/{noteId}/counts") public ApiResponse<StocktakeDetailResponse> count(@PathVariable UUID noteId,@Valid @RequestBody StocktakeCountRequest request){return ApiResponse.success(service.count(noteId,request));}
 @PostMapping("/{noteId}/complete") public ApiResponse<StocktakeDetailResponse> complete(@PathVariable UUID noteId){return ApiResponse.success(service.complete(noteId));}
 @PostMapping("/{noteId}/cancel") public ApiResponse<StocktakeNote> cancel(@PathVariable UUID noteId){return ApiResponse.success(service.cancel(noteId));}
 @GetMapping public ApiResponse<Page<StocktakeNote>> list(@RequestParam(required=false) StocktakeStatus status,
         @PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){return ApiResponse.success(service.list(status,pageable));}
 @GetMapping("/{noteId}") public ApiResponse<StocktakeDetailResponse> get(@PathVariable UUID noteId){return ApiResponse.success(service.get(noteId));}
}
