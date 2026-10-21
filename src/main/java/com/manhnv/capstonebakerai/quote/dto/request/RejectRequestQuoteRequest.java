package com.manhnv.capstonebakerai.quote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequestQuoteRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
