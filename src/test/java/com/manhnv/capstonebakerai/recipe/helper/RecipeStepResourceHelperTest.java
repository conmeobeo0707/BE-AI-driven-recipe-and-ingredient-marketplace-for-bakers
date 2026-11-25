package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.recipe.dto.request.*;
import com.manhnv.capstonebakerai.recipe.entity.*;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepMapper;
import com.manhnv.capstonebakerai.recipe.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeStepResourceHelperTest {
    @Mock RecipeStepIngredientRepository stepIngredients;
    @Mock RecipeStepToolRepository stepTools;
    @Mock RecipeIngredientRepository recipeIngredients;
    @Mock RecipeToolRepository recipeTools;
    @InjectMocks RecipeStepResourceHelper helper;

    private final Recipe recipe = Recipe.builder().id(UUID.randomUUID()).build();

    @Test
    void replacesBothListsByRecipeLinkIdsAndReturnsThemInStepResponse() {
        Ingredient flour = Ingredient.builder().id(UUID.randomUUID()).name("Flour").build();
        RecipeIngredient flourLink = RecipeIngredient.builder().id(UUID.randomUUID()).recipe(recipe)
                .ingredient(flour).rawName("Flour").unit("g").build();
        RecipeTool bowlLink = RecipeTool.builder().id(UUID.randomUUID()).recipe(recipe)
                .tool(Tool.builder().id(UUID.randomUUID()).name("Bowl").build()).rawName("Bowl").build();
        RecipeStep step = RecipeStep.builder().id(UUID.randomUUID()).recipe(recipe).build();
        when(recipeIngredients.findById(flourLink.getId())).thenReturn(Optional.of(flourLink));
        when(recipeTools.findById(bowlLink.getId())).thenReturn(Optional.of(bowlLink));
        when(stepIngredients.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        when(stepTools.saveAll(any())).thenAnswer(c -> c.getArgument(0));

        helper.replaceResources(step,
                List.of(new StepIngredientEmbeddedRequest(flourLink.getId(), new BigDecimal("125.50"))),
                List.of(new StepToolEmbeddedRequest(bowlLink.getId(), BigDecimal.ONE)));

        var response = RecipeStepMapper.toDTO(step);
        var ingredient = response.getIngredients().getFirst();
        assertEquals(flourLink.getId(), ingredient.getRecipeIngredientId());
        assertEquals(flour.getId(), ingredient.getIngredientId());
        assertEquals("Flour", ingredient.getIngredientName());
        assertEquals(new BigDecimal("125.50"), ingredient.getQuantity());
        assertEquals("g", ingredient.getUnit());
        assertEquals(bowlLink.getId(), response.getTools().getFirst().getRecipeToolId());
        assertEquals(bowlLink.getTool().getId(), response.getTools().getFirst().getToolId());
        var order = inOrder(stepIngredients, stepTools);
        order.verify(stepIngredients).deleteByRecipeStep_Id(step.getId());
        order.verify(stepTools).deleteByRecipeStep_Id(step.getId());
        order.verify(stepIngredients).flush();
        order.verify(stepTools).flush();
        order.verify(stepIngredients).saveAll(any());
        order.verify(stepTools).saveAll(any());
    }

    @Test
    void unmatchedRecipeIngredientCanBeUsedInStep() {
        RecipeIngredient sauce = RecipeIngredient.builder().id(UUID.randomUUID()).recipe(recipe)
                .rawName("sốt chocolate").build();
        RecipeStep step = RecipeStep.builder().id(UUID.randomUUID()).recipe(recipe).build();
        when(recipeIngredients.findById(sauce.getId())).thenReturn(Optional.of(sauce));
        when(stepIngredients.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        when(stepTools.saveAll(any())).thenAnswer(c -> c.getArgument(0));

        helper.replaceResources(step, List.of(new StepIngredientEmbeddedRequest(sauce.getId(), BigDecimal.ONE)), null);

        var ingredient = RecipeStepMapper.toDTO(step).getIngredients().getFirst();
        assertEquals(sauce.getId(), ingredient.getRecipeIngredientId());
        assertNull(ingredient.getIngredientId());
        assertEquals("sốt chocolate", ingredient.getIngredientName());
    }

    @Test
    void rejectsRecipeLinkFromAnotherRecipe() {
        RecipeIngredient other = RecipeIngredient.builder().id(UUID.randomUUID())
                .recipe(Recipe.builder().id(UUID.randomUUID()).build()).build();
        RecipeStep step = RecipeStep.builder().id(UUID.randomUUID()).recipe(recipe).build();
        when(recipeIngredients.findById(other.getId())).thenReturn(Optional.of(other));

        assertThrows(ResponseStatusException.class, () -> helper.replaceResources(step,
                List.of(new StepIngredientEmbeddedRequest(other.getId(), BigDecimal.ONE)), List.of()));
        verifyNoInteractions(stepIngredients, stepTools);
    }

    @Test
    void omittedListsClearExistingLinks() {
        var step = RecipeStep.builder().id(UUID.randomUUID()).recipe(recipe).build();
        helper.replaceResources(step, null, null);
        verify(stepIngredients).deleteByRecipeStep_Id(step.getId());
        verify(stepTools).deleteByRecipeStep_Id(step.getId());
        verify(stepIngredients).saveAll(List.of());
        verify(stepTools).saveAll(List.of());
        verifyNoInteractions(recipeIngredients, recipeTools);
    }

    @Test
    void rejectsDuplicatesBeforeChangingExistingLinks() {
        var request = new StepIngredientEmbeddedRequest(UUID.randomUUID(), BigDecimal.ONE);
        assertThrows(ResponseStatusException.class, () -> helper.replaceResources(
                RecipeStep.builder().recipe(recipe).build(), List.of(request, request), List.of()));
        verifyNoInteractions(stepIngredients, stepTools, recipeIngredients, recipeTools);
    }

    @Test
    void missingRecipeToolDoesNotDeleteExistingLinks() {
        var id = UUID.randomUUID();
        when(recipeTools.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> helper.replaceResources(
                RecipeStep.builder().recipe(recipe).build(), List.of(),
                List.of(new StepToolEmbeddedRequest(id, BigDecimal.ONE))));
        verifyNoInteractions(stepIngredients, stepTools);
    }

    @Test
    void embeddedStepsResolveToSavedRecipeLinksByCatalogId() {
        Ingredient flour = Ingredient.builder().id(UUID.randomUUID()).name("Flour").build();
        RecipeIngredient flourLink = RecipeIngredient.builder().id(UUID.randomUUID()).recipe(recipe).ingredient(flour).build();
        RecipeStep step = RecipeStep.builder().id(UUID.randomUUID()).recipe(recipe).stepNumber(1).build();
        when(stepIngredients.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        when(stepTools.saveAll(any())).thenAnswer(c -> c.getArgument(0));

        helper.replaceEmbeddedResources(step, List.of(new IngredientEmbeddedRequest(flour.getId(), BigDecimal.TEN)),
                List.of(), Map.of(flour.getId(), flourLink), Map.of());

        assertSame(flourLink, step.getIngredients().getFirst().getRecipeIngredient());
        assertThrows(ResponseStatusException.class, () -> helper.replaceEmbeddedResources(step, List.of(),
                List.of(new ToolEmbeddedRequest(UUID.randomUUID(), BigDecimal.ONE)), Map.of(), Map.of()));
    }
}
