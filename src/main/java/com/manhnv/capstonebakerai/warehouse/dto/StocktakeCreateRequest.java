package com.manhnv.capstonebakerai.warehouse.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
public record StocktakeCreateRequest(@NotEmpty List<@NotNull UUID> lotIds, UUID handleBy) { }
