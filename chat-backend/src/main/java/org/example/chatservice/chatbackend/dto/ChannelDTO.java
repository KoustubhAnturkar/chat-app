package org.example.chatservice.chatbackend.dto;

import com.example.chat.proto.Channel;

public class ChannelDTO {
    private String channelId;
    private String name;
    private String description;

    public ChannelDTO() {
    }

    public ChannelDTO(String channelId, String name, String description) {
        this.channelId = channelId;
        this.name = name;
        this.description = description;
    }

    public static ChannelDTO fromProto(Channel channel) {
        return new ChannelDTO(
                channel.getChannelId(),
                channel.getName(),
                channel.getDescription()
        );
    }

    public Channel toProto() {
        return Channel.newBuilder()
                .setChannelId(this.channelId)
                .setName(this.name)
                .setDescription(this.description)
                .build();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

