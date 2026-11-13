package com.example.chatapp.dto;

public record ApiResponse(String message, String status, String roomId) {
    public static ApiResponse success(String message) {
        return new ApiResponse(message, "success", null);
    }

    public static ApiResponse created(String message, String roomId) {
        return new ApiResponse(message, "success", roomId);
    }
}
