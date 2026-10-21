package com.manhnv.capstonebakerai.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envelope chung cho API response. Thành công có success=true và data; lỗi có success=false và message.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Schema(description = "Thông báo lỗi; thường không có trong response thành công", nullable = true)
    private String message;

    @Schema(description = "Cho biết request thành công hay thất bại", example = "true")
    private boolean success;

    @Schema(description = "Dữ liệu kết quả của API; kiểu dữ liệu phụ thuộc endpoint", nullable = true)
    private T data;

    public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data(data)
                    .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
