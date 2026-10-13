package com.manhnv.capstonebakerai.recipe.dto.response;

import com.manhnv.capstonebakerai.recipe.enums.MediaStatus;
import com.manhnv.capstonebakerai.recipe.enums.MediaType;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private UUID id;
    private String url;
    private MediaType type;
    private MediaStatus status;
}
