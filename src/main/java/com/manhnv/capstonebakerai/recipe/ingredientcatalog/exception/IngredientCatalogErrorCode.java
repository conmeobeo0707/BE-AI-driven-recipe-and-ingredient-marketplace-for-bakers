package com.manhnv.capstonebakerai.recipe.ingredientcatalog.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IngredientCatalogErrorCode implements ErrorCode {

    INVALID_INGREDIENT_NAME(
            "Ingredient name must contain letters or numbers and be at most 150 characters",
            HttpStatus.BAD_REQUEST
    ),
    CATALOG_NOT_FOUND("Ingredient catalog not found", HttpStatus.NOT_FOUND),
    ALIAS_NOT_FOUND("Ingredient alias not found", HttpStatus.NOT_FOUND),
    ALIAS_ALREADY_EXISTS("This alias already exists for the ingredient", HttpStatus.CONFLICT),
    NAME_ALREADY_IN_USE("This normalized name already belongs to another ingredient", HttpStatus.CONFLICT),
    CANONICAL_ALIAS_REQUIRED("The current canonical name alias cannot be deleted", HttpStatus.CONFLICT),
    CATALOG_USED_BY_PUBLISHED_RECIPE("Cannot deactivate an ingredient used by a published recipe", HttpStatus.CONFLICT),
    INVALID_SEARCH("Keyword must be at most 150 characters and limit must be between 1 and 20", HttpStatus.BAD_REQUEST),
    INVALID_BACKFILL("Provide 1 to 200 distinct ingredient mappings", HttpStatus.BAD_REQUEST),
    INVALID_BACKFILL_LIMIT("Backfill preview limit must be between 1 and 200", HttpStatus.BAD_REQUEST),
    BACKFILL_CONFLICT("An ingredient no longer exists or has already been mapped. Refresh the preview", HttpStatus.CONFLICT),
    CATALOG_INACTIVE("The selected ingredient catalog does not exist or is inactive", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;
}
