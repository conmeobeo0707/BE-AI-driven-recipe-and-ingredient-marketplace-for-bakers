package com.manhnv.capstonebakerai.warehouse.controller;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockInCreateRequest;
import com.manhnv.capstonebakerai.warehouse.entity.StockInNote;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.dto.StockInDetailResponse;
import com.manhnv.capstonebakerai.warehouse.service.StockInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import java.util.UUID;
@RestController @RequestMapping("/api/warehouse/stock-in")
public class StockInController {
 private final StockInService service; public StockInController(StockInService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<StockInNote> create(@Valid @RequestBody StockInCreateRequest request){return ApiResponse.success(service.create(request));}
 @PostMapping("/{id}/complete") public ApiResponse<StockInNote> complete(@PathVariable UUID id){return ApiResponse.success(service.complete(id));}
 @PostMapping("/{id}/cancel") public ApiResponse<StockInNote> cancel(@PathVariable UUID id){return ApiResponse.success(service.cancel(id));}
 @GetMapping public ApiResponse<Page<StockInNote>> list(@RequestParam(required=false) NoteStatus status,
         @PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){return ApiResponse.success(service.list(status,pageable));}
 @GetMapping("/{id}") public ApiResponse<StockInDetailResponse> get(@PathVariable UUID id){return ApiResponse.success(service.get(id));}
}
