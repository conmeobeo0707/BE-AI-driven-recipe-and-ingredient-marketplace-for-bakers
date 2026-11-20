package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recipe.dto.request.*;
import com.manhnv.capstonebakerai.recipe.entity.*;
import com.manhnv.capstonebakerai.recipe.helper.RecipeMediaHelper;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepResourceHelper;
import com.manhnv.capstonebakerai.recipe.repository.*;
import com.manhnv.capstonebakerai.recipe.service.impl.RecipeServiceImpl;
import com.manhnv.capstonebakerai.recipe.service.impl.RecipeStepServiceImpl;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeStepNestedResourcesTest {
    @Mock RecipeRepository recipes;
    @Mock RecipeStepRepository steps;
    @Mock RecipeStepImageRepository stepImages;
    @Mock RecipeImageRepository images;
    @Mock RecipeIngredientRepository recipeIngredients;
    @Mock RecipeToolRepository recipeTools;
    @Mock IngredientRepository ingredients;
    @Mock ToolRepository tools;
    @Mock RecipeCategoryRepository categories;
    @Mock UserRepository users;
    @Mock RecipeMediaHelper mediaHelper;
    @Mock RecipeStepResourceHelper resourceHelper;
    @Mock ApplicationEventPublisher publisher;
    @Mock EntityManager entityManager;
    @InjectMocks RecipeServiceImpl recipeService;
    @InjectMocks RecipeStepServiceImpl stepService;

    @BeforeEach
    void injectPersistenceContext() {
        ReflectionTestUtils.setField(recipeService, "entityManager", entityManager);
        ReflectionTestUtils.setField(stepService, "entityManager", entityManager);
    }

    private RecipeStepEmbeddedRequest stepRequest() {
        return RecipeStepEmbeddedRequest.builder().stepNumber(1).instruction("Mix")
                .ingredients(List.of(new IngredientEmbeddedRequest(UUID.randomUUID(), BigDecimal.TEN)))
                .tools(List.of(new ToolEmbeddedRequest(UUID.randomUUID(), BigDecimal.ONE))).build();
    }

    @Test
    void standaloneCreateSavesNestedResources() {
        var nested = stepRequest();
        var recipe = Recipe.builder().id(UUID.randomUUID()).build();
        when(recipes.findById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(steps.save(any())).thenAnswer(c -> c.getArgument(0));
        stepService.createRecipeStep(RecipeStepRequest.builder().recipeId(recipe.getId())
                .stepNumber(1).instruction("Mix").ingredients(nested.getIngredients()).tools(nested.getTools()).build());
        verify(resourceHelper).replaceResources(any(RecipeStep.class), eq(nested.getIngredients()), eq(nested.getTools()));
    }

    @Test
    void standaloneUpdateReplacesNestedResources() {
        var request = stepRequest();
        var recipe = Recipe.builder().id(UUID.randomUUID()).build();
        var step = RecipeStep.builder().id(UUID.randomUUID()).recipe(recipe).stepNumber(1).build();
        when(steps.findById(step.getId())).thenReturn(Optional.of(step));
        when(steps.findByRecipe_IdOrderByStepNumberAsc(recipe.getId())).thenReturn(List.of(step));
        stepService.updateRecipeStep(step.getId(), request);
        verify(resourceHelper).replaceResources(step, request.getIngredients(), request.getTools());
    }

    @Test
    void recipeCreateSavesResourcesForEmbeddedSteps() {
        var nested = stepRequest();
        var recipe = Recipe.builder().id(UUID.randomUUID()).build();
        var userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(new User()));
        when(recipes.save(any())).thenReturn(recipe);
        when(recipes.findById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(steps.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            recipeService.create(RecipeRequest.builder().steps(List.of(nested)).build());
        }
        verify(resourceHelper).replaceResources(any(RecipeStep.class), eq(nested.getIngredients()), eq(nested.getTools()));
    }

    @Test
    void recipeUpdateSavesResourcesForReplacementSteps() {
        var nested = stepRequest();
        var recipe = Recipe.builder().id(UUID.randomUUID()).build();
        when(recipes.findById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(steps.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        recipeService.update(recipe.getId(), RecipeRequest.builder().steps(List.of(nested)).build());
        verify(resourceHelper).replaceResources(any(RecipeStep.class), eq(nested.getIngredients()), eq(nested.getTools()));
    }
}
