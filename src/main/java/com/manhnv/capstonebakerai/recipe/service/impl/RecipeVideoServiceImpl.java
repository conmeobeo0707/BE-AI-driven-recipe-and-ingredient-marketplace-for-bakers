package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.enums.MediaType;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.event.ShotstackRenderVerifiedEvent;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import com.manhnv.capstonebakerai.media.service.ShotstackService;
import com.manhnv.capstonebakerai.recipe.dto.request.CreateRecipeVideoRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeVideoRenderResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeVideoRender;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.helper.RecipeMediaHelper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeVideoRenderRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeVideoService;
import com.manhnv.capstonebakerai.recipe.video.VideoDesign;
import com.manhnv.capstonebakerai.recipe.video.VideoArt;
import com.manhnv.capstonebakerai.recipe.video.VideoSlides;
import com.manhnv.capstonebakerai.recipe.video.VideoTheme;
import com.manhnv.capstonebakerai.recipe.video.VideoThemeSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeVideoServiceImpl implements RecipeVideoService {
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE = "DONE";
    private static final String FAILED = "FAILED";
    private static final int ROWS_PER_SLIDE = 14;
    private static final Pattern TEMPERATURE = Pattern.compile("(\\d{2,3})\\s*(?:°|độ)\\s*C", Pattern.CASE_INSENSITIVE);
    private static final int MAX_NARRATION_CHARS = 2500;

    private final ShotstackService shotstackService;
    private final RecipeVideoRenderRepository renderRepository;
    private final RecipeRepository recipeRepository;
    private final MediaRepository mediaRepository;
    private final R2StorageService storageService;
    private final RecipeMediaHelper mediaHelper;
    private final PlatformTransactionManager transactionManager;
    private final VideoThemeSelector themeSelector;
    private final VideoSlides slides;

    @Value("${app.shotstack.tts-model:elevenlabs-turbo-v2.5}")
    private String ttsModel;
    @Value("${app.shotstack.tts-voice:Sarah}")
    private String ttsVoice;
    @Value("${app.shotstack.tts-language:vi}")
    private String ttsLanguage;

    @Override
    public RecipeVideoRenderResponse createVideo(CreateRecipeVideoRequest request) {
        UUID recipeId = request.getRecipeId();
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        RecipeSnapshot recipe = readOnly.execute(status -> {
            Recipe entity = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
            requireOwnerOrAdmin(entity);
            return snapshot(entity);
        });

        if (renderRepository.existsByRecipeIdAndStatus(recipeId, PROCESSING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A video is already being rendered for this recipe");
        }

        VideoDesign design = themeSelector.select(recipeId, recipe.title(), recipe.category(), recipe.description(),
                recipe.steps().stream().map(StepSnapshot::instruction).toList());
        String renderId = shotstackService.createRender(buildEdit(buildScenes(recipe, design)));
        RecipeVideoRender saved = renderRepository.save(RecipeVideoRender.builder()
                .recipeId(recipeId).renderId(renderId).status(PROCESSING).build());
        return toResponse(saved);
    }

    @Override
    @EventListener
    public void handleRenderResult(ShotstackRenderVerifiedEvent result) {
        RecipeVideoRender render = renderRepository.findByRenderId(result.renderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Render not found"));
        if (!PROCESSING.equals(render.getStatus())) {
            return;
        }
        String status = result.status() == null ? "" : result.status();

        if (status.equalsIgnoreCase("failed")) {
            render.setStatus(FAILED);
            render.setErrorMessage(truncate(result.errorMessage(), 2000));
            render.setCompletedAt(Instant.now());
            renderRepository.save(render);
            return;
        }
        if (!status.equalsIgnoreCase("done") || result.videoUrl() == null) {
            return;
        }

        byte[] video = shotstackService.downloadOutput(result.videoUrl());
        String key = "media/videos/" + UUID.randomUUID() + ".mp4";
        storageService.upload(R2BucketType.PUBLIC, key, video, "video/mp4");
        String storedUrl = storageService.publicUrl(key);

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> attachVideo(render.getId(), storedUrl));
        } catch (RuntimeException e) {
            try {
                storageService.delete(R2BucketType.PUBLIC, key);
            } catch (RuntimeException cleanupError) {
                log.error("Failed to clean up rendered video {} from R2", key, cleanupError);
            }
            throw e;
        }
    }

    private void attachVideo(UUID renderEntityId, String storedUrl) {
        RecipeVideoRender render = renderRepository.findById(renderEntityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Render not found"));
        if (!PROCESSING.equals(render.getStatus())) {
            throw new IllegalStateException("Render " + render.getRenderId() + " was already completed");
        }
        Recipe recipe = recipeRepository.findById(render.getRecipeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        Media previous = recipe.getMedia();
        Media video = mediaRepository.save(Media.builder()
                .type(MediaType.VIDEO)
                .status(MediaStatus.ATTACHED)
                .url(storedUrl)
                .build());
        recipe.setMedia(video);
        recipeRepository.save(recipe);

        if (previous != null) {
            mediaHelper.detachMedia(List.of(previous));
            mediaRepository.save(previous);
            mediaHelper.deleteMediaAfterCommit(List.of(previous.getId()));
        }

        render.setStatus(DONE);
        render.setVideoUrl(storedUrl);
        render.setCompletedAt(Instant.now());
        renderRepository.save(render);
    }

    private void requireOwnerOrAdmin(Recipe recipe) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean owner = recipe.getUser() != null && Objects.equals(recipe.getUser().getUserId(), currentUserId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!owner && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipe owner can create its video");
        }
    }

    private RecipeSnapshot snapshot(Recipe recipe) {
        List<VideoSlides.Row> ingredients = nullToEmpty(recipe.getIngredients()).stream()
                .filter(link -> link.getIngredient() != null)
                .map(link -> new VideoSlides.Row(link.getIngredient().getName(),
                        amount(link.getQuantity(), link.getIngredient().getUnit()).trim()))
                .toList();
        List<VideoSlides.Row> tools = nullToEmpty(recipe.getTools()).stream()
                .filter(link -> link.getTool() != null)
                .map(link -> new VideoSlides.Row(link.getTool().getName(),
                        link.getQuantity() == null ? "" : "×" + link.getQuantity().stripTrailingZeros().toPlainString()))
                .toList();
        List<StepSnapshot> steps = nullToEmpty(recipe.getSteps()).stream()
                .sorted(Comparator.comparing(RecipeStep::getStepNumber, Comparator.nullsLast(Integer::compareTo)))
                .map(step -> {
                    List<String> stepIngredients = nullToEmpty(step.getIngredients()).stream()
                            .filter(link -> link.getIngredient() != null)
                            .map(link -> (link.getIngredient().getName() + " "
                                    + amount(link.getQuantity(), link.getIngredient().getUnit())).trim())
                            .toList();
                    List<String> stepTools = nullToEmpty(step.getTools()).stream()
                            .filter(link -> link.getTool() != null)
                            .map(link -> link.getTool().getName())
                            .toList();
                    return new StepSnapshot(step.getRecipeStepType(), step.getDurationMinutes(),
                            blankToDefault(step.getInstruction(), ""), blankToDefault(step.getNotes(), ""),
                            stepIngredients, stepTools);
                })
                .toList();

        return new RecipeSnapshot(blankToDefault(recipe.getTitle(), "Công thức"),
                recipe.getRecipeCategory() == null ? null : recipe.getRecipeCategory().getName(),
                blankToDefault(recipe.getDescription(), ""), recipe.getPrepTimeMinutes(), recipe.getCookTimeMinutes(),
                recipe.getServings(), recipe.getDifficulty(), ingredients, tools, steps);
    }

    private List<Scene> buildScenes(RecipeSnapshot recipe, VideoDesign design) {
        VideoTheme theme = design.theme();
        List<Scene> scenes = new ArrayList<>();

        StringBuilder intro = new StringBuilder(recipe.title()).append(". ");
        if (recipe.category() != null && !recipe.category().isBlank()) {
            intro.append("Thuộc danh mục ").append(recipe.category()).append(". ");
        }
        if (!recipe.description().isEmpty()) intro.append(endSentence(recipe.description()));
        scenes.add(new Scene(slides.intro(theme, recipe.category(), recipe.title(), recipe.description()), intro.toString()));

        List<String> facts = new ArrayList<>();
        if (recipe.prep() != null) facts.add("thời gian chuẩn bị " + recipe.prep() + " phút");
        if (recipe.cook() != null) facts.add("thời gian nấu " + recipe.cook() + " phút");
        if (recipe.servings() != null) facts.add("dành cho " + recipe.servings() + " người");
        if (recipe.difficulty() != null) facts.add("độ khó " + difficultyLabel(recipe.difficulty()));
        String overviewNarration = facts.isEmpty() ? "Cùng xem tổng quan công thức."
                : "Tổng quan công thức: " + String.join(", ", facts) + ".";
        scenes.add(new Scene(slides.overview(theme, recipe.prep(), recipe.cook(), recipe.servings(),
                recipe.difficulty() == null ? "–" : capitalize(difficultyLabel(recipe.difficulty())),
                difficultyLevel(recipe.difficulty())), overviewNarration));

        addListScenes(scenes, theme, "DỤNG CỤ", "Dụng cụ", null, recipe.tools(),
                "Dụng cụ cần chuẩn bị gồm có: ", false);
        addListScenes(scenes, theme, "NGUYÊN LIỆU", "Nguyên liệu",
                recipe.servings() == null ? null : "cho " + recipe.servings() + " phần", recipe.ingredients(),
                "Nguyên liệu gồm có: ", true);

        List<StepSnapshot> steps = recipe.steps();
        for (int i = 0; i < steps.size(); i++) {
            String icon = i < design.stepIcons().size() ? design.stepIcons().get(i) : "bowl";
            scenes.add(stepScene(theme, steps.get(i), i + 1, steps.size(), icon));
        }

        scenes.add(new Scene(slides.outro(theme, recipe.title()),
                "Món " + recipe.title() + " đã hoàn thành. Chúc bạn ngon miệng và hẹn gặp lại!"));
        return scenes;
    }

    private void addListScenes(List<Scene> scenes, VideoTheme theme, String kicker, String title, String subtitle,
                               List<VideoSlides.Row> rows, String narrationPrefix, boolean ingredients) {
        for (int start = 0; start < rows.size(); start += ROWS_PER_SLIDE) {
            List<VideoSlides.Row> group = rows.subList(start, Math.min(rows.size(), start + ROWS_PER_SLIDE));
            boolean wide = group.size() <= 7;
            String art = ingredients ? VideoArt.hero(theme.decoration(), wide ? 640 : 480)
                    : VideoArt.icon("bowl", wide ? 640 : 480);
            String narration = (start == 0 ? narrationPrefix : "Tiếp theo: ") + String.join(", ", group.stream()
                    .map(row -> (row.quantity().startsWith("×") ? row.quantity().substring(1) + " " + row.name()
                            : row.quantity() + " " + row.name()).trim())
                    .toList()) + ".";
            scenes.add(new Scene(slides.list(theme, kicker, start == 0 ? title : title + " (tiếp)", subtitle, group, art),
                    narration));
        }
    }

    private Scene stepScene(VideoTheme theme, StepSnapshot step, int index, int total, String icon) {
        String type = step.type() == RecipeStepType.PREPARATION ? "CHUẨN BỊ" : "NẤU";
        StringBuilder narration = new StringBuilder("Bước ").append(index).append(", ").append(type.toLowerCase());
        List<String> badges = new ArrayList<>();
        if (step.duration() != null && step.duration() > 0) {
            badges.add(step.duration() + " phút");
            narration.append(", khoảng ").append(step.duration()).append(" phút");
        }
        Matcher temperature = TEMPERATURE.matcher(step.instruction());
        if (temperature.find()) badges.add(temperature.group(1) + "°C");
        narration.append(". ");
        if (!step.instruction().isEmpty()) narration.append(endSentence(step.instruction())).append(' ');
        if (!step.ingredients().isEmpty()) narration.append("Nguyên liệu: ").append(String.join(", ", step.ingredients())).append(". ");
        if (!step.tools().isEmpty()) narration.append("Dụng cụ: ").append(String.join(", ", step.tools())).append(". ");
        if (!step.notes().isEmpty()) narration.append("Lưu ý: ").append(endSentence(step.notes()));

        return new Scene(slides.step(theme, "BƯỚC " + index + " · " + type, index, total, step.instruction(),
                step.ingredients(), step.tools(), badges, step.notes(), icon), narration.toString().trim());
    }

    private Map<String, Object> buildEdit(List<Scene> scenes) {
        List<Map<String, Object>> slideClips = new ArrayList<>();
        List<Map<String, Object>> voiceClips = new ArrayList<>();

        for (int i = 0; i < scenes.size(); i++) {
            Scene scene = scenes.get(i);
            String alias = "scene-" + i;
            String timing = "alias://" + alias;

            Map<String, Object> voice = new LinkedHashMap<>();
            voice.put("asset", Map.of(
                    "type", "audio",
                    "prompt", truncate(scene.narration(), MAX_NARRATION_CHARS),
                    "model", ttsModel,
                    "options", Map.of("voice", ttsVoice, "language", ttsLanguage)));
            voice.put("alias", alias);
            voice.put("start", i == 0 ? 0 : "auto");
            voice.put("length", "auto");
            voiceClips.add(voice);

            Map<String, Object> slide = new LinkedHashMap<>();
            slide.put("asset", scene.asset());
            slide.put("start", timing);
            slide.put("length", timing);
            slide.put("width", VideoSlides.WIDTH);
            slide.put("height", VideoSlides.HEIGHT);
            slideClips.add(slide);
        }

        Map<String, Object> timeline = new LinkedHashMap<>();
        timeline.put("background", "#FFFFFF");
        timeline.put("tracks", List.of(Map.of("clips", slideClips), Map.of("clips", voiceClips)));

        Map<String, Object> edit = new LinkedHashMap<>();
        edit.put("timeline", timeline);
        edit.put("output", Map.of("format", "mp4", "resolution", "1080", "aspectRatio", "16:9"));
        return edit;
    }

    private static String amount(BigDecimal quantity, String unit) {
        StringBuilder value = new StringBuilder();
        if (quantity != null) value.append(quantity.stripTrailingZeros().toPlainString()).append(' ');
        if (unit != null && !unit.isBlank()) value.append(unit.trim()).append(' ');
        return value.toString();
    }

    private static String difficultyLabel(DifficultyEnum difficulty) {
        return switch (difficulty) {
            case EASY -> "dễ";
            case MEDIUM -> "trung bình";
            case HARD -> "khó";
        };
    }

    private static int difficultyLevel(DifficultyEnum difficulty) {
        if (difficulty == null) return 0;
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String endSentence(String text) {
        String trimmed = text.trim();
        return trimmed.matches(".*[.!?…]$") ? trimmed : trimmed + ".";
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private RecipeVideoRenderResponse toResponse(RecipeVideoRender render) {
        return RecipeVideoRenderResponse.builder().id(render.getId()).recipeId(render.getRecipeId())
                .renderId(render.getRenderId()).status(render.getStatus()).videoUrl(render.getVideoUrl()).build();
    }

    private record RecipeSnapshot(String title, String category, String description, Integer prep, Integer cook,
                                  Integer servings, DifficultyEnum difficulty, List<VideoSlides.Row> ingredients,
                                  List<VideoSlides.Row> tools, List<StepSnapshot> steps) { }

    private record StepSnapshot(RecipeStepType type, Integer duration, String instruction, String notes,
                                List<String> ingredients, List<String> tools) { }

    private record Scene(Map<String, Object> asset, String narration) { }
}
