package com.manhnv.capstonebakerai.loyalty.controller;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.loyalty.dto.LoyaltyBalanceResponse;
import com.manhnv.capstonebakerai.loyalty.dto.LoyaltyTransactionResponse;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import com.manhnv.capstonebakerai.loyalty.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers/{customerId}/loyalty")
@RequiredArgsConstructor
@Tag(name = "AdminLoyaltyController", description = "ADMIN xem điểm thưởng của một Customer. Chỉ đọc.")
public class AdminLoyaltyController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LoyaltyService loyaltyService;
    private final CustomerRepository customerRepository;

    @GetMapping
    @Operation(summary = "Xem số dư điểm của một Customer",
            description = "customerId là id của Customer (không phải userId). Không tìm thấy trả 404.")
    public ApiResponse<LoyaltyBalanceResponse> getBalance(@PathVariable UUID customerId) {
        requireCustomer(customerId);
        return ApiResponse.success(new LoyaltyBalanceResponse(
                loyaltyService.getBalance(customerId),
                loyaltyService.priceOf(AiFeature.INGREDIENT_IMAGE),
                loyaltyService.priceOf(AiFeature.RECIPE_VIDEO)
        ));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Xem lịch sử điểm của một Customer",
            description = "Cùng định dạng với API lịch sử điểm của Customer. page mặc định 0, size mặc định 20, tối đa 100.")
    public ApiResponse<Page<LoyaltyTransactionResponse>> getTransactions(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireCustomer(customerId);
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        return ApiResponse.success(
                loyaltyService.getTransactions(customerId, pageable)
                        .map(LoyaltyTransactionResponse::from)
        );
    }

    private void requireCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND);
        }
    }
}
