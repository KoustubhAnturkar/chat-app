package org.example.chatservice.chatbackend.kafka;

import com.example.chat.proto.Channel;
import com.example.chat.proto.ChannelUpdate;
import com.example.chat.proto.UserUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.example.chat.proto.ChatMessage;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class.getSimpleName());

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String CHAT_TOPIC = "chat-stream";
    private static final String USER_UPDATES_TOPIC = "user-updates";
    private static final String CHANNEL_UPDATES_TOPIC = "channel-updates";

    public void sendMessage(Channel channel, ChatMessage message){
        log.info("Sent Message on Topic: {}", CHAT_TOPIC);
        kafkaTemplate.send(CHAT_TOPIC, channel.getChannelId(), message.toByteArray());
    }

    public void sendUserUpdate(UserUpdate userUpdate){
        log.info("Sent User Update on Topic: {}", USER_UPDATES_TOPIC);
        kafkaTemplate.send(USER_UPDATES_TOPIC, userUpdate.getUser().getUserId(), userUpdate.toByteArray());
    }

    public void sendChannelUpdate(ChannelUpdate channelUpdate){
        log.info("Sent Channel Update on Topic: {}", CHANNEL_UPDATES_TOPIC);
        kafkaTemplate.send(CHANNEL_UPDATES_TOPIC, channelUpdate.getChannel().getChannelId(), channelUpdate.toByteArray());
    }
}
