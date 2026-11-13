package com.example.chatapp.model;

import java.time.Instant;

public record ChatMessage(String participant, String message, Instant timestamp) {
}
