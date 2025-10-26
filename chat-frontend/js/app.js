import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import '../css/style.css';
import { config } from './config.js';

class ChatApp {
    constructor() {
        this.stompClient = null;
        this.currentUser = null;
        this.currentChannel = {};
        this.channels = [

        ];
        this.subscriptions = new Map();
        this.channelMessages = new Map();
        this.users = new Map();
        this.connected = false;

        this.loadUsers();
        this.loadChannels().then(() => {
            // Load Messages for all channels
            this.channels.forEach(channel => {
                this.loadMessages(channel.id);
            });
            this.initializeUI();
        });

    }

    initializeUI() {
        // Get DOM elements
        this.elements = {
            connectionModal: document.getElementById('connectionModal'),
            userForm: document.getElementById('userForm'),
            usernameInput: document.getElementById('username'),
            displayNameInput: document.getElementById('displayName'),
            connectionStatus: document.getElementById('connectionStatus'),
            chatContainer: document.getElementById('chatContainer'),
            channelsList: document.getElementById('channelsList'),
            currentChannelName: document.getElementById('currentChannelName'),
            currentUserName: document.getElementById('currentUserName'),
            userAvatar: document.getElementById('userAvatar'),
            messagesWrapper: document.getElementById('messagesWrapper'),
            messagesContainer: document.getElementById('messagesContainer'),
            messageForm: document.getElementById('messageForm'),
            messageInput: document.getElementById('messageInput'),
            disconnectBtn: document.getElementById('disconnectBtn'),
            connectionIndicator: document.getElementById('connectionIndicator'),
            createChannelModal: document.getElementById('createChannelModal'),
            addChannelBtn: document.getElementById('addChannelBtn'),
            createChannelForm: document.getElementById('createChannelForm'),
            channelNameInput: document.getElementById('channelName'),
            channelDescriptionInput: document.getElementById('channelDescription'),
            closeChannelModal: document.getElementById('closeChannelModal'),
            cancelChannelBtn: document.getElementById('cancelChannelBtn'),
            createChannelStatus: document.getElementById('createChannelStatus')
        };

        // Attach event listeners
        this.elements.userForm.addEventListener('submit', (e) => this.handleUserFormSubmit(e));
        this.elements.messageForm.addEventListener('submit', (e) => this.handleMessageFormSubmit(e));
        this.elements.disconnectBtn.addEventListener('click', () => this.disconnect());
        this.elements.addChannelBtn.addEventListener('click', () => this.openCreateChannelModal());
        this.elements.createChannelForm.addEventListener('submit', (e) => this.handleCreateChannelSubmit(e));
        this.elements.closeChannelModal.addEventListener('click', () => this.closeCreateChannelModal());
        this.elements.cancelChannelBtn.addEventListener('click', () => this.closeCreateChannelModal());

        // Render channels
        this.renderChannels();
    }

    renderChannels() {
        this.elements.channelsList.innerHTML = '';
        this.channels.forEach(channel => {
            const channelElement = document.createElement('div');
            channelElement.className = 'channel-item';
            if (channel.id === this.currentChannel.id) {
                channelElement.classList.add('active');
            }
            channelElement.innerHTML = `
                <span class="hashtag">#</span>
                <span>${channel.name}</span>
            `;
            channelElement.addEventListener('click', () => this.switchChannel(channel));
            this.elements.channelsList.appendChild(channelElement);
        });
    }

    async handleUserFormSubmit(event) {
        event.preventDefault();

        const username = this.elements.usernameInput.value.trim();
        const displayName = this.elements.displayNameInput.value.trim();

        if (!username || !displayName) {
            this.showConnectionStatus('Please enter both username and display name', 'error');
            return;
        }

        await this.generateUser(username, displayName);

        this.showConnectionStatus('Connecting to server...', 'success');
        await this.connect();
    }

    async connect() {
        try {
            // Create STOMP client with SockJS
            this.stompClient = new Client({
                webSocketFactory: () => new SockJS(config.WS_ENDPOINT),
                reconnectDelay: config.STOMP_RECONNECT_DELAY,
                heartbeatIncoming: config.STOMP_HEARTBEAT_INCOMING,
                heartbeatOutgoing: config.STOMP_HEARTBEAT_OUTGOING,
                debug: (str) => {
                    console.log('STOMP: ' + str);
                },
                onConnect: (frame) => {
                    console.log('Connected: ' + frame);
                    this.onConnected();
                },
                onStompError: (frame) => {
                    console.error('Broker reported error: ' + frame.headers['message']);
                    console.error('Additional details: ' + frame.body);
                    this.onError(frame);
                },
                onWebSocketClose: (event) => {
                    console.log('WebSocket closed', event);
                    this.onDisconnected();
                },
                onWebSocketError: (event) => {
                    console.error('WebSocket error', event);
                    this.showConnectionStatus('WebSocket connection error', 'error');
                }
            });

            // Activate the client
            this.stompClient.activate();

        } catch (error) {
            console.error('Error connecting to WebSocket:', error);
            this.showConnectionStatus('Failed to connect: ' + error.message, 'error');
        }
    }

    onConnected() {
        console.log('WebSocket connected successfully');
        this.connected = true;

        // Subscribe to all channels
        this.channels.forEach(channel => {
            this.subscribeToChannel(channel.id);
        });
        this.subscribeToChannelUpdate();

        // Update UI
        this.elements.connectionModal.style.display = 'none';
        this.elements.chatContainer.style.display = 'flex';
        this.elements.messageInput.disabled = false;
        this.elements.messageForm.querySelector('.send-button').disabled = false;
        this.elements.currentUserName.textContent = this.currentUser.displayName;
        this.elements.userAvatar.textContent = this.currentUser.displayName.charAt(0).toUpperCase();

        // Update connection indicator
        const statusDot = this.elements.connectionIndicator.querySelector('.status-dot');
        statusDot.classList.add('online');
        this.elements.connectionIndicator.querySelector('span:last-child').textContent = 'online';

        // Add welcome message
        this.addSystemMessage(`Welcome to the chat, ${this.currentUser.displayName}!`);

        // Update placeholder
        this.updateMessagePlaceholder();
    }

    subscribeToChannel(channelId) {
        if (this.subscriptions.has(channelId)) {
            return; // Already subscribed
        }

        const subscription = this.stompClient.subscribe(`${config.TOPIC_PREFIX}${channelId}`, (message) => {
            this.onMessageReceived(message, channelId);
        });

        this.subscriptions.set(channelId, subscription);
        console.log(`Subscribed to channel: ${channelId}`);
    }

    subscribeToChannelUpdate(){
      if(this.subscriptions.has('channel-updates')){
        return; // Already subscribed
      }

      const subscription = this.stompClient.subscribe(`${config.CHANNEL_UPDATE_TOPIC}`, (channelUpdate) => {
        this.onChannelUpdateReceived(channelUpdate);
      });

      this.subscriptions.set('channel-updates', subscription);
      console.log(`Subscribed to channel updates`);
    }

    unsubscribeFromChannel(channelId) {
        const subscription = this.subscriptions.get(channelId);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(channelId);
            console.log(`Unsubscribed from channel: ${channelId}`);
        }
    }

    onMessageReceived(message, channelId) {
        try {
            const chatMessage = JSON.parse(message.body);
            console.log('Received message:', chatMessage);

            // Store message in the channel's message array
            if (!this.channelMessages.has(channelId)) {
                this.channelMessages.set(channelId, []);
            }
            this.channelMessages.get(channelId).push(chatMessage);

            // Only display message if it's for the current channel
            if (channelId === this.currentChannel.id) {
                this.displayMessage(chatMessage);
            } else {
            }
        } catch (error) {
            console.error('Error parsing message:', error);
        }
    }

    onChannelUpdateReceived(update) {
      try{
        const channelUpdate = JSON.parse(update.body);
        console.log('Channel update received:', channelUpdate);
        if(channelUpdate.updateType === 'NEW_CHANNEL_DTO') {
          const newChannel = channelUpdate.channel;
          this.channels.push({
            id: newChannel.channelId || newChannel.id || '',
            name: newChannel.name || newChannel.channelName || 'unnamed',
            description: newChannel.description || ''
          });
          this.renderChannels();

          // Subscribe to the new channel if connected
          if (this.connected && this.stompClient) {
            this.subscribeToChannel(newChannel.channelId);
          }

          console.log('This channels after addition:', this.channels);
        } else {
          const deletedChannel = channelUpdate.channel;
          this.channels = this.channels.filter(ch => ch.id !== deletedChannel.channelId);
          this.renderChannels();
        }

      } catch(error) {
        console.error('Error parsing channel update:', error);
      }
    }

    displayMessage(message) {
        const messageElement = document.createElement('div');
        messageElement.className = 'message';

        const timestamp = new Date(message.timeStamp || Date.now()).toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });

        const senderInitial = message.sender?.displayName?.charAt(0).toUpperCase() || 'U';
        const senderName = message.sender?.displayName || 'Unknown';

        messageElement.innerHTML = `
            <div class="message-avatar">${senderInitial}</div>
            <div class="message-content">
                <div class="message-header">
                    <span class="message-author">${senderName}</span>
                    <span class="message-timestamp">${timestamp}</span>
                </div>
                <div class="message-body">${this.escapeHtml(message.body || '')}</div>
            </div>
        `;

        this.elements.messagesWrapper.appendChild(messageElement);
        this.scrollToBottom();
    }

    addSystemMessage(text) {
        const messageElement = document.createElement('div');
        messageElement.className = 'system-message';
        messageElement.textContent = text;
        this.elements.messagesWrapper.appendChild(messageElement);
        this.scrollToBottom();
    }

    handleMessageFormSubmit(event) {
        event.preventDefault();

        const messageText = this.elements.messageInput.value.trim();

        if (!messageText || !this.connected) {
            return;
        }

        const chatMessage = {
            messageId: this.generateMessageId(),
            channel: {
                channelId: this.currentChannel.id,
                name: this.currentChannel.name,
                description: this.currentChannel.description || ''
            },
            sender: {
                userId: this.currentUser.userId,
                username: this.currentUser.username,
                displayName: this.currentUser.displayName
            },
            body: messageText,
            type: 'MESSAGE_TEXT',
            timeStamp: Date.now()
        };

        this.sendMessage(chatMessage);
        this.elements.messageInput.value = '';
    }

    sendMessage(message) {
        if (this.stompClient && this.connected) {
            this.stompClient.publish({
                destination: `${config.APP_PREFIX}`,
                body: JSON.stringify(message)
            });
            console.log('Message sent:', message);
        } else {
            console.error('Not connected to WebSocket');
            this.addSystemMessage('Error: Not connected to server');
        }
    }

    generateMessageId() {
        return 'msg_' + Math.random().toString(36).substr(2, 9) + Date.now().toString(36);
    }

    switchChannel(channel) {

        this.currentChannel = channel;
        this.elements.currentChannelName.textContent = channel.name;
        this.elements.messagesWrapper.innerHTML = '';
        this.renderChannels();
        this.updateMessagePlaceholder();

        // Load stored messages for this channel
        if (this.channelMessages.has(channel.id)) {
            const messages = this.channelMessages.get(channel.id);
            messages.forEach(msg => this.displayMessage(msg));
            console.log(`Loaded ${messages.length} stored messages for channel ${channel.id}`);
        }

        this.addSystemMessage(`Switched to #${channel.name}`);
    }

    updateMessagePlaceholder() {
        this.elements.messageInput.placeholder = `Message #${this.currentChannel.name}`;
    }

    onError(error) {
        console.error('STOMP error:', error);
        this.showConnectionStatus('Connection error. Please try again.', 'error');
        this.connected = false;
    }

    onDisconnected() {
        console.log('Disconnected from WebSocket');
        this.connected = false;

        if (this.elements.chatContainer.style.display === 'flex') {
            // Update connection indicator
            const statusDot = this.elements.connectionIndicator.querySelector('.status-dot');
            statusDot.classList.remove('online');
            this.elements.connectionIndicator.querySelector('span:last-child').textContent = 'offline';

            this.addSystemMessage('Disconnected from server');
            this.elements.messageInput.disabled = true;
            this.elements.messageForm.querySelector('.send-button').disabled = true;
        }
    }

    disconnect() {
        if (this.stompClient) {
            // Unsubscribe from all channels
            this.subscriptions.forEach((subscription, channelId) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();

            // Deactivate the client
            this.stompClient.deactivate();
            this.stompClient = null;
        }

        this.connected = false;
        this.currentUser = null;
        this.elements.chatContainer.style.display = 'none';
        this.elements.connectionModal.style.display = 'flex';
        this.elements.messagesWrapper.innerHTML = '';
        this.elements.usernameInput.value = '';
        this.elements.displayNameInput.value = '';
        this.elements.connectionStatus.style.display = 'none';

        console.log('Disconnected and reset');
    }

    showConnectionStatus(message, type) {
        this.elements.connectionStatus.textContent = message;
        this.elements.connectionStatus.className = `connection-status ${type}`;
    }

    scrollToBottom() {
        this.elements.messagesContainer.scrollTop = this.elements.messagesContainer.scrollHeight;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    openCreateChannelModal() {
        this.elements.createChannelModal.style.display = 'flex';
        this.elements.channelNameInput.value = '';
        this.elements.channelDescriptionInput.value = '';
        this.elements.createChannelStatus.style.display = 'none';
        this.elements.channelNameInput.focus();
    }

    closeCreateChannelModal() {
        this.elements.createChannelModal.style.display = 'none';
        this.elements.channelNameInput.value = '';
        this.elements.channelDescriptionInput.value = '';
        this.elements.createChannelStatus.style.display = 'none';
    }

    async handleCreateChannelSubmit(event) {
        event.preventDefault();

        const channelName = this.elements.channelNameInput.value.trim();
        const channelDescription = this.elements.channelDescriptionInput.value.trim();

        if (!channelName) {
            this.showCreateChannelStatus('Please enter a channel name', 'error');
            return;
        }

        this.showCreateChannelStatus('Creating channel...', 'success');

        try {
            const params = new URLSearchParams({
                'name': channelName
            });

            const response = await fetch(`${config.REST_API_BASE_URL}/channel/?${params.toString()}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain'
                },
                body: channelDescription || ''
            });

            if (!response.ok) {
                throw new Error(`Failed to create channel: ${response.status} ${response.statusText}`);
            }

            const newChannel = await response.json();
            console.log('Channel created:', newChannel);

            // Add the new channel to the list
            const channel = {
                id: newChannel.channelId || newChannel.id || '',
                name: newChannel.name || newChannel.channelName || channelName,
                description: newChannel.description || channelDescription || ''
            };


            // Switch to the new channel
            this.switchChannel(channel);

            // Close the modal
            this.closeCreateChannelModal();
            this.addSystemMessage(`Channel #${channel.name} created successfully!`);

        } catch (error) {
            console.error('Error creating channel:', error);
            this.showCreateChannelStatus('Failed to create channel: ' + error.message, 'error');
        }
    }

    showCreateChannelStatus(message, type) {
        this.elements.createChannelStatus.textContent = message;
        this.elements.createChannelStatus.className = `connection-status ${type}`;
    }

    loadChannels() {
        // Load channels from REST API and normalize to internal shape
        return (async () => {
            try {
                const resp = await fetch(`${config.REST_API_BASE_URL}/channel/all`);
                if (!resp.ok) {
                    throw new Error(`Failed to load channels: ${resp.status} ${resp.statusText}`);
                }

                const data = await resp.json();
                if (!Array.isArray(data)) {
                    throw new Error('Invalid channels payload: expected an array');
                }

                // Map backend channel objects to internal channel shape { id, name, description }
                this.channels = data.map(c => ({
                    id: c.channelId || c.id || '',
                    name: c.name || c.channelName || 'unnamed',
                    description: c.description || ''
                })).filter(ch => ch.id);

                // Always set the current channel to the first channel
                if (this.channels.length > 0) {
                    this.currentChannel = this.channels[0];
                }

                // Render channels in the UI
                if (this.elements && this.elements.channelsList) {
                    this.renderChannels();
                }

                // If already connected, subscribe to channels
                if (this.connected && this.stompClient) {
                    this.channels.forEach(channel => this.subscribeToChannel(channel.id));
                }

                return this.channels;
            } catch (error) {
                console.error('Error loading channels:', error);
                // Show a non-blocking system message if UI is ready
                if (this.elements && this.elements.messagesWrapper) {
                    this.addSystemMessage('Could not load channels: ' + (error.message || error));
                }
                return [];
            }
        })();
    }

    generateUser(username, displayName) {
      return (async () => {
        try {
          const params = new URLSearchParams({
            'username': username,
            'displayName': displayName
          });
          const resp = await fetch(`${config.REST_API_BASE_URL}/user/?${params.toString()}`, {
            method: 'POST'
          });
          if (resp.status !== 201) {
            throw new Error(`Failed to generate user: ${resp.status} ${resp.statusText}`);
          }

          const user = await resp.json();

          // Store the user object directly
          this.currentUser = {
            userId: user.userId || user.id || '',
            username: user.username || 'user_' + Math.random().toString(36).substr(2, 9),
            displayName: user.displayName || 'User'
          };

          return this.currentUser;
        } catch (error) {
          console.error('Error generating user:', error);
          return null;
        }
      })();
    }

  loadUsers() {
    // Load users from REST API and store them in a Map
    return (async () => {
      try {
        const resp = await fetch(`${config.REST_API_BASE_URL}/user/all`);
        if (!resp.ok) {
          throw new Error(`Failed to load users: ${resp.status} ${resp.statusText}`);
        }

        const data = await resp.json();
        if (!Array.isArray(data)) {
          throw new Error('Invalid users payload: expected an array');
        }

        // Map backend user objects to internal user shape and store in Map
        data.forEach(u => {
          const user = {
            userId: u.userId || u.id || '',
            username: u.username || 'user_' + Math.random().toString(36).substr(2, 9),
            displayName: u.displayName || 'User'
          };
          if (user.userId) {
            this.users.set(user.userId, user);
          }
        });

        return this.users;
      } catch (error) {
        console.error('Error loading users:', error);
        return new Map();
      }
    })();
  }

  loadMessages(channelId) {
    // Load messages for a specific channel from REST API
    return (async () => {
      try {
        const resp = await fetch(`${config.REST_API_BASE_URL}/message/${channelId}/history`);
        if (!resp.ok) {
          throw new Error(`Failed to load messages: ${resp.status} ${resp.statusText}`);
        }

        const data = await resp.json();
        const messages = data.messages || data; // Support different payload shapes
        console.log("Loaded messages for channel", channelId, messages);
        if (!Array.isArray(messages)) {
          throw new Error('Invalid messages payload: expected an array');
        }

        // Store messages in the channelMessages map
        this.channelMessages.set(channelId, messages.reverse());

        return messages;
      } catch (error) {
        console.error('Error loading messages:', error);
        return [];
      }
    })();
  }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('Initializing Chat App...');
    new ChatApp();
});
