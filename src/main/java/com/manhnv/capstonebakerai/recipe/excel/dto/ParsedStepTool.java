package com.manhnv.capstonebakerai.recipe.excel.dto;

import com.manhnv.capstonebakerai.recipe.entity.Tool;

import java.math.BigDecimal;

public record ParsedStepTool(int rowNumber, Tool tool, BigDecimal quantity, String note) {
}
