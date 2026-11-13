package com.example.chatapp.exception;

public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(String roomId) {
        super("Chat room '" + roomId + "' does not exist.");
    }
}
