package com.manhnv.capstonebakerai.loyalty.controller;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/me/loyalty")
@RequiredArgsConstructor
@Tag(name = "CustomerLoyaltyController", description = "Điểm thưởng của Customer đang đăng nhập.")
public class CustomerLoyaltyController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LoyaltyService loyaltyService;
    private final CustomerRepository customerRepository;

    @GetMapping
    @Operation(summary = "Xem số dư điểm", description = """
            Trả số dư điểm hiện tại và giá điểm của từng chức năng AI
            (ingredientImagePrice: chụp ảnh nguyên liệu, recipeVideoPrice: tóm tắt video).
            Customer chưa có giao dịch điểm nào thì balance = 0.
            """)
    public ApiResponse<LoyaltyBalanceResponse> getBalance() {
        return ApiResponse.success(new LoyaltyBalanceResponse(
                loyaltyService.getBalance(currentCustomerId()),
                loyaltyService.priceOf(AiFeature.INGREDIENT_IMAGE),
                loyaltyService.priceOf(AiFeature.RECIPE_VIDEO)
        ));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Xem lịch sử điểm", description = """
            Mỗi lần cộng, trừ, hoàn điểm là một dòng, mới nhất trước. points dương là cộng, âm là trừ;
            balanceAfter là số dư sau giao dịch; note là lý do hiển thị cho khách.
            type: WELCOME, ORDER_EARN, AI_SPEND, AI_REFUND_ERROR, AI_REFUND_PURCHASE.
            orderId có giá trị với ORDER_EARN và AI_REFUND_PURCHASE.
            Phân trang: page mặc định 0, size mặc định 20, tối đa 100.
            """)
    public ApiResponse<Page<LoyaltyTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        return ApiResponse.success(
                loyaltyService.getTransactions(currentCustomerId(), pageable)
                        .map(LoyaltyTransactionResponse::from)
        );
    }

    private UUID currentCustomerId() {
        return customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND))
                .getId();
    }
}
