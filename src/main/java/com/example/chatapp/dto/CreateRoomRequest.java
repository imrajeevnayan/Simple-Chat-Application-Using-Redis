package com.example.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "roomName is required")
        @Size(max = 64, message = "roomName must be at most 64 characters")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "roomName may contain only letters, numbers, underscores, and hyphens")
        String roomName) {
}
