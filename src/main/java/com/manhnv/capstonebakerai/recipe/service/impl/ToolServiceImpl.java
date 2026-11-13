package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.ToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.ToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.mapper.ToolMapper;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

    private final ToolRepository repository;

    @Override
    public PageResponse<ToolResponse> getAll(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<Tool> page = (search != null && !search.isBlank())
                ? repository.findByNameContainingIgnoreCase(search.trim(), validatedPageable)
                : repository.findAll(validatedPageable);

        List<ToolResponse> list = page.map(ToolMapper::toDTO).toList();

        return PageResponse.<ToolResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    public ToolResponse getById(UUID id) {
        Tool entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));
        return ToolMapper.toDTO(entity);
    }

    @Override
    public ToolResponse create(ToolRequest request) {
        Tool entity = ToolMapper.toEntity(request);

        return ToolMapper.toDTO(repository.save(entity));
    }

    @Override
    public ToolResponse update(UUID id, ToolRequest request) {
        Tool existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found"));

        existing.setName(request.getName());

        return ToolMapper.toDTO(repository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Tool not found");
        }
        repository.deleteById(id);
    }

}
