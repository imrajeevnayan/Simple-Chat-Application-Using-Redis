package com.example.chatapp.service;

import com.example.chatapp.exception.ChatRoomAlreadyExistsException;
import com.example.chatapp.exception.ChatRoomNotFoundException;
import com.example.chatapp.model.ChatMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
public class ChatRoomService {
    private static final String ROOM_PREFIX = "chat:room:";
    private final RedisTemplate<String, ChatMessage> redis;
    private final StringRedisTemplate strings;
    private final ChannelTopic topic;

    public ChatRoomService(RedisTemplate<String, ChatMessage> redis, StringRedisTemplate strings, ChannelTopic topic) {
        this.redis = redis;
        this.strings = strings;
        this.topic = topic;
    }

    public void createRoom(String roomId) {
        String metadataKey = metadataKey(roomId);
        Long created = strings.execute((RedisCallback<Long>) connection -> connection.hashCommands().hSetNX(
                strings.getStringSerializer().serialize(metadataKey),
                strings.getStringSerializer().serialize("roomId"),
                strings.getStringSerializer().serialize(roomId)) ? 1L : 0L);
        if (!Long.valueOf(1).equals(created)) {
            throw new ChatRoomAlreadyExistsException(roomId);
        }
        redis.opsForHash().put(metadataKey, "createdAt", Instant.now().toString());
    }

    public void joinRoom(String roomId, String participant) {
        requireRoom(roomId);
        strings.opsForSet().add(participantsKey(roomId), participant);
    }

    public void sendMessage(String roomId, String participant, String body) {
        requireRoom(roomId);
        ChatMessage message = new ChatMessage(participant, body, Instant.now());
        redis.opsForList().rightPush(messagesKey(roomId), message);
        redis.convertAndSend(topic.getTopic() + ":" + roomId, message);
    }

    public List<ChatMessage> getRecentMessages(String roomId, int limit) {
        requireRoom(roomId);
        if (limit == 0) return List.of();
        List<ChatMessage> newestFirst = redis.opsForList().range(messagesKey(roomId), -limit, -1);
        if (newestFirst == null || newestFirst.isEmpty()) return List.of();
        return new ArrayList<>(newestFirst);
    }

    public Set<String> getParticipants(String roomId) {
        requireRoom(roomId);
        Set<String> participants = strings.opsForSet().members(participantsKey(roomId));
        return participants == null ? Set.of() : participants;
    }

    public boolean deleteRoom(String roomId) {
        if (!roomExists(roomId)) return false;
        redis.delete(List.of(metadataKey(roomId), participantsKey(roomId), messagesKey(roomId)));
        return true;
    }

    private void requireRoom(String roomId) {
        if (!roomExists(roomId)) throw new ChatRoomNotFoundException(roomId);
    }

    private boolean roomExists(String roomId) {
        return Boolean.TRUE.equals(redis.hasKey(metadataKey(roomId)));
    }

    private String metadataKey(String roomId) { return ROOM_PREFIX + roomId + ":metadata"; }
    private String participantsKey(String roomId) { return ROOM_PREFIX + roomId + ":participants"; }
    private String messagesKey(String roomId) { return ROOM_PREFIX + roomId + ":messages"; }
}
