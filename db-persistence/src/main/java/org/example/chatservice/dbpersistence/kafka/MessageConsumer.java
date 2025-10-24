package org.example.chatservice.dbpersistence.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.example.chatservice.dbpersistence.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
    private final KafkaConsumer<String, byte[]> kafkaConsumer;

    public MessageConsumer() {
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", Config.getProperty("kafka.consumer.bootstrap-servers"));
        consumerProps.put("group.id", Config.getProperty("kafka.consumer.group-id"));
        consumerProps.put("key.deserializer", Config.getProperty("kafka.consumer.key-deserializer"));
        consumerProps.put("value.deserializer", Config.getProperty("kafka.consumer.value-deserializer"));
        consumerProps.put("auto.offset.reset", Config.getProperty("kafka.consumer.auto-offset-reset", "earliest"));
        consumerProps.put("enable.auto.commit", Boolean.parseBoolean(Config.getProperty("kafka.consumer.enable-auto-commit", "false")));

        this.kafkaConsumer = new KafkaConsumer<String, byte[]>(consumerProps);
    }

    public void subscribe(String topic, Integer startPartition, Integer totalPartitions) {
        List<TopicPartition> partitions = new ArrayList<>();
        for (int partition = startPartition; partition < startPartition + totalPartitions; partition++) {
            partitions.add(new TopicPartition(topic, partition));
        }
        kafkaConsumer.assign(partitions);
        log.info("Subscribed to topic '{}' for partitions: {}", topic, partitions);
    }

    public ConsumerRecords<String, byte[]> poll(long timeout) {
        return kafkaConsumer.poll(java.time.Duration.ofMillis(timeout));
    }

    public void shutdown() {
        kafkaConsumer.wakeup();
    }

    public void commitSync(){
        kafkaConsumer.commitSync();
    }
}
