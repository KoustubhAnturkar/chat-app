package org.example.chatservice.chatbackend.scylla;

import com.datastax.driver.core.*;
import com.example.chat.proto.Channel;
import com.example.chat.proto.ChatMessage;
import com.example.chat.proto.User;
import org.example.chatservice.chatbackend.cache.Cache;
import org.example.chatservice.chatbackend.dto.ChatMessageDTO;
import org.example.chatservice.chatbackend.scylla.session.ScyllaSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ScyllaDB {
    private static final Logger log = LoggerFactory.getLogger(ScyllaDB.class);
    private final Session session;
    private final Map<String, PreparedStatement> preparedStatements = new HashMap<>();

    public ScyllaDB(ScyllaSession scyllaSession) {
        session = scyllaSession.getSession();
        createTablesIfNotExists();
        createPreparedStatements();
    }

    private void createTablesIfNotExists(){
        // messages table
        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                "channel_id text," +
                "message_id text," +
                "sender text," +
                "body text," +
                "created_at timestamp," +
                "PRIMARY KEY (channel_id, created_at)" +
                ") WITH CLUSTERING ORDER BY (created_at DESC);";
        session.execute(createMessagesTable);

        // Channel table
        String createChannelTable = "CREATE TABLE IF NOT EXISTS channels (" +
                "channel_id text," +
                "created_at timestamp," +
                "name text," +
                "description text," +
                "PRIMARY KEY (channel_id)" +
                ");";
        session.execute(createChannelTable);

        // Users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id text," +
                "username text," +
                "display_name text," +
                "created_at timestamp," +
                "PRIMARY KEY (user_id)" +
                ");";

        session.execute(createUsersTable);
    }

    private void createPreparedStatements() {
        // Create Channel prepared statement
        String createChannelCQL = "INSERT INTO channels (channel_id, created_at, name, description) VALUES (?, ?, ?, ?);";
        preparedStatements.put("createChannel", session.prepare(createChannelCQL));

        // Create User prepared statement
        String createUserCQL = "INSERT INTO users (user_id, username, display_name, created_at) VALUES (?, ?, ?, ?);";
        preparedStatements.put("createUser", session.prepare(createUserCQL));

        String getMessagesByChannelCQL = "SELECT * FROM messages WHERE channel_id = ?;";
        preparedStatements.put("getMessagesByChannel", session.prepare(getMessagesByChannelCQL));
    }

    public ResultSet createChannel(String channelId, String name, String description) {
        PreparedStatement ps = preparedStatements.get("createChannel");
        BoundStatement bound = ps.bind(
                channelId,
                new Date(),
                name,
                description
        );
        return session.execute(bound);
    }

    public ResultSet createUser(String userId, String username, String displayName) {
        PreparedStatement ps = preparedStatements.get("createUser");
        BoundStatement bound = ps.bind(
                userId,
                username,
                displayName,
                new Date()
        );
        return session.execute(bound);
    }

    public void populateUsersPaging(){
        Statement statement = new SimpleStatement("SELECT * FROM users;");
        boolean stop = false;
        int pageSize = 500;
        statement.setFetchSize(pageSize);
        while(!stop){
            ResultSet resultSet = session.execute(statement);
            for (Row row : resultSet) {
                String userId = row.getString("user_id");
                String username = row.getString("username");
                String displayName = row.getString("display_name");

                User user = User.newBuilder()
                        .setUserId(userId)
                        .setUsername(username)
                        .setDisplayName(displayName)
                        .build();

                Cache.userCache.put(userId, user);
                Cache.userNameCache.put(username, user);
            }
            if (resultSet.getExecutionInfo().getPagingState() != null) {
                statement.setPagingState(resultSet.getExecutionInfo().getPagingState());
            } else {
                stop = true;
            }
        }
    }

    public void populateChannelsPaging(){
        Statement statement = new SimpleStatement("SELECT * FROM channels;");
        boolean stop = false;
        int pageSize = 500;
        statement.setFetchSize(pageSize);
        while(!stop){
            ResultSet resultSet = session.execute(statement);
            for (Row row : resultSet) {
                String channelId = row.getString("channel_id");
                String name = row.getString("name");
                String description = row.getString("description");

                Channel channel = Channel.newBuilder()
                        .setChannelId(channelId)
                        .setName(name)
                        .setDescription(description)
                        .build();
                Cache.channelCache.put(channelId, channel);
                Cache.channelNameCache.put(name, channel);
            }
            if (resultSet.getExecutionInfo().getPagingState() != null) {
                statement.setPagingState(resultSet.getExecutionInfo().getPagingState());
            } else {
                stop = true;
            }
        }
    }

    public Map<Channel, ArrayList<ChatMessage>> getMessagesByChannel(String channelId){
        PreparedStatement ps = preparedStatements.get("getMessagesByChannel");
        BoundStatement bound = ps.bind(channelId);
        ResultSet resultSet = session.execute(bound);
        ArrayList<ChatMessage> messages = new ArrayList<>();
        Channel channel = Cache.channelCache.get(channelId);
        for (Row row : resultSet) {
            String messageId = row.getString("message_id");
            String sender = row.getString("sender");
            String body = row.getString("body");
            Date createdAt = row.getTimestamp("created_at");

            User user = Cache.userCache.getOrDefault(sender, User.newBuilder()
                    .setUsername(sender)
                    .setDisplayName("Unknown")
                    .setUserId("unknown")
                    .build());

            ChatMessage chatMessage = ChatMessage.newBuilder()
                    .setMessageId(messageId)
                    .setChannel(channel)
                    .setSender(user)
                    .setBody(body)
                    .setTimeStamp(createdAt.getTime())
                    .build();
            messages.add(chatMessage);
        }
        Map<Channel, ArrayList<ChatMessage>> channelMessagesMap = new HashMap<>();
        channelMessagesMap.put(channel, messages);
        return channelMessagesMap;
    }
}
