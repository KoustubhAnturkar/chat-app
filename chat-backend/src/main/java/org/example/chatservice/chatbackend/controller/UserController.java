package org.example.chatservice.chatbackend.controller;

import com.datastax.driver.core.ResultSet;
import com.example.chat.proto.User;
import com.example.chat.proto.UserUpdate;
import com.example.chat.proto.UserUpdateType;
import org.example.chatservice.chatbackend.cache.Cache;
import org.example.chatservice.chatbackend.kafka.KafkaHandler;
import org.example.chatservice.chatbackend.scylla.ScyllaDB;
import org.example.chatservice.chatbackend.dto.UserDTO;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final ScyllaDB scyllaDB;
    private final KafkaHandler kafkaHandler;

    public UserController(ScyllaDB scyllaDB, KafkaHandler kafkaHandler) {
        this.scyllaDB = scyllaDB;
        Cache.populateUserCache(scyllaDB);
        this.kafkaHandler = kafkaHandler;
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> createUser(@RequestParam("username") String username, @RequestParam("displayName") String displayName) {
        log.info("Creating user: {}", username);

        if(Cache.userNameCache.containsKey(username)) {     // TODO: Handle case sensitivity and duplicate usernames. For now, just return existing user. Later we'll add authentication.
            log.warn("User already exists in cache: {}", username);
            User user = Cache.userNameCache.get(username);
            return getMapResponseEntity(user);
        }

        User user = User.newBuilder()
                .setUsername(username)
                .setDisplayName(displayName)
                .setUserId(UUID.randomUUID().toString())  // TODO: Replace with a hash function to generate consistent user IDs for same names
                .build();

        String userId = user.getUserId();
        ResultSet resultSet = scyllaDB.createUser(userId, username, displayName);
        log.info("User created in ScyllaDB: {}", resultSet.wasApplied());

        Cache.userCache.put(userId, user);
        Cache.userNameCache.put(username, user);

        UserUpdate userUpdate = UserUpdate.newBuilder()
                .setUser(user)
                .setType(UserUpdateType.NEW_USER)
                .build();
        kafkaHandler.sendUserUpdate(userUpdate);

        return getMapResponseEntity(user);
    }

    @NotNull
    private ResponseEntity<Map<String, Object>> getMapResponseEntity(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("userId", user.getUserId());
        response.put("username", user.getUsername());
        response.put("displayName", user.getDisplayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        log.info("Fetching user with ID: {}", userId);
        User user = Cache.userCache.get(userId);

        return getResponseEntity(user);
    }

    @NotNull
    private ResponseEntity<?> getResponseEntity(User user) {
        if (user == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Channel not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        return ResponseEntity.ok(UserDTO.fromProto(user));
    }


    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user with username: {}", username);
        User user = Cache.userNameCache.get(username);
        return getResponseEntity(user);
    }

    @GetMapping("/all")
    public @NotNull ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("Fetching all users");
        List<UserDTO> users = Cache.userCache.values().stream()
                .map(UserDTO::fromProto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
