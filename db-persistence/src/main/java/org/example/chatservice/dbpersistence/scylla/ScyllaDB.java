package org.example.chatservice.dbpersistence.scylla;

import com.datastax.driver.core.*;
import org.example.chatservice.dbpersistence.config.Config;
import org.example.chatservice.dbpersistence.scylla.session.ScyllaSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ScyllaDB {
    private static final Logger log = LoggerFactory.getLogger(ScyllaDB.class);
    private final Session session;
    private final Map<String, PreparedStatement> preparedStatements = new HashMap<>();

    public ScyllaDB() {
        session = ScyllaSession.getSession();
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
        // storeMessage prepared statement
        String storeMessageCQL = "INSERT INTO messages (channel_id, message_id, sender, body, created_at) VALUES (?, ?, ?, ?, ?);";
        preparedStatements.put("storeMessage", session.prepare(storeMessageCQL));
    }

    public ResultSetFuture storeMessage(String channelId, String messageId, String sender, String body, Date timestamp){
        PreparedStatement storedMessageStmt = preparedStatements.get("storeMessage");
        BoundStatement boundStatement = storedMessageStmt.bind(
                channelId,
                messageId,
                sender,
                body,
                timestamp
        );
        return session.executeAsync(boundStatement);
    }

}
