package com.manhnv.capstonebakerai.recipe.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecipeErrorCode implements ErrorCode {

    RECIPE_NOT_FOUND("Recipe not found", HttpStatus.NOT_FOUND),
    RECIPE_ACCESS_DENIED("You do not have permission to modify this recipe", HttpStatus.FORBIDDEN),
    PERSONAL_STATUS_LOCKED("Only the owner can mark a recipe as personal", HttpStatus.FORBIDDEN),
    RECIPE_ALREADY_HIDDEN("Recipe is already hidden", HttpStatus.CONFLICT),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    RECIPE_STEP_NOT_FOUND("Recipe step not found", HttpStatus.NOT_FOUND),
    RECIPE_STEP_INGREDIENT_NOT_FOUND("Recipe step ingredient not found", HttpStatus.NOT_FOUND),
    RECIPE_STEP_TOOL_NOT_FOUND("Recipe step tool not found", HttpStatus.NOT_FOUND),
    RECIPE_INGREDIENT_NOT_FOUND("Recipe ingredient not found", HttpStatus.NOT_FOUND),
    RECIPE_TOOL_NOT_FOUND("Recipe tool not found", HttpStatus.NOT_FOUND),
    STEP_INGREDIENT_DUPLICATE("Ingredient already exists in this step", HttpStatus.BAD_REQUEST),
    STEP_TOOL_DUPLICATE("Tool already exists in this step", HttpStatus.BAD_REQUEST),
    DUPLICATE_ITEM_IDS("The same item appears more than once", HttpStatus.BAD_REQUEST),
    STEP_QUANTITY_EXCEEDS_RECIPE("Quantity used in steps exceeds the recipe quantity", HttpStatus.BAD_REQUEST),
    STEP_QUANTITY_MISMATCH("Total quantity across steps must equal the recipe quantity", HttpStatus.BAD_REQUEST),
    STEP_ITEM_NOT_IN_RECIPE("Step item is not part of the recipe", HttpStatus.BAD_REQUEST),
    RECIPE_ITEM_USED_BY_STEPS("Item is still used by steps of this recipe", HttpStatus.BAD_REQUEST),
    RECIPE_QUANTITY_BELOW_STEP_USAGE("Recipe quantity is less than the quantity used by steps", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;
}
