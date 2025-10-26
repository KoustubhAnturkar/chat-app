package org.example.chatservice.chatbackend.websockets;

import com.example.chat.proto.ChannelUpdate;
import com.example.chat.proto.ChatMessage;
import com.example.chat.proto.UserUpdate;
import org.example.chatservice.chatbackend.dto.ChannelUpdateDTO;
import org.example.chatservice.chatbackend.dto.ChatMessageDTO;
import org.example.chatservice.chatbackend.dto.UserUpdateDTO;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WebSocketBroadcastService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcastService.class.getSimpleName());
    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketBroadcastService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastMessage(String destination, ChatMessage message){
        log.info("Broadcasting message to destination: {}", destination);
        messagingTemplate.convertAndSend(destination, ChatMessageDTO.fromProto(message));
    }

    public void broadcastUserUpdate(String destination, UserUpdate userUpdate){
        log.info("Broadcasting user update to destination: {}", destination);
        messagingTemplate.convertAndSend(destination, UserUpdateDTO.fromProto(userUpdate));
    }

    public void broadcastChannelUpdate(String destination, ChannelUpdate channelUpdate){
        log.info("Broadcasting channel update to destination: {}", destination);
        messagingTemplate.convertAndSend(destination, ChannelUpdateDTO.fromProto(channelUpdate));
    }
}
