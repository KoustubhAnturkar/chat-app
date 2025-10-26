package org.example.chatservice.chatbackend.controller;

import com.example.chat.proto.Channel;
import com.example.chat.proto.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.example.chatservice.chatbackend.cache.Cache;
import org.example.chatservice.chatbackend.dto.ChatMessageDTO;
import org.example.chatservice.chatbackend.kafka.KafkaHandler;
import org.example.chatservice.chatbackend.scylla.ScyllaDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/message")
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    private final ScyllaDB scyllaDB;
    private final KafkaHandler kafkaHandler;

    public MessageController( ScyllaDB scyllaDB, KafkaHandler kafkaHandler) {
        this.scyllaDB = scyllaDB;
        this.kafkaHandler = kafkaHandler;
    }

    @PostMapping("/{channel}/send")
    public ResponseEntity<Map<String, Object>> send(@RequestParam("user") String userName, @PathVariable("channel") String channelName, @RequestBody String body){

        ChatMessage chatMessage = kafkaHandler.processMessage(userName, channelName, body);
        if(chatMessage.getMessageId().isEmpty()){
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message. Invalid user or channel.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("status", "OK");
        successResponse.put("messageId", chatMessage.getMessageId());
        successResponse.put("timestamp", chatMessage.getTimeStamp());

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/{channelId}/history")
    public ResponseEntity<Map<String, Object>> getMessageHistory(@PathVariable("channelId") String channelId) {
        log.info("Fetching message history for channel ID: {}", channelId);
        Channel channel = Cache.channelCache.get(channelId);
        if (channel == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Channel not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<Channel, ArrayList<ChatMessage>> messagesMap = scyllaDB.getMessagesByChannel(channelId);
        ArrayList<ChatMessageDTO> messageDTOs = new ArrayList<>();
        if (messagesMap.containsKey(channel)) {
            for (ChatMessage message : messagesMap.get(channel)) {
                messageDTOs.add(ChatMessageDTO.fromProto(message));
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("channelId", channelId);
        response.put("messages", messageDTOs);
        return ResponseEntity.ok(response);
    }

}
