package com.manhnv.capstonebakerai.media.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.media.dto.response.MediaResponse;
import com.manhnv.capstonebakerai.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "Upload nhiều ảnh và tạo media IMAGE", description = "Multipart request: files bắt buộc, nhận từ 1 đến 10 ảnh; mỗi ảnh tối đa 5 MB và tổng dung lượng tối đa 25 MB. Hỗ trợ JPEG, PNG và WebP.")
    @PostMapping(
            value = "/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<MediaResponse>>> uploadImages(
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(mediaService.uploadImages(files))
        );
    }

    @Operation(summary = "Upload một video và tạo media VIDEO", description = "Multipart request: file bắt buộc, chỉ nhận một video, tối đa 50 MB. Hỗ trợ MP4, MOV và WebM.")
    @PostMapping(
            value = "/videos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MediaResponse>> uploadVideo(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(mediaService.uploadVideo(file))
        );
    }

    @Operation(summary = "Xóa ảnh theo media ID", description = "Xóa ảnh theo media ID. Chỉ xóa được ảnh có status TEMP hoặc DELETING.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaResponse>> deleteImage(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mediaService.deleteMedia(id)));
    }
}
