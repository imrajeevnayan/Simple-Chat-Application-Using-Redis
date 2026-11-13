package com.example.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "participant is required")
        @Size(max = 64, message = "participant must be at most 64 characters")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "participant may contain only letters, numbers, underscores, and hyphens")
        String participant,
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message) {
}
