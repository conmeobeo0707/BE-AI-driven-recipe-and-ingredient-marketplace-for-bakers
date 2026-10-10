package com.manhnv.capstonebakerai.hobby.service;

import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.hobby.dto.SaveHobbyRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HobbyService {

    List<HobbyResponse> getAll();

    List<HobbyResponse> getActiveHobbies();

    HobbyResponse getById(UUID id);

    List<HobbyResponse> getByIds(Set<UUID> ids);

    List<HobbyResponse> getActiveByIds(Set<UUID> ids);

    HobbyResponse create(SaveHobbyRequest request);

    HobbyResponse update(UUID id, SaveHobbyRequest request);

    HobbyResponse setActive(UUID id, boolean active);
}
