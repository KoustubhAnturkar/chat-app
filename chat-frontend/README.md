# Chat App Frontend - Discord Style

A modern, Discord-inspired chat application frontend that connects to a Spring Boot backend using STOMP over WebSocket with SockJS fallback.

## Features

- 🎨 Discord-inspired dark theme UI
- 🔌 WebSocket connection using STOMP protocol with SockJS
- 💬 Real-time messaging across multiple channels
- 👥 User presence and status indicators
- 📱 Responsive design
- 🔄 Automatic reconnection handling
- 📡 Subscribe to multiple channels simultaneously

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn
- Backend server running on `http://localhost:8080`

## Installation

1. Install dependencies:
```bash
npm install
```

## Development

Start the development server:
```bash
npm start
```

This will:
- Start webpack dev server on `http://localhost:3000`
- Enable hot module replacement
- Automatically open the browser

## Build for Production

Create an optimized production build:
```bash
npm run build
```

The built files will be in the `dist/` directory.

## WebSocket Configuration

The app connects to the backend WebSocket endpoint:
- **Endpoint**: `http://localhost:8080/ws-chat`
- **Protocol**: STOMP over SockJS
- **Subscribe destination**: `/topic/channel/{channelId}`
- **Send destination**: `/app/chat.send/{channelId}`

### Message Format

Messages follow the protobuf schema defined in the backend:

```javascript
{
  messageId: string,
  channel: {
    channelId: string,
    name: string,
    description: string
  },
  sender: {
    userId: string,
    username: string,
    displayName: string
  },
  body: string,
  type: "MESSAGE_TEXT" | "MESSAGE_IMAGE",
  timeStamp: number
}
```

## Project Structure

```
chat-frontend/
├── css/
│   └── style.css          # Discord-inspired styles
├── js/
│   └── app.js             # Main application logic
├── index.html             # HTML template
├── package.json           # Dependencies
├── webpack.common.js      # Common webpack config
├── webpack.config.dev.js  # Development config
└── webpack.config.prod.js # Production config
```

## Usage

1. Start your backend server (must be running on port 8080)
2. Start the frontend development server: `npm start`
3. Enter your username and display name
4. Click "Connect" to establish WebSocket connection
5. Select a channel from the sidebar
6. Start chatting!

## Available Channels

The app comes with 4 default channels:
- **#general** - General discussion
- **#random** - Random chat
- **#tech** - Tech discussions
- **#gaming** - Gaming talk

## Troubleshooting

### WebSocket Connection Issues

If you see connection errors:
1. Ensure the backend server is running on `http://localhost:8080`
2. Check that CORS is properly configured in the backend
3. Verify the WebSocket endpoint `/ws-chat` is accessible
4. Check browser console for detailed error messages

### Cannot Send Messages

If messages aren't sending:
1. Verify you're connected (green status indicator)
2. Check the browser console for errors
3. Ensure the STOMP destination paths match the backend configuration

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)

## Technologies Used

- **@stomp/stompjs**: STOMP client for WebSocket
- **sockjs-client**: SockJS client for WebSocket fallback
- **Webpack**: Module bundler
- **Babel**: JavaScript transpiler
- **CSS3**: Modern styling with CSS variables

## License

See LICENSE.txt

