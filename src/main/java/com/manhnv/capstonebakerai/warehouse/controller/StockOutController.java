package com.manhnv.capstonebakerai.warehouse.controller;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockOutCreateRequest;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import com.manhnv.capstonebakerai.warehouse.dto.StockOutDetailResponse;
import com.manhnv.capstonebakerai.warehouse.service.StockOutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import java.util.UUID;
@RestController @RequestMapping("/api/warehouse/stock-out")
public class StockOutController {
 private final StockOutService service; public StockOutController(StockOutService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<StockOutNote> create(@Valid @RequestBody StockOutCreateRequest request){return ApiResponse.success(service.create(request));}
 @PostMapping("/{id}/complete") public ApiResponse<StockOutNote> complete(@PathVariable UUID id){return ApiResponse.success(service.complete(id));}
 @PostMapping("/{id}/cancel") public ApiResponse<StockOutNote> cancel(@PathVariable UUID id){return ApiResponse.success(service.cancel(id));}
 @GetMapping public ApiResponse<Page<StockOutNote>> list(@RequestParam(required=false) NoteStatus status,
         @RequestParam(required=false) StockOutType type,
         @PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){return ApiResponse.success(service.list(status,type,pageable));}
 @GetMapping("/{id}") public ApiResponse<StockOutDetailResponse> get(@PathVariable UUID id){return ApiResponse.success(service.get(id));}
}
