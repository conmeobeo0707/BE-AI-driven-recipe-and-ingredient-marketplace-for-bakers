package com.manhnv.capstonebakerai.media.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShotstackWebhookRequest {
    private String type;
    private String action;
    private String id;
    private String status;
    private String url;
    private String error;
}
