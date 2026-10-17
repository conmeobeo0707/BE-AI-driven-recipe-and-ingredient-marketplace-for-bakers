package com.manhnv.capstonebakerai.media.service;

import com.manhnv.capstonebakerai.media.dto.response.MediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {
    List<MediaResponse> uploadImages(List<MultipartFile> files);

    MediaResponse uploadVideo(MultipartFile file);

    MediaResponse deleteMedia(UUID id);


}
