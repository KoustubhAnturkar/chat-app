package org.example.chatservice.dbpersistence.kafka;

import com.datastax.driver.core.ResultSetFuture;
import com.example.chat.proto.ChatMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.example.chatservice.dbpersistence.config.Config;
import org.example.chatservice.dbpersistence.scylla.ScyllaDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MessageWorker implements Runnable{
    private final Logger log;
    private final Integer startPartition;
    private final Integer totalPartitions;
    private final MessageConsumer consumer;
    private final ScyllaDB scyllaDB;

    public MessageWorker(Integer startPartition, Integer totalPartitions) {
        log = LoggerFactory.getLogger(MessageWorker.class.getSimpleName() + "-Partition-" + startPartition);
        this.startPartition = startPartition;
        this.totalPartitions = totalPartitions;
        this.consumer = new MessageConsumer();
        this.scyllaDB = new ScyllaDB();
    }

    @Override
    public void run() {
        consumer.subscribe("chat-stream", startPartition, totalPartitions);
        log.info("Started MessageWorker for partitions starting at {} to {}", startPartition, startPartition + totalPartitions);
        try {
            while (true) {
                final ConsumerRecords<String, byte[]> records = consumer.poll(Integer.parseInt(Config.getProperty("kafka.consumer.poll-timeout-ms", "1000")));
                List<ResultSetFuture> futures = new ArrayList<>();

                records.forEach(record -> {
                    log.info("Processing record with key: '{}', partition: {}, offset: {}",
                            record.key(), record.partition(), record.offset());
                    try {
                        ChatMessage message = ChatMessage.parseFrom(record.value());
                        log.info(message.toString());
                        ResultSetFuture resultSetFuture = scyllaDB.storeMessage(
                            message.getChannel().getChannelId(),
                            message.getMessageId(),
                            message.getMessageId(),
                            message.getBody(),
                            new Date(message.getTimeStamp())
                        );
                        futures.add(resultSetFuture);
                    } catch (InvalidProtocolBufferException e) {
                        log.error("Failed to parse ChatMessage from record at partition: {}, offset: {}, value: {}",
                                record.partition(), record.offset(), e.getMessage());
                    }
                });

                try {
                    for (ResultSetFuture future : futures) {
                        future.get();
                    }
                    consumer.commitSync();
                    log.info("Successfully stored {} messages and committed offsets", futures.size());
                } catch (Exception e) {
                    log.error("Error while waiting for futures to complete: {}", e.getMessage(), e);
                }
            }
        } catch (WakeupException e) {
            log.info("Shutdown signal received for MessageWorker starting at partition {}", startPartition);
        } catch (Exception e) {
            log.error("Unexpected error in MessageWorker starting at partition {}: {}", startPartition, e.getMessage(), e);
        } finally {
            log.info("Closing consumer for MessageWorker starting at partition {}", startPartition);
        }
    }

    public void shutdown(CountDownLatch countDownLatch){
        log.info("Shutdown initiated for MessageWorker starting at partition {}", startPartition);
        consumer.shutdown();
        countDownLatch.countDown();
    }
}
