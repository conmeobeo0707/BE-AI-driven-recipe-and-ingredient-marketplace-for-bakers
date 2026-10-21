package com.manhnv.capstonebakerai.customer.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Thay toàn bộ hobby đã chọn của Customer.")
public record UpdateCustomerHobbiesRequest(
        @ArraySchema(arraySchema = @Schema(description = "UUID các hobby cần chọn. Mảng rỗng xóa toàn bộ lựa chọn; mọi ID phải thuộc hobby active.", example = "[\"d2719c43-6b3a-48e6-a429-43ac46f468af\"]"), schema = @Schema(type = "string", format = "uuid"))
        @NotNull
        Set<UUID> hobbyIds
) {
}
