package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class IngredientNormalizer {

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        //bỏ dấu tiếng Việt, chuyển sang chữ thường, thay các ký tự đặc biệt bằng dấu cách, xóa khoảng trắng thừa
        String noDiacritics = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');

        return noDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
