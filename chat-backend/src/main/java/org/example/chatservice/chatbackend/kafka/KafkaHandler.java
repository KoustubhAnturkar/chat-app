package org.example.chatservice.chatbackend.kafka;


import com.example.chat.proto.*;
import org.example.chatservice.chatbackend.cache.Cache;
import org.example.chatservice.chatbackend.dto.ChatMessageDTO;
import org.springframework.stereotype.Component;

import java.util.Date;
@Component
public class KafkaHandler {
    private final KafkaProducer kafkaProducer;

    public KafkaHandler(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    private void sendMessage(ChatMessageDTO message, KafkaProducer kafkaProducer) {
        sendMessage(message.toProto(), kafkaProducer);
    }

    private void sendMessage(ChatMessage message, KafkaProducer kafkaProducer) {
        kafkaProducer.sendMessage(message.getChannel(), message);
    }

    public ChatMessage processMessage(String userName, String channelName,String body) {
        Channel channel = Cache.channelNameCache.get(channelName);

        User user = Cache.userNameCache.get(userName);

        if(channel == null){
            return ChatMessage.newBuilder().build();
        }
        if(user == null){
            return ChatMessage.newBuilder().build();
        }

        ChatMessage chatMessage = ChatMessage.newBuilder()
                .setMessageId(java.util.UUID.randomUUID().toString())
                .setChannel(channel)
                .setSender(user)
                .setBody(body)
                .setTimeStamp(new Date().getTime())
                .build();

        sendMessage(chatMessage, kafkaProducer);
        return chatMessage;
    }

    public ChatMessage processMessage(ChatMessageDTO chatMessageDTO) {
        processMessage(chatMessageDTO.getSender().getUsername(),
                chatMessageDTO.getChannel().getName(),
                chatMessageDTO.getBody());
        return chatMessageDTO.toProto();
    }

    public void sendUserUpdate(UserUpdate userUpdate){
        kafkaProducer.sendUserUpdate(userUpdate);
    }

    public void sendChannelUpdate(ChannelUpdate channelUpdate){
        kafkaProducer.sendChannelUpdate(channelUpdate);
    }
}
