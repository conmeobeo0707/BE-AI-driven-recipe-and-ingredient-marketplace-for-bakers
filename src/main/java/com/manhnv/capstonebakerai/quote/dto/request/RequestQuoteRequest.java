package com.manhnv.capstonebakerai.quote.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequestQuoteRequest(
        @NotBlank @Size(max = 20) String contactPhone,
        @Size(max = 150) String contactName,
        @Size(max = 1000) String content,
        @NotEmpty @Valid List<RequestQuoteItemRequest> items
        ) {
}
