package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.response.MediaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaResponse uploadImage(MultipartFile file);
    MediaResponse uploadVideo(MultipartFile file);
}
