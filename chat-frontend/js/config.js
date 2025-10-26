// Configuration for the chat application
export const config = {
    // Backend WebSocket endpoint
    WS_ENDPOINT: 'http://localhost:8080/ws-chat',

    // REST API base URL
    REST_API_BASE_URL: 'http://localhost:8080/api/v1',

    // STOMP configuration
    STOMP_RECONNECT_DELAY: 5000,
    STOMP_HEARTBEAT_INCOMING: 4000,
    STOMP_HEARTBEAT_OUTGOING: 4000,

    // Application settings
    DEFAULT_CHANNEL: 'general',

    // Subscription destinations
    TOPIC_PREFIX: '/topic/channel/',
    CHANNEL_UPDATE_TOPIC: '/topic/channels',
    USER_UPDATE_TOPIC: '/topic/users',
    APP_PREFIX: '/app/chat.sendMessage'
};

