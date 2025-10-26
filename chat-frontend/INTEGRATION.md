# WebSocket Integration Guide

## Backend Endpoint Configuration

This frontend is configured to connect to your Spring Boot backend using STOMP over WebSocket with SockJS fallback.

### WebSocket Endpoints

Based on your backend configuration (`WebSocketBroadcastConfig.java`):

**Connection Endpoint:**
- Primary: `ws://localhost:8080/ws-chat`
- With SockJS: `http://localhost:8080/ws-chat`

**Message Broker:**
- Broker prefix: `/topic`
- Application prefix: `/app`

### Message Destinations

#### Subscribe (Client listening):
```
/topic/channel/{channelId}
```
Examples:
- `/topic/channel/general`
- `/topic/channel/random`
- `/topic/channel/tech`
- `/topic/channel/gaming`

#### Send (Client publishing):
```
/app/chat.send/{channelId}
```
Examples:
- `/app/chat.send/general`
- `/app/chat.send/random`

### Backend Controller Expected

Your Spring Boot backend should have a controller method like:

```java
@MessageMapping("/chat.send/{channelId}")
@SendTo("/topic/channel/{channelId}")
public ChatMessage handleMessage(@DestinationVariable String channelId,
                                 ChatMessage message) {
    // Process the message
    return message;
}
```

Or using the broadcast service:

```java
@MessageMapping("/chat.send/{channelId}")
public void handleMessage(@DestinationVariable String channelId,
                         ChatMessage message) {
    webSocketBroadcastService.broadcastMessage(
        "/topic/channel/" + channelId,
        message
    );
}
```

## Message Protocol

The frontend sends and receives messages in JSON format that matches your Protobuf schema:

### Outgoing Message (Frontend → Backend)
```json
{
  "messageId": "msg_abc123xyz456",
  "channel": {
    "channelId": "general",
    "name": "general",
    "description": "General discussion"
  },
  "sender": {
    "userId": "user_def789uvw012",
    "username": "john_doe",
    "displayName": "John Doe"
  },
  "body": "Hello everyone!",
  "type": "MESSAGE_TEXT",
  "timeStamp": 1698249600000
}
```

### Incoming Message (Backend → Frontend)
The frontend expects the same format. The backend should broadcast messages to:
- All subscribers on `/topic/channel/{channelId}`

## CORS Configuration

Make sure your backend has CORS properly configured:

```java
@Configuration
public class WebSocketBroadcastConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")  // For development
                .withSockJS();
    }
}
```

## Connection Flow

1. **User connects**: Frontend creates SockJS connection to `/ws-chat`
2. **STOMP handshake**: Establishes STOMP protocol over WebSocket
3. **Subscribe to channels**: Frontend subscribes to all 4 channels:
   - `/topic/channel/general`
   - `/topic/channel/random`
   - `/topic/channel/tech`
   - `/topic/channel/gaming`
4. **Send messages**: User sends to `/app/chat.send/{channelId}`
5. **Receive broadcasts**: Backend broadcasts to `/topic/channel/{channelId}`
6. **Display messages**: Frontend displays messages for active channel only

## Testing the Connection

### Using Browser DevTools

Open browser console to see:
- STOMP debug logs
- Connection status
- Message send/receive logs

### Expected Console Output

```
Initializing Chat App...
STOMP: Opening Web Socket...
STOMP: Web Socket Opened...
STOMP: >>> CONNECT
Connected: CONNECTED
WebSocket connected successfully
Subscribed to channel: general
Subscribed to channel: random
Subscribed to channel: tech
Subscribed to channel: gaming
```

### Sending Test Message

When you send "Hello", you should see:
```
Message sent: {messageId: "msg_...", channel: {...}, sender: {...}, ...}
```

### Receiving Messages

When a message arrives:
```
STOMP: <<< MESSAGE
Received message: {messageId: "msg_...", body: "Hello", ...}
```

## Customization

### Change Backend URL

Edit `js/config.js`:
```javascript
export const config = {
    WS_ENDPOINT: 'http://your-server:8080/ws-chat',
    // ...
};
```

### Add More Channels

Edit `js/app.js`:
```javascript
this.channels = [
    { id: 'general', name: 'general', description: 'General discussion' },
    { id: 'random', name: 'random', description: 'Random chat' },
    { id: 'tech', name: 'tech', description: 'Tech discussions' },
    { id: 'gaming', name: 'gaming', description: 'Gaming talk' },
    { id: 'music', name: 'music', description: 'Music lovers' }  // Add new channel
];
```

### Change Destination Paths

If your backend uses different paths, update `js/config.js`:
```javascript
export const config = {
    // ...
    TOPIC_PREFIX: '/topic/chat/',      // Instead of /topic/channel/
    APP_PREFIX: '/app/message.send/'   // Instead of /app/chat.send/
};
```

## Production Deployment

### Build for Production
```bash
npm run build
```

### Deploy Static Files
Upload `dist/` folder contents to your web server (Nginx, Apache, S3, etc.)

### Environment Variables
Consider using environment-specific configs:
- `config.dev.js` - Development settings
- `config.prod.js` - Production settings

## Security Considerations

1. **CORS**: Configure proper CORS in production (not wildcard `*`)
2. **Authentication**: Add JWT or session-based auth before going to production
3. **Rate Limiting**: Implement rate limiting on backend
4. **Input Validation**: Backend should validate all messages
5. **XSS Protection**: Frontend already escapes HTML in messages

## Monitoring

Watch for:
- Connection drops (auto-reconnect enabled with 5s delay)
- Message delivery failures
- High message volume on channels
- WebSocket connection limits

Check `WebSocketEventListener.java` logs for connection events.

