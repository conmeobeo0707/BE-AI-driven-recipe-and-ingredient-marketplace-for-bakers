package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientImageRecognitionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IngredientImageService {

    IngredientImageRecognitionResponse recognize(MultipartFile file);
}
