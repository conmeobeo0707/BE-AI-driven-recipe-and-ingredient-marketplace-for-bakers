package com.manhnv.capstonebakerai.report.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.report.dto.response.FinancialReportResponse;
import com.manhnv.capstonebakerai.report.service.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/staff/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Báo cáo tài chính")
public class FinancialReportController {

    private final FinancialReportService service;

    @Operation(
            summary = "API báo cáo kết quả hoạt động kinh doanh theo tháng",
            description = """
                    Trả về báo cáo tài chính từng tháng trong khoảng from → to (tính cả hai tháng đầu và cuối), \
                    kèm một dòng TOTAL tổng hợp cả kỳ. Tháng không phát sinh dữ liệu vẫn được trả về với giá trị 0. \
                    Tối đa 36 tháng mỗi lần gọi, from không được sau to.

                    Các chỉ tiêu trong mỗi tháng:
                    - grossSales (1): tổng tiền hàng của các đơn đã giao (DELIVERED), tính theo ngày giao hàng.
                    - discounts (2): giảm giá thủ công của nhân viên + giảm giá voucher.
                    - netRevenue (3) = (1) - (2): doanh thu thuần, không gồm phí vận chuyển.
                    - cogs (4): giá vốn hàng bán, lấy từ phiếu xuất kho SALE đã hoàn tất của các đơn đã giao, \
                    tính theo giá nhập của đúng các lô đã xuất (FEFO).
                    - grossProfit (5) = (3) - (4): lợi nhuận gộp.
                    - operatingExpenses (6): tổng phiếu chi đang hiệu lực (ACTIVE), phiếu đã hủy không tính.
                    - stocktakeLoss (7): giá trị hàng hụt qua các phiếu kiểm kho đã hoàn tất \
                    (chỉ tính các lô bị hụt, phần thừa không được cộng lại).
                    - disposalLoss (8): giá trị hàng hủy qua phiếu xuất kho DISPOSAL đã hoàn tất.
                    - totalShrinkage (9) = (7) + (8): tổng hao hụt.
                    - netProfit (10) = (5) - (6) - (9): lợi nhuận thuần.

                    Mốc thời gian: đơn hàng và giá vốn theo ngày giao hàng; phiếu chi theo ngày tạo; \
                    kiểm kho và hủy hàng theo ngày hoàn tất phiếu."""
    )
    @GetMapping("/financial")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> monthly(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return ResponseEntity.ok(ApiResponse.success(service.getMonthlyReport(from, to)));
    }

}
