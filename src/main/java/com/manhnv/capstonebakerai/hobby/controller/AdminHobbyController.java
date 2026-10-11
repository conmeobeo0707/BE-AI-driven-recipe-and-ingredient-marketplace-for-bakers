package com.manhnv.capstonebakerai.hobby.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.hobby.dto.SaveHobbyRequest;
import com.manhnv.capstonebakerai.hobby.dto.UpdateHobbyActiveRequest;
import com.manhnv.capstonebakerai.hobby.service.HobbyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/hobbies")
@RequiredArgsConstructor
public class AdminHobbyController {

    private final HobbyService hobbyService;

    @GetMapping
    public ApiResponse<List<HobbyResponse>> getAll() {
        return ApiResponse.success(hobbyService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<HobbyResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(hobbyService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HobbyResponse> create(@Valid @RequestBody SaveHobbyRequest request) {
        return ApiResponse.success(hobbyService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<HobbyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveHobbyRequest request
    ) {
        return ApiResponse.success(hobbyService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<HobbyResponse> setActive(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHobbyActiveRequest request
    ) {
        return ApiResponse.success(hobbyService.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<HobbyResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.success(hobbyService.setActive(id, false));
    }
}
