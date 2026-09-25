package com.manhnv.capstonebakerai.product.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApiResponseDTO {
    private int status;
    private Object data;
    String timestamp;

    public ApiResponseDTO(int status, Object data){
        this.status=status;
        this.data=data;
        this.timestamp= ZonedDateTime.now(ZoneId.of("UTC")).
                format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

}