package com.manhnv.capstonebakerai.quote.dto.request;

import jakarta.validation.constraints.Size;

public record MarkOrderedRequest(
        @Size(max = 500) String resultNote
) {
}
