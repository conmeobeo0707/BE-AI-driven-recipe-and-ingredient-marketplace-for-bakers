package com.manhnv.capstonebakerai.hobby.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.hobby.dto.SaveHobbyRequest;
import com.manhnv.capstonebakerai.hobby.entity.Hobby;
import com.manhnv.capstonebakerai.hobby.exception.HobbyErrorCode;
import com.manhnv.capstonebakerai.hobby.repository.HobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HobbyServiceImpl implements HobbyService {

    private final HobbyRepository hobbyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HobbyResponse> getAll() {
        return toResponses(hobbyRepository.findAllByOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HobbyResponse> getActiveHobbies() {
        return toResponses(hobbyRepository.findAllByActiveTrueOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public HobbyResponse getById(UUID id) {
        return toResponse(findHobby(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HobbyResponse> getByIds(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return toResponses(hobbyRepository.findAllByIdIn(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HobbyResponse> getActiveByIds(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return toResponses(hobbyRepository.findAllByIdInAndActiveTrue(ids));
    }

    @Override
    @Transactional
    public HobbyResponse create(SaveHobbyRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name);

        Hobby hobby = new Hobby();
        hobby.setName(name);
        hobby.setDescription(normalizeDescription(request.description()));
        hobby.setActive(true);

        return save(hobby);
    }

    @Override
    @Transactional
    public HobbyResponse update(UUID id, SaveHobbyRequest request) {
        Hobby hobby = findHobby(id);
        String name = request.name().trim();

        if (hobbyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new AppException(HobbyErrorCode.HOBBY_NAME_ALREADY_EXISTS);
        }

        hobby.setName(name);
        hobby.setDescription(normalizeDescription(request.description()));

        return save(hobby);
    }

    @Override
    @Transactional
    public HobbyResponse setActive(UUID id, boolean active) {
        Hobby hobby = findHobby(id);
        hobby.setActive(active);
        return save(hobby);
    }

    private Hobby findHobby(UUID id) {
        return hobbyRepository.findById(id)
                .orElseThrow(() -> new AppException(HobbyErrorCode.HOBBY_NOT_FOUND));
    }

    private void ensureNameAvailable(String name) {
        if (hobbyRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(HobbyErrorCode.HOBBY_NAME_ALREADY_EXISTS);
        }
    }

    private HobbyResponse save(Hobby hobby) {
        try {
            return toResponse(hobbyRepository.saveAndFlush(hobby));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(HobbyErrorCode.HOBBY_NAME_ALREADY_EXISTS);
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }

    private List<HobbyResponse> toResponses(List<Hobby> hobbies) {
        return hobbies.stream()
                .sorted(Comparator.comparing(Hobby::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    private HobbyResponse toResponse(Hobby hobby) {
        return new HobbyResponse(
                hobby.getId(),
                hobby.getName(),
                hobby.getDescription(),
                hobby.isActive()
        );
    }
}
