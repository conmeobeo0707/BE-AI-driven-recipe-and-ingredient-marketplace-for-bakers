package com.manhnv.capstonebakerai.shipment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.order.dto.request.ShippingRatesRequest;
import com.manhnv.capstonebakerai.order.service.OrderService;
import com.manhnv.capstonebakerai.shipment.dto.response.AddressOptionResponse;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'STAFF')")
@Tag(name = "Vận chuyển")
public class ShippingController {

    private final GoshipClient goshipClient;
    private final OrderService orderService;

    @Operation(summary = "API lấy thông tin các thành phố trong nước cho customer chọn")
    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<AddressOptionResponse>>> cities() {
        return ResponseEntity.ok(ApiResponse.success(goshipClient.cities().stream()
                .map(c -> new AddressOptionResponse(c.id(), c.name())).toList()));
    }

    @Operation(summary = "API lấy thông tin các quận trong thành phố cho customer chọn")
    @GetMapping("/cities/{cityId}/districts")
    public ResponseEntity<ApiResponse<List<AddressOptionResponse>>> districts(@PathVariable String cityId) {
        return ResponseEntity.ok(ApiResponse.success(goshipClient.districts(cityId).stream()
                .map(d -> new AddressOptionResponse(d.id(), d.name())).toList()));
    }

    @Operation(summary = "API lấy thông tin các phường trong quận cho customer chọn")
    @GetMapping("/districts/{districtId}/wards")
    public ResponseEntity<ApiResponse<List<AddressOptionResponse>>> wards(@PathVariable String districtId) {
        return ResponseEntity.ok(ApiResponse.success(goshipClient.wards(districtId).stream()
                .map(w -> new AddressOptionResponse(w.id(), w.name())).toList()));
    }


    @Operation(summary = "API gửi thông tin vận chuyển để lấy các đơn vị vận chuyển và giá cho customer chọn", description = "Dùng RateId này để tạo được đơn hàng -> sau khi order được CONFIRMED thì hệ thống tự tạo vận đơn với đơn vị đó.")
    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<List<ShippingRateResponse>>> rates(
            @Valid @RequestBody ShippingRatesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.previewShippingRates(request)));
    }
}
