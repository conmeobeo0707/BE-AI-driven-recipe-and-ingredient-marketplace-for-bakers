package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recipe.event.CommentCreatedEvent;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCommentRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCommentResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeComment;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeCommentMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCommentRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeCommentServiceImpl implements RecipeCommentService {

    private final RecipeCommentRepository repository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeCommentResponse> getAll(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeComment> page = (search != null && !search.isBlank())
                ? repository.findByContentContainingIgnoreCase(search.trim(), validatedPageable)
                : repository.findAll(validatedPageable);

        List<RecipeCommentResponse> list = page.map(RecipeCommentMapper::toDTO).toList();

        return PageResponse.<RecipeCommentResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeCommentResponse getById(UUID id) {
        RecipeComment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeComment not found"));
        return RecipeCommentMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public RecipeCommentResponse create(RecipeCommentRequest request) {
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecipeComment entity = new RecipeComment();
        entity.setRecipe(recipe);
        entity.setUser(user);
        entity.setContent(request.getContent());

        UUID parentOwnerId = null;
        if (request.getParentId() != null) {
            RecipeComment parent = repository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            entity.setParent(parent);
            parentOwnerId = parent.getUser().getUserId();
        }

        RecipeComment saved = repository.save(entity);

        eventPublisher.publishEvent(new CommentCreatedEvent(
                saved.getId(),
                currentUserId,
                recipe.getId(),
                recipe.getTitle(),
                recipe.getUser().getUserId(),
                parentOwnerId
        ));

        return RecipeCommentMapper.toDTO(saved);
    }

    @Override
    public RecipeCommentResponse update(UUID id, RecipeCommentRequest request) {
        RecipeComment existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeComment not found"));

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existing.setRecipe(recipe);
        existing.setUser(user);
        existing.setContent(request.getContent());

        if (request.getParentId() != null) {
            RecipeComment parent = repository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            existing.setParent(parent);
        } else {
            existing.setParent(null);
        }

        return RecipeCommentMapper.toDTO(repository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("RecipeComment not found");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeCommentResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeComment> page = (search != null && !search.isBlank())
                ? repository.findByRecipe_IdAndContentContainingIgnoreCase(recipeId, search.trim(), validatedPageable)
                : repository.findByRecipe_Id(recipeId, validatedPageable);

        List<RecipeCommentResponse> list = page.map(RecipeCommentMapper::toDTO).toList();

        return PageResponse.<RecipeCommentResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeCommentResponse> getByUserId(UUID userId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeComment> page = (search != null && !search.isBlank())
                ? repository.findByUser_UserIdAndContentContainingIgnoreCase(userId, search.trim(), validatedPageable)
                : repository.findByUser_UserId(userId, validatedPageable);

        List<RecipeCommentResponse> list = page.map(RecipeCommentMapper::toDTO).toList();

        return PageResponse.<RecipeCommentResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeCommentResponse> getReplies(UUID parentId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeComment> page = (search != null && !search.isBlank())
                ? repository.findByParent_IdAndContentContainingIgnoreCase(parentId, search.trim(), validatedPageable)
                : repository.findByParent_Id(parentId, validatedPageable);

        List<RecipeCommentResponse> list = page.map(RecipeCommentMapper::toDTO).toList();

        return PageResponse.<RecipeCommentResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }
}
