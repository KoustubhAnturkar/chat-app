package org.example.chatservice.chatbackend.controller;

import org.example.chatservice.chatbackend.dto.ChatMessageDTO;
import org.example.chatservice.chatbackend.kafka.KafkaHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class.getSimpleName());
    private final SimpMessageSendingOperations messagingTemplate;
    private KafkaHandler kafkaHandler;

    public ChatController(SimpMessageSendingOperations messagingTemplate, KafkaHandler kafkaHandler) {
        this.kafkaHandler = kafkaHandler;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO messageRaw) throws Exception {
        log.info("Broadcasting message to channel {}", messageRaw.getChannel().getChannelId());
        kafkaHandler.processMessage(messageRaw);
    }
}
