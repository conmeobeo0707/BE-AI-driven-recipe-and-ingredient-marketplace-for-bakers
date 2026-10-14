package com.manhnv.capstonebakerai.warehouse.dto;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
public record StockOutCreateRequest(@NotNull StockOutType type, UUID referenceOrderId,
        @Size(max = 5000) String note, @NotEmpty List<@Valid StockOutItemRequest> items) { }
