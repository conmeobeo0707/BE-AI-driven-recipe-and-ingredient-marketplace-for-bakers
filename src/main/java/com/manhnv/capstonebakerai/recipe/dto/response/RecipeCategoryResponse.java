package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCategoryResponse {
    private UUID id;
    private String name;
}