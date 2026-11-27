package com.manhnv.capstonebakerai.recipe.dto.response;

import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientAliasResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientResponse {
    private UUID id;
    private String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<IngredientAliasResponse> aliases;
}
