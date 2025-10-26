package org.example.chatservice.chatbackend.dto;

import com.example.chat.proto.ChannelUpdate;

import static com.example.chat.proto.ChannelUpdateType.*;

public class ChannelUpdateDTO {
    public enum ChannelUpdateTypeDTO{
        NEW_CHANNEL_DTO,
        DELETE_CHANNEL_DTO,
    }

    private ChannelUpdateTypeDTO updateType;
    private ChannelDTO channel;

    public ChannelUpdateDTO() {
    }

    private ChannelUpdateDTO(ChannelUpdateTypeDTO updateType, ChannelDTO channel) {
        this.updateType = updateType;
        this.channel = channel;
    }

    public static ChannelUpdateDTO fromProto(ChannelUpdate channelUpdate) {
        ChannelUpdateTypeDTO typeDTO = switch (channelUpdate.getType()) {
            case NEW_CHANNEL -> ChannelUpdateTypeDTO.NEW_CHANNEL_DTO;
            case DELETE_CHANNEL -> ChannelUpdateTypeDTO.DELETE_CHANNEL_DTO;
            default -> throw new IllegalArgumentException("Unknown ChannelUpdateType: " + channelUpdate.getType());
        };
        return new ChannelUpdateDTO(
                typeDTO,
                ChannelDTO.fromProto(channelUpdate.getChannel())
        );
    }

    public ChannelUpdateTypeDTO getUpdateType() {
        return updateType;
    }

    public void setUpdateType(ChannelUpdateTypeDTO updateType) {
        this.updateType = updateType;
    }

    public ChannelDTO getChannel() {
        return channel;
    }

    public void setChannel(ChannelDTO channel) {
        this.channel = channel;
    }
}
