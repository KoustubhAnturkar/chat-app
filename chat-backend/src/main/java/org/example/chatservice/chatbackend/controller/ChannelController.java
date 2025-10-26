package org.example.chatservice.chatbackend.controller;

import com.datastax.driver.core.ResultSet;
import com.example.chat.proto.Channel;
import com.example.chat.proto.ChannelUpdate;
import com.example.chat.proto.ChannelUpdateType;
import org.example.chatservice.chatbackend.cache.Cache;
import org.example.chatservice.chatbackend.dto.ChannelDTO;
import org.example.chatservice.chatbackend.kafka.KafkaHandler;
import org.example.chatservice.chatbackend.scylla.ScyllaDB;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/channel")
public class ChannelController {
    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    private final ScyllaDB scyllaDB;
    private final KafkaHandler kafkaHandler;

    public ChannelController(ScyllaDB scyllaDB, KafkaHandler kafkaHandler) {
        this.scyllaDB = scyllaDB;
        Cache.populateChannelCache(scyllaDB);
        this.kafkaHandler = kafkaHandler;
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> createChannel(@RequestParam("name") String channelName, @RequestBody String description) {
        log.info("Creating channel: {}", channelName);

        if(Cache.channelNameCache.containsKey(channelName)){
            log.warn("Channel already exists in cache: {}", channelName);
            Channel channel = Cache.channelNameCache.get(channelName);
            return getMapResponseEntity(channel);
        }

        Channel channel = Channel.newBuilder()
                .setName(channelName)
                .setDescription(description)
                .setChannelId(UUID.randomUUID().toString())     //TODO: Replace with a hash function to generate consistent channel IDs for same names
                .build();
        ResultSet resultSet = scyllaDB.createChannel(channel.getChannelId(), channel.getName(), channel.getDescription());
        log.info("Channel created in ScyllaDB: {}", resultSet.wasApplied());

        Cache.channelCache.put(channel.getChannelId(), channel);
        Cache.channelNameCache.put(channelName, channel);

        ChannelUpdate channelUpdate = ChannelUpdate.newBuilder()
                .setType(ChannelUpdateType.NEW_CHANNEL)
                .setChannel(channel)
                .build();
        kafkaHandler.sendChannelUpdate(channelUpdate);

        return getMapResponseEntity(channel);
    }

    @NotNull
    public ResponseEntity<Map<String, Object>> getMapResponseEntity(Channel channel) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Channel created successfully");
        response.put("channelId", channel.getChannelId());
        response.put("name", channel.getName());
        response.put("description", channel.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<?> getChannel(@PathVariable String channelId) {
        log.info("Fetching channel with ID: {}", channelId);

        Channel channel = Cache.channelCache.get(channelId);

        return getResponseEntity(channel);
    }

    @GetMapping("/name/{channelName}")
    public ResponseEntity<?> getChannelByName(@PathVariable String channelName) {
        log.info("Fetching channel with name: {}", channelName);

        Channel channel = Cache.channelNameCache.get(channelName);

        return getResponseEntity(channel);
    }

    @NotNull
    private ResponseEntity<?> getResponseEntity(Channel channel) {
        if (channel == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Channel not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        return ResponseEntity.ok(ChannelDTO.fromProto(channel));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ChannelDTO>> getAllChannels() {
        log.info("Fetching all channels");

        List<ChannelDTO> channels = Cache.channelCache.values().stream()
                .map(ChannelDTO::fromProto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(channels);
    }
}
