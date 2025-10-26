package org.example.chatservice.chatbackend.kafka;

import com.example.chat.proto.ChannelUpdate;
import com.example.chat.proto.ChatMessage;
import com.example.chat.proto.UserUpdate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.chatservice.chatbackend.websockets.WebSocketBroadcastService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class.getSimpleName());
    private final WebSocketBroadcastService webSocketBroadcastService;
    private static final String CHAT_TOPIC = "chat-stream";
    private static final String USER_UPDATES_TOPIC = "user-updates";
    private static final String CHANNEL_UPDATES_TOPIC = "channel-updates";
    private static final String GROUP_ID = "chat-backend-live-updates-group";

    public KafkaConsumer(WebSocketBroadcastService webSocketBroadcastService) {
        this.webSocketBroadcastService = webSocketBroadcastService;
    }

    @KafkaListener(topics = CHAT_TOPIC, groupId = GROUP_ID)
    public void listen(ConsumerRecord<String, byte[]> record) {
        log.info("Received message from Kafka on partition: {} and offset: {}", record.partition(), record.offset());
        try {
            ChatMessage message = ChatMessage.parseFrom(record.value());
            String destination = "/topic/channel/" + message.getChannel().getChannelId();
            webSocketBroadcastService.broadcastMessage(destination, message);
        } catch (Exception e) {
            log.error("Failed to parse ChatMessage from record at partition: {}, offset: {}, error: {}",
                    record.partition(), record.offset(), e.getMessage());
        }
    }

    @KafkaListener(topics = USER_UPDATES_TOPIC, groupId = GROUP_ID)
    public void listenUserUpdates(ConsumerRecord<String, byte[]> record) {
        log.info("Received user update from Kafka on partition: {} and offset: {}", record.partition(), record.offset());
        try{
            UserUpdate userUpdate = UserUpdate.parseFrom(record.value());
            String destination = "/topic/users";
            webSocketBroadcastService.broadcastUserUpdate(destination, userUpdate);
        } catch (Exception e) {
            log.error("Failed to parse UserUpdate from record at partition: {}, offset: {}, error: {}",
                    record.partition(), record.offset(), e.getMessage());
        }
    }

    @KafkaListener(topics = CHANNEL_UPDATES_TOPIC, groupId = GROUP_ID)
    public void listenChannelUpdates(ConsumerRecord<String, byte[]> record) {
        log.info("Received channel update from Kafka on partition: {} and offset: {}", record.partition(), record.offset());
        try {
            ChannelUpdate channelUpdate = ChannelUpdate.parseFrom(record.value());
            String destination = "/topic/channels";
            webSocketBroadcastService.broadcastChannelUpdate(destination, channelUpdate);
        } catch (Exception e) {
            log.error("Failed to parse ChannelUpdate from record at partition: {}, offset: {}, error: {}",
                    record.partition(), record.offset(), e.getMessage());
        }
    }
}
