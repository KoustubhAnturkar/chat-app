package org.example.chatservice.chatbackend.dto;

import com.example.chat.proto.ChatMessage;

public class ChatMessageDTO {
    private String messageId;
    private String body;
    private ChannelDTO channel;
    private UserDTO sender;
    private long timeStamp;

    public ChatMessageDTO() {
    }

    public ChatMessageDTO(String messageId, String body, ChannelDTO channel, UserDTO sender, long timeStamp) {
        this.messageId = messageId;
        this.body = body;
        this.channel = channel;
        this.sender = sender;
        this.timeStamp = timeStamp;
    }

    public static ChatMessageDTO fromProto(ChatMessage message) {
        return new ChatMessageDTO(
                message.getMessageId(),
                message.getBody(),
                ChannelDTO.fromProto(message.getChannel()),
                UserDTO.fromProto(message.getSender()),
                message.getTimeStamp()
        );
    }

    public ChatMessage toProto() {
        return ChatMessage.newBuilder()
                .setMessageId(this.messageId)
                .setBody(this.body)
                .setChannel(this.channel.toProto())
                .setSender(this.sender.toProto())
                .setTimeStamp(this.timeStamp)
                .build();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ChannelDTO getChannel() {
        return channel;
    }

    public void setChannel(ChannelDTO channel) {
        this.channel = channel;
    }

    public UserDTO getSender() {
        return sender;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

}
