package com.manhnv.capstonebakerai.shipment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.order.dto.request.ShippingRatesRequest;
import com.manhnv.capstonebakerai.order.service.OrderService;
import com.manhnv.capstonebakerai.shipment.dto.response.AddressOptionResponse;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.shipment.provider.goship.GoshipClient;
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
public class ShippingController {

    private final GoshipClient goshipClient;
    private final OrderService orderService;

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<AddressOptionResponse>>> cities() {
        return ResponseEntity.ok(ApiResponse.success(goshipClient.cities().stream()
                .map(c -> new AddressOptionResponse(c.id(), c.name())).toList()));
    }

    @GetMapping("/cities/{cityId}/districts")
    public ResponseEntity<ApiResponse<List<AddressOptionResponse>>> districts(@PathVariable String cityId) {
        return ResponseEntity.ok(ApiResponse.success(goshipClient.districts(cityId).stream()
                .map(d -> new AddressOptionResponse(d.id(), d.name())).toList()));
    }

    @GetMapping("/districts/{districtId}/wards")
    public ResponseEntity<ApiResponse<List<AddressOptionResponse>>> wards(@PathVariable String districtId) {
        return ResponseEntity.ok(ApiResponse.success(goshipClient.wards(districtId).stream()
                .map(w -> new AddressOptionResponse(w.id(), w.name())).toList()));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<List<ShippingRateResponse>>> rates(
            @Valid @RequestBody ShippingRatesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.previewShippingRates(request)));
    }
}
