package com.manhnv.capstonebakerai.media.dto.response;

import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.enums.MediaType;
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
