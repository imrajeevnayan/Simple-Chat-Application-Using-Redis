package com.example.chatapp.exception;

public class ChatRoomAlreadyExistsException extends RuntimeException {
    public ChatRoomAlreadyExistsException(String roomId) {
        super("Chat room '" + roomId + "' already exists.");
    }
}
