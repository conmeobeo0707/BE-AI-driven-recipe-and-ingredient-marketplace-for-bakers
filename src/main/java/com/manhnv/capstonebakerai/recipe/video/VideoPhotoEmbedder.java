package com.manhnv.capstonebakerai.recipe.video;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VideoPhotoEmbedder {

    public static final int RENDER_PHOTO_BUDGET = 24;
    private static final int MAX_PHOTOS_PER_SLIDE = 3;

    public static final class Budget {
        private int remaining;

        public Budget(int count) {
            this.remaining = count;
        }
    }

    public List<String> embed(List<String> urls, int width, int height, Budget budget) {
        if (urls == null || urls.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        synchronized (budget) {
            for (String url : urls) {
                if (result.size() >= MAX_PHOTOS_PER_SLIDE || budget.remaining <= 0) break;
                if (url == null || !url.startsWith("https://")) continue;
                result.add(url);
                budget.remaining--;
            }
        }
        return result;
    }
}
