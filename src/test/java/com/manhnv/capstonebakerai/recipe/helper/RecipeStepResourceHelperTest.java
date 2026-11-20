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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeStepResourceHelperTest {
    @Mock IngredientRepository ingredients;
    @Mock ToolRepository tools;
    @Mock RecipeStepIngredientRepository stepIngredients;
    @Mock RecipeStepToolRepository stepTools;
    @InjectMocks RecipeStepResourceHelper helper;

    @Test
    void replacesBothListsAndReturnsThemInStepResponse() {
        RecipeStep step = RecipeStep.builder().id(UUID.randomUUID()).build();
        Ingredient ingredient = Ingredient.builder().id(UUID.randomUUID()).name("Flour").unit("g").build();
        Tool tool = Tool.builder().id(UUID.randomUUID()).name("Bowl").build();
        when(ingredients.findById(ingredient.getId())).thenReturn(Optional.of(ingredient));
        when(tools.findById(tool.getId())).thenReturn(Optional.of(tool));
        when(stepIngredients.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        when(stepTools.saveAll(any())).thenAnswer(c -> c.getArgument(0));
        helper.replaceResources(step,
                List.of(new IngredientEmbeddedRequest(ingredient.getId(), new BigDecimal("125.50"))),
                List.of(new ToolEmbeddedRequest(tool.getId(), BigDecimal.ONE)));
        var response = RecipeStepMapper.toDTO(step);
        assertEquals(ingredient.getId(), response.getIngredients().getFirst().getIngredientId());
        assertEquals(new BigDecimal("125.50"), response.getIngredients().getFirst().getQuantity());
        assertEquals("g", response.getIngredients().getFirst().getUnit());
        assertEquals(tool.getId(), response.getTools().getFirst().getToolId());
        var order = inOrder(stepIngredients, stepTools);
        order.verify(stepIngredients).deleteByRecipeStep_Id(step.getId());
        order.verify(stepTools).deleteByRecipeStep_Id(step.getId());
        order.verify(stepIngredients).flush();
        order.verify(stepTools).flush();
        order.verify(stepIngredients).saveAll(any());
        order.verify(stepTools).saveAll(any());
    }

    @Test
    void omittedListsClearExistingLinks() {
        var step = RecipeStep.builder().id(UUID.randomUUID()).build();
        helper.replaceResources(step, null, null);
        verify(stepIngredients).deleteByRecipeStep_Id(step.getId());
        verify(stepTools).deleteByRecipeStep_Id(step.getId());
        verify(stepIngredients).saveAll(List.of());
        verify(stepTools).saveAll(List.of());
        verifyNoInteractions(ingredients, tools);
    }

    @Test
    void rejectsDuplicatesBeforeChangingExistingLinks() {
        var request = new IngredientEmbeddedRequest(UUID.randomUUID(), BigDecimal.ONE);
        assertThrows(ResponseStatusException.class, () -> helper.replaceResources(
                new RecipeStep(), List.of(request, request), List.of()));
        verifyNoInteractions(stepIngredients, stepTools, ingredients, tools);
    }

    @Test
    void missingToolDoesNotDeleteExistingLinks() {
        var id = UUID.randomUUID();
        when(tools.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> helper.replaceResources(
                new RecipeStep(), List.of(), List.of(new ToolEmbeddedRequest(id, BigDecimal.ONE))));
        verifyNoInteractions(stepIngredients, stepTools);
    }
}
