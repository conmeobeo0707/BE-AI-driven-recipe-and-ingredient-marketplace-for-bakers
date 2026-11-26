package com.manhnv.capstonebakerai.recipe.excel.dto;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;

import java.math.BigDecimal;

public record ParsedStepIngredient(int rowNumber, Ingredient ingredient, BigDecimal quantity, String unit, String note) {
}
