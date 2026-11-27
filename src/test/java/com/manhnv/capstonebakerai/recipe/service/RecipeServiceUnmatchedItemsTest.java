package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.exception.RecipeErrorCode;
import com.manhnv.capstonebakerai.recipe.helper.RecipeMediaHelper;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepDurationHelper;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepResourceHelper;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientAliasService;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeMapper;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeImageRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepImageRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.service.impl.RecipeServiceImpl;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * POST/PUT /recipes with rows that are not matched to the catalog (ingredientId/toolId = null, rawName + ref).
 */
class RecipeServiceUnmatchedItemsTest {

    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final ToolRepository toolRepository = mock(ToolRepository.class);
    private final RecipeIngredientRepository recipeIngredientRepository = mock(RecipeIngredientRepository.class);
    private final RecipeToolRepository recipeToolRepository = mock(RecipeToolRepository.class);
    private final RecipeStepRepository recipeStepRepository = mock(RecipeStepRepository.class);
    private final RecipeStepIngredientRepository stepIngredientRepository = mock(RecipeStepIngredientRepository.class);
    private final RecipeStepToolRepository stepToolRepository = mock(RecipeStepToolRepository.class);
    private final IngredientAliasService aliasService = mock(IngredientAliasService.class);

    private final UUID userId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();
    private final Ingredient oreo = Ingredient.builder().id(UUID.randomUUID()).name("Bánh Oreo").build();
    private final User user = User.builder().userId(userId).build();

    /** Rows "persisted" by the service in the current call, used to build what findById returns afterwards. */
    private final List<RecipeIngredient> savedIngredients = new ArrayList<>();
    private final List<RecipeTool> savedTools = new ArrayList<>();
    private final List<RecipeStep> savedSteps = new ArrayList<>();

    private RecipeServiceImpl service;
    private Recipe stored;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        RecipeStepResourceHelper stepHelper = new RecipeStepResourceHelper(stepIngredientRepository, stepToolRepository,
                recipeIngredientRepository, recipeToolRepository);
        service = new RecipeServiceImpl(recipeRepository, userRepository, mock(RecipeCategoryRepository.class),
                ingredientRepository, aliasService, toolRepository, recipeIngredientRepository, recipeToolRepository,
                recipeStepRepository, mock(RecipeImageRepository.class), mock(RecipeStepImageRepository.class),
                mock(RecipeMediaHelper.class), stepHelper, new RecipeStepDurationHelper(), mock(UserSummaryService.class));
        ReflectionTestUtils.setField(service, "entityManager", mock(EntityManager.class));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ingredientRepository.findById(oreo.getId())).thenReturn(Optional.of(oreo));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe recipe = inv.getArgument(0);
            if (recipe.getId() == null) recipe.setId(recipeId);
            stored = recipe;
            return recipe;
        });
        when(recipeIngredientRepository.saveAll(anyList())).thenAnswer(inv -> withIds(inv.getArgument(0), savedIngredients));
        when(recipeToolRepository.saveAll(anyList())).thenAnswer(inv -> withIds(inv.getArgument(0), savedTools));
        when(recipeStepRepository.saveAll(anyList())).thenAnswer(inv -> withIds(inv.getArgument(0), savedSteps));
        when(stepIngredientRepository.saveAll(anyList())).thenAnswer(inv -> new ArrayList<>(inv.<List<?>>getArgument(0)));
        when(stepToolRepository.saveAll(anyList())).thenAnswer(inv -> new ArrayList<>(inv.<List<?>>getArgument(0)));
        when(recipeRepository.findById(recipeId)).thenAnswer(inv -> Optional.ofNullable(stored).map(recipe -> {
            if (!savedIngredients.isEmpty() || !savedTools.isEmpty() || !savedSteps.isEmpty()) {
                recipe.setIngredients(new ArrayList<>(savedIngredients));
                recipe.setTools(new ArrayList<>(savedTools));
                recipe.setSteps(new ArrayList<>(savedSteps));
            }
            return recipe;
        }));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> withIds(List<T> rows, List<T> sink) {
        for (T row : rows) {
            if (row instanceof RecipeIngredient r && r.getId() == null) r.setId(UUID.randomUUID());
            if (row instanceof RecipeTool r && r.getId() == null) r.setId(UUID.randomUUID());
            if (row instanceof RecipeStep r && r.getId() == null) r.setId(UUID.randomUUID());
        }
        sink.addAll(rows);
        return new ArrayList<>(rows);
    }

    private RecipeRequest personalRequest(RecipeStatus status) {
        return RecipeRequest.builder()
                .title("Bánh Oreo hấp").difficulty(DifficultyEnum.EASY).recipeStatus(status)
                .prepTimeMinutes(10).cookTimeMinutes(25).servings(1).imageIds(List.of())
                .ingredients(List.of(
                        IngredientEmbeddedRequest.builder().ref("a1").ingredientId(oreo.getId())
                                .quantity(BigDecimal.ONE).unit("gói").build(),
                        IngredientEmbeddedRequest.builder().ref("a2").rawName("  Sữa tươi không đường ")
                                .quantity(BigDecimal.valueOf(50)).unit("ml").build()))
                .tools(List.of(ToolEmbeddedRequest.builder().ref("t1").rawName("Cây lăn bột").quantity(BigDecimal.ONE).build()))
                .steps(List.of(
                        RecipeStepEmbeddedRequest.builder().stepNumber(1).instruction("Nghiền bánh, trộn sữa")
                                .durationMinutes(10).recipeStepType(RecipeStepType.PREPARATION)
                                .ingredients(List.of(
                                        IngredientEmbeddedRequest.builder().ref("a1").quantity(BigDecimal.ONE).build(),
                                        IngredientEmbeddedRequest.builder().ref("a2").quantity(BigDecimal.valueOf(50)).build()))
                                .tools(List.of(ToolEmbeddedRequest.builder().ref("t1").quantity(BigDecimal.ONE).build()))
                                .build(),
                        RecipeStepEmbeddedRequest.builder().stepNumber(2).instruction("Hấp 25 phút")
                                .durationMinutes(25).recipeStepType(RecipeStepType.COOKING)
                                .ingredients(List.of()).tools(List.of()).build()))
                .build();
    }

    @Test
    void createPersonalRecipeWithUnmatchedRowsSavesRawNamesAndLinksStepsByRef() {
        RecipeResponse response = service.create(personalRequest(RecipeStatus.PERSONAL));

        assertThat(response.getIngredients()).hasSize(2);
        assertThat(response.getIngredients().get(0).getIngredientId()).isEqualTo(oreo.getId());
        assertThat(response.getIngredients().get(0).getRawName()).isEqualTo("Bánh Oreo");
        assertThat(response.getIngredients().get(1).getIngredientId()).isNull();
        assertThat(response.getIngredients().get(1).getRawName()).isEqualTo("Sữa tươi không đường");
        assertThat(response.getTools()).singleElement().satisfies(t -> {
            assertThat(t.getToolId()).isNull();
            assertThat(t.getRawName()).isEqualTo("Cây lăn bột");
        });

        RecipeStep firstStep = savedSteps.get(0);
        assertThat(firstStep.getIngredients()).extracting(RecipeStepIngredient::getRecipeIngredient)
                .containsExactly(savedIngredients.get(0), savedIngredients.get(1));
        assertThat(firstStep.getTools()).extracting(RecipeStepTool::getRecipeTool).containsExactly(savedTools.get(0));
        verify(ingredientRepository, never()).findById(null);
        verify(toolRepository, never()).findById(any());
    }

    @Test
    void createPublishedRecipeWithUnmatchedRowIsRejectedBeforeAnyWrite() {
        Throwable thrown = catchThrowable(() -> service.create(personalRequest(RecipeStatus.PUBLISHED)));

        assertThat(((AppException) thrown).getErrorCode()).isEqualTo(RecipeErrorCode.UNMATCHED_ITEM_NOT_ALLOWED);
        verify(recipeRepository, never()).save(any());
        verify(recipeIngredientRepository, never()).saveAll(anyList());
    }

    @Test
    void switchingPersonalRecipeWithUnmatchedRowsToPublishedIsRejectedAndNothingChanges() {
        stored = Recipe.builder().id(recipeId).user(user).title("Bánh Oreo hấp")
                .recipeStatus(RecipeStatus.PERSONAL).build();

        Throwable thrown = catchThrowable(() -> service.update(recipeId, personalRequest(RecipeStatus.PUBLISHED)));

        assertThat(((AppException) thrown).getErrorCode()).isEqualTo(RecipeErrorCode.UNMATCHED_ITEM_NOT_ALLOWED);
        assertThat(stored.getRecipeStatus()).isEqualTo(RecipeStatus.PERSONAL);
        verify(recipeRepository, never()).save(any());
        verify(recipeIngredientRepository, never()).deleteByRecipe_Id(any());
        verify(recipeToolRepository, never()).deleteByRecipe_Id(any());
    }

    @Test
    void videoExtractedRecipeRoundTripsThroughUpdateUnchanged() {
        // State as written by POST /recipes/video-extractions: one matched and one unmatched row of each kind.
        stored = Recipe.builder().id(recipeId).user(user).title("Bánh Oreo hấp").difficulty(DifficultyEnum.EASY)
                .recipeStatus(RecipeStatus.PERSONAL).prepTimeMinutes(10).cookTimeMinutes(25).servings(1).build();
        RecipeIngredient matched = RecipeIngredient.builder().id(UUID.randomUUID()).recipe(stored).ingredient(oreo)
                .rawName("Bánh Oreo").quantity(BigDecimal.ONE).unit("gói").build();
        RecipeIngredient unmatched = RecipeIngredient.builder().id(UUID.randomUUID()).recipe(stored)
                .rawName("Bột nở").quantity(BigDecimal.valueOf(5)).unit("g").build();
        RecipeTool unmatchedTool = RecipeTool.builder().id(UUID.randomUUID()).recipe(stored)
                .rawName("Cây lăn bột").quantity(BigDecimal.ONE).build();
        RecipeStep prep = RecipeStep.builder().id(UUID.randomUUID()).recipe(stored).stepNumber(1).instruction("Trộn")
                .durationMinutes(10).recipeStepType(RecipeStepType.PREPARATION).build();
        prep.setIngredients(List.of(
                RecipeStepIngredient.builder().recipeStep(prep).recipeIngredient(matched).quantity(BigDecimal.ONE).build(),
                RecipeStepIngredient.builder().recipeStep(prep).recipeIngredient(unmatched).quantity(BigDecimal.valueOf(5)).build()));
        prep.setTools(List.of(RecipeStepTool.builder().recipeStep(prep).recipeTool(unmatchedTool).quantity(BigDecimal.ONE).build()));
        RecipeStep cook = RecipeStep.builder().id(UUID.randomUUID()).recipe(stored).stepNumber(2).instruction("Hấp")
                .durationMinutes(25).recipeStepType(RecipeStepType.COOKING).build();
        cook.setIngredients(List.of());
        cook.setTools(List.of());
        stored.setIngredients(List.of(matched, unmatched));
        stored.setTools(List.of(unmatchedTool));
        stored.setSteps(List.of(prep, cook));

        RecipeResponse before = RecipeMapper.toDTO(stored);
        RecipeResponse after = service.update(recipeId, toRequestAsMobileEditor(before));

        assertThat(after.getIngredients()).extracting("ingredientId", "rawName", "quantity", "unit")
                .containsExactlyElementsOf(before.getIngredients().stream()
                        .map(i -> org.assertj.core.groups.Tuple.tuple(i.getIngredientId(), i.getRawName(), i.getQuantity(), i.getUnit()))
                        .toList());
        assertThat(after.getTools()).extracting("toolId", "rawName", "quantity")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(null, "Cây lăn bột", BigDecimal.ONE));
        assertThat(after.getSteps().get(0).getIngredients()).extracting("rawName", "quantity")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Bánh Oreo", BigDecimal.ONE),
                        org.assertj.core.groups.Tuple.tuple("Bột nở", BigDecimal.valueOf(5)));
        assertThat(after.getSteps().get(0).getTools()).extracting("rawName").containsExactly("Cây lăn bột");
        assertThat(after.getSteps().get(1).getIngredients()).isEmpty();
    }

    /** What the mobile editor sends back: ref = row id from the response, rawName for unmatched rows. */
    private static RecipeRequest toRequestAsMobileEditor(RecipeResponse response) {
        return RecipeRequest.builder()
                .title(response.getTitle()).difficulty(response.getDifficulty()).recipeStatus(response.getRecipeStatus())
                .prepTimeMinutes(response.getPrepTimeMinutes()).cookTimeMinutes(response.getCookTimeMinutes())
                .servings(response.getServings()).imageIds(List.of())
                .ingredients(response.getIngredients().stream().map(i -> IngredientEmbeddedRequest.builder()
                        .ref(i.getId().toString()).ingredientId(i.getIngredientId()).rawName(i.getRawName())
                        .quantity(i.getQuantity()).unit(i.getUnit()).build()).toList())
                .tools(response.getTools().stream().map(t -> ToolEmbeddedRequest.builder()
                        .ref(t.getId().toString()).toolId(t.getToolId()).rawName(t.getRawName())
                        .quantity(t.getQuantity()).build()).toList())
                .steps(response.getSteps().stream().map(s -> RecipeStepEmbeddedRequest.builder()
                        .stepNumber(s.getStepNumber()).instruction(s.getInstruction())
                        .durationMinutes(s.getDurationMinutes()).recipeStepType(s.getRecipeStepType())
                        .ingredients(s.getIngredients().stream().map(i -> IngredientEmbeddedRequest.builder()
                                .ref(i.getRecipeIngredientId().toString()).quantity(i.getQuantity()).build()).toList())
                        .tools(s.getTools().stream().map(t -> ToolEmbeddedRequest.builder()
                                .ref(t.getRecipeToolId().toString()).quantity(t.getQuantity()).build()).toList())
                        .build()).toList())
                .build();
    }
}
