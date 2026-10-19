package com.manhnv.capstonebakerai.notification.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.notification.dto.response.MarkAllReadResponse;
import com.manhnv.capstonebakerai.notification.dto.response.NotificationResponse;
import com.manhnv.capstonebakerai.notification.dto.response.UnreadCountResponse;
import com.manhnv.capstonebakerai.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Thông báo", description = "Các API quản lý thông báo")
public class NotificationController {

    private final NotificationService service;

    @Operation(summary = "Lấy danh sách thông báo bởi userId", description = "Thuộc tính read kiểu boolean là trạng thái đã đọc hoặc chưa đọc(cho phép null), có phân trang(mặc định page = 0, size = 10")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAll(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.getAll(userId, read, pageable)));
    }

    @Operation(summary = "Lấy số lượng thông báo chưa đọc bởi userId")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> countUnread() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.countUnread(userId)));
    }

    @Operation(summary = "Lấy chi tiết thông báo bởi userId")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.getById(userId, id)));
    }

    @Operation(summary = "Đánh dấu 1 thông báo đã đọc")
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.markAsRead(userId, id)));
    }

    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.markAllAsRead(userId)));
    }
}
