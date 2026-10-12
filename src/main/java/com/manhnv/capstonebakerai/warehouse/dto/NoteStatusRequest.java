package com.manhnv.capstonebakerai.warehouse.dto;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import jakarta.validation.constraints.NotNull;
public record NoteStatusRequest(@NotNull NoteStatus status) { }
