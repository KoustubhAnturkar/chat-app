# Quick Start Guide

## Starting the Application

### 1. Backend Setup
Make sure your Spring Boot backend server is running on port 8080 with WebSocket support.

### 2. Frontend Setup

Install dependencies (first time only):
```bash
npm install
```

Start the development server:
```bash
npm start
```

The application will open automatically at `http://localhost:3000`

## Using the Chat App

1. **Connect**: Enter your username and display name, then click "Connect"
2. **Select Channel**: Click on any channel in the sidebar (#general, #random, #tech, #gaming)
3. **Send Messages**: Type your message and press Enter or click the send button
4. **Switch Channels**: Click different channels to view and send messages in different topics
5. **Disconnect**: Click the lock icon in the top right to disconnect

## Configuration

Edit `js/config.js` to change:
- Backend WebSocket URL
- STOMP reconnection settings
- Topic and application prefixes

## Message Format

The app uses the protobuf schema defined in your backend:

```javascript
{
  messageId: "msg_...",
  channel: {
    channelId: "general",
    name: "general",
    description: "General discussion"
  },
  sender: {
    userId: "user_...",
    username: "john_doe",
    displayName: "John Doe"
  },
  body: "Hello world!",
  type: "MESSAGE_TEXT",
  timeStamp: 1234567890
}
```

## Features

✅ Real-time messaging via WebSocket
✅ Multiple channel support
✅ Discord-inspired dark theme
✅ Auto-reconnection on disconnect
✅ User presence indicators
✅ Message history per channel
✅ Responsive design

## Troubleshooting

**Can't connect?**
- Ensure backend is running on http://localhost:8080
- Check CORS settings in backend
- Verify WebSocket endpoint `/ws-chat` is accessible

**Messages not appearing?**
- Check browser console for errors
- Verify channel subscription destinations match backend
- Ensure message format matches protobuf schema

**Build errors?**
- Delete `node_modules` and run `npm install` again
- Clear webpack cache: `rm -rf .cache dist`

