package com.manhnv.capstonebakerai.media.service;

import java.util.UUID;

public interface MediaCleanupService {
    boolean deleteIfTempOrDeleting(UUID id);
}
