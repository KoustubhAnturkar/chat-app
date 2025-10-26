package org.example.chatservice.chatbackend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic chatStreamTopic(){
        return TopicBuilder.name("chat-stream").partitions(15).build();
    }

    public NewTopic userUpdatesTopic(){
        return TopicBuilder.name("user-updates").partitions(3).build();
    }

    public NewTopic channelUpdatesTopic(){
        return TopicBuilder.name("channel-updates").partitions(3).build();
    }
}
