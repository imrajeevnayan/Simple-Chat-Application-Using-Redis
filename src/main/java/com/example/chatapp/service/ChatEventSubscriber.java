package com.example.chatapp.service;

import com.example.chatapp.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class ChatEventSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(ChatEventSubscriber.class);
    private final ObjectMapper objectMapper;

    public ChatEventSubscriber(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getBody(), ChatMessage.class);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            log.info("Received chat event on {}: participant={}, timestamp={}", channel,
                    chatMessage.participant(), chatMessage.timestamp());
        } catch (Exception exception) {
            log.error("Could not deserialize a Redis chat event", exception);
        }
    }
}
