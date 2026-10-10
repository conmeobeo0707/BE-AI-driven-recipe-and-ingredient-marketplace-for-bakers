package com.manhnv.capstonebakerai.hobby.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.hobby.service.HobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyController {

    private final HobbyService hobbyService;

    @GetMapping
    public ApiResponse<List<HobbyResponse>> getActiveHobbies() {
        return ApiResponse.success(hobbyService.getActiveHobbies());
    }
}
