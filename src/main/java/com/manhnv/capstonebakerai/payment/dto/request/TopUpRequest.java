package com.manhnv.capstonebakerai.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class TopUpRequest {

    @NotNull
    @DecimalMin(value = "10000", message = "Minimum top-up amount is 10,000 VND")
    private BigDecimal amount;
}
