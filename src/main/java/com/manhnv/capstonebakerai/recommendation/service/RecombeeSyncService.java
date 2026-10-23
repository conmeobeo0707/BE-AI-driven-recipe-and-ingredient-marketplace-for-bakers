package com.manhnv.capstonebakerai.recommendation.service;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.entity.CustomerHobby;
import com.manhnv.capstonebakerai.customer.repository.CustomerHobbyRepository;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.hobby.entity.Hobby;
import com.manhnv.capstonebakerai.hobby.repository.HobbyRepository;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionDeletedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionRecordedEvent;
import com.recombee.api_client.RecombeeClient;
import com.recombee.api_client.api_requests.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecombeeSyncService {

    private final ObjectProvider<RecombeeClient> clientProvider;
    private final RecipeRepository recipeRepository;
    private final CustomerRepository customerRepository;
    private final CustomerHobbyRepository customerHobbyRepository;
    private final HobbyRepository hobbyRepository;

    @Transactional(readOnly = true)
    public void syncRecipe(UUID recipeId) throws Exception {
        RecombeeClient client = clientProvider.getIfAvailable();
        if (client == null) return;

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElse(null);
        if (recipe == null) return;

        Map<String, Object> values = new HashMap<>();
        values.put("title", recipe.getTitle());
        values.put("description", recipe.getDescription());
        values.put("category", recipe.getRecipeCategory() == null
                ? null : recipe.getRecipeCategory().getName());
        values.put("difficulty", recipe.getDifficulty() == null
                ? null : recipe.getDifficulty().name());
        values.put("ingredientNames", recipe.getIngredients() == null
                ? List.of()
                : recipe.getIngredients().stream()
                .map(Ingredient::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList());
        values.put("prepTimeMinutes", recipe.getPrepTimeMinutes());
        values.put("cookTimeMinutes", recipe.getCookTimeMinutes());
        values.put("servings", recipe.getServings());
        values.put("isPublished", recipe.getRecipeStatus() == RecipeStatus.PUBLISHED);

        client.send(new SetItemValues(recipe.getId().toString(), values).setCascadeCreate(true));
    }

    @Transactional(readOnly = true)
    public void syncCustomer(UUID customerId) throws Exception {
        RecombeeClient client = clientProvider.getIfAvailable();
        if (client == null) return;

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return;

        Set<UUID> hobbyIds = customerHobbyRepository.findAllByCustomerId(customerId).stream()
                .map(CustomerHobby::getHobbyId)
                .collect(Collectors.toSet());
        List<String> hobbyNames = hobbyIds.isEmpty()
                ? List.of()
                : hobbyRepository.findAllByIdIn(hobbyIds).stream()
                .map(Hobby::getName)
                .filter(Objects::nonNull)
                .toList();

        Map<String, Object> values = new HashMap<>();
        values.put("skillLevel", customer.getSkillLevel() == null ? null : customer.getSkillLevel().name());
        values.put("hobbies", hobbyNames);

        client.send(new SetUserValues(customerId.toString(), values).setCascadeCreate(true));
    }

    public void syncInteraction(RecipeInteractionRecordedEvent event) throws Exception {
        RecombeeClient client = clientProvider.getIfAvailable();
        if (client == null || event.recipeId() == null || event.type() == RecipeInteractionType.SEARCH) return;

        String userId = event.customerId().toString();
        String itemId = event.recipeId().toString();
        Date timestamp = Date.from(event.occurredAt());

        switch (event.type()) {
            case VIEW -> {
                AddDetailView request = new AddDetailView(userId, itemId)
                        .setTimestamp(timestamp)
                        .setCascadeCreate(true);
                if (hasText(event.recommId())) request.setRecommId(event.recommId());
                client.send(request);
            }
            case BOOKMARK -> {
                AddBookmark request = new AddBookmark(userId, itemId)
                        .setTimestamp(timestamp)
                        .setCascadeCreate(true);
                if (hasText(event.recommId())) request.setRecommId(event.recommId());
                client.send(request);
            }
            case RATING -> {
                if (event.ratingScore() == null) return;
                double rating = (event.ratingScore() - 3) / 2.0;
                AddRating request = new AddRating(userId, itemId, rating)
                        .setTimestamp(timestamp)
                        .setCascadeCreate(true);
                if (hasText(event.recommId())) request.setRecommId(event.recommId());
                client.send(request);
            }
            case SEARCH -> { }
        }
    }

    public void deleteInteraction(RecipeInteractionDeletedEvent event) throws Exception {
        RecombeeClient client = clientProvider.getIfAvailable();
        if (client == null) return;

        String userId = event.customerId().toString();
        String itemId = event.recipeId().toString();
        if (event.type() == RecipeInteractionType.BOOKMARK) {
            client.send(new DeleteBookmark(userId, itemId));
        } else if (event.type() == RecipeInteractionType.RATING) {
            client.send(new DeleteRating(userId, itemId));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
