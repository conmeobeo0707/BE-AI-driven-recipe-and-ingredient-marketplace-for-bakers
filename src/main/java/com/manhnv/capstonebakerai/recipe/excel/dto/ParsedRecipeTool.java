package com.manhnv.capstonebakerai.recipe.excel.dto;

import com.manhnv.capstonebakerai.recipe.entity.Tool;

import java.math.BigDecimal;

public record ParsedRecipeTool(Tool tool, BigDecimal quantity) {
}
