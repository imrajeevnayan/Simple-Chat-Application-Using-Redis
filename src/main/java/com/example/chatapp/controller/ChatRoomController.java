package com.example.chatapp.controller;

import com.example.chatapp.dto.ApiResponse;
import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.MessageHistoryResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.service.ChatRoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/chatapp/chatrooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        chatRoomService.createRoom(request.roomName());
        return ApiResponse.created("Chatroom '" + request.roomName() + "' created successfully.", request.roomName());
    }

    @PostMapping("/{roomId}/join")
    public ApiResponse joinRoom(@PathVariable String roomId, @Valid @RequestBody JoinRoomRequest request) {
        chatRoomService.joinRoom(roomId, request.participant());
        return ApiResponse.success("User '" + request.participant() + "' joined chatroom '" + roomId + "'.");
    }

    @PostMapping("/{roomId}/messages")
    public ApiResponse sendMessage(@PathVariable String roomId, @Valid @RequestBody SendMessageRequest request) {
        chatRoomService.sendMessage(roomId, request.participant(), request.message());
        return ApiResponse.success("Message sent successfully.");
    }

    @GetMapping("/{roomId}/messages")
    public MessageHistoryResponse getMessages(@PathVariable String roomId,
            @RequestParam(defaultValue = "50") @Min(0) @Max(500) int limit) {
        return new MessageHistoryResponse(chatRoomService.getRecentMessages(roomId, limit));
    }

    @GetMapping("/{roomId}/participants")
    public Set<String> getParticipants(@PathVariable String roomId) {
        return chatRoomService.getParticipants(roomId);
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse deleteRoom(@PathVariable String roomId) {
        if (!chatRoomService.deleteRoom(roomId)) {
            throw new com.example.chatapp.exception.ChatRoomNotFoundException(roomId);
        }
        return ApiResponse.success("Chatroom '" + roomId + "' deleted successfully.");
    }
}
