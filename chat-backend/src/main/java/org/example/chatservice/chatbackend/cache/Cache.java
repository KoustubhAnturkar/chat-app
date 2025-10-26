package org.example.chatservice.chatbackend.cache;

import com.example.chat.proto.Channel;
import com.example.chat.proto.User;
import org.example.chatservice.chatbackend.scylla.ScyllaDB;

import java.util.HashMap;
import java.util.Map;

public class Cache {
    public static Map<String, Channel> channelCache = new HashMap<>();
    public static Map<String, Channel> channelNameCache = new HashMap<>();
    public static Map<String, User> userCache = new HashMap<>();
    public static Map<String, User> userNameCache = new HashMap<>();

    private Cache() {
        // Private constructor to prevent instantiation
    }

    public static void populateChannelCache(ScyllaDB scyllaDB) {
        scyllaDB.populateChannelsPaging();
    }

    public static void populateUserCache(ScyllaDB scyllaDB) {
        scyllaDB.populateUsersPaging();
    }
}
