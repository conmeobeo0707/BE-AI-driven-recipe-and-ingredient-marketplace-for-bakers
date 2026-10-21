package com.manhnv.capstonebakerai.quote.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.quote.dto.request.MarkOrderedRequest;
import com.manhnv.capstonebakerai.quote.dto.request.RejectRequestQuoteRequest;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteResponse;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteSummaryResponse;
import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import com.manhnv.capstonebakerai.quote.service.RequestQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/request-quotes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','MANAGER')")
@Tag(name = "Yêu cầu đặt sỉ (staff/manager)")
public class StaffRequestQuoteController {

    private final RequestQuoteService service;

    @Operation(summary = "API lấy danh sách yêu cầu đặt sỉ", description = "Lọc theo status (cho phép null), sort theo createdAt")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RequestQuoteSummaryResponse>>> list(
            @RequestParam(required = false) RequestQuoteStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(service.getAll(status, pageable)));
    }

    @Operation(summary = "API lấy chi tiết yêu cầu đặt sỉ")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getForStaff(id)));
    }

    @Operation(summary = "API nhận xử lý yêu cầu", description = "PENDING sang IN_PROGRESS. Nếu đã có người nhận trả 409")
    @PatchMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> claim(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.claim(id, SecurityUtils.getCurrentUserId())));
    }

    @Operation(summary = "API đánh dấu đã tạo đơn", description = "IN_PROGRESS sang ORDERED. resultNote cho phép null")
    @PatchMapping("/{id}/ordered")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> ordered(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) MarkOrderedRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.markOrdered(id, SecurityUtils.getCurrentUserId(), isManager(), request)));
    }

    @Operation(summary = "API từ chối yêu cầu", description = "Bắt buộc có lý do")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> reject(
            @PathVariable UUID id, @Valid @RequestBody RejectRequestQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.reject(id, SecurityUtils.getCurrentUserId(), isManager(), request)));
    }

    private boolean isManager() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }
}
