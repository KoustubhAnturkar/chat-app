package org.example.chatservice.chatbackend.dto;

import com.example.chat.proto.UserUpdate;

import static com.example.chat.proto.UserUpdateType.*;

public class UserUpdateDTO {
    public enum UserUpdateTypeDTO{
        NEW_USER_DTO,
        DELETE_USER_DTO,
    }

    private UserUpdateTypeDTO updateType;
    private UserDTO user;

    public UserUpdateDTO() {
    }

    private UserUpdateDTO(UserUpdateTypeDTO updateType, UserDTO user) {
        this.updateType = updateType;
        this.user = user;
    }

    public static UserUpdateDTO fromProto(UserUpdate userUpdate) {
        UserUpdateTypeDTO typeDTO = switch (userUpdate.getType()) {
            case NEW_USER -> UserUpdateTypeDTO.NEW_USER_DTO;
            case DELETE_USER -> UserUpdateTypeDTO.DELETE_USER_DTO;
            default -> throw new IllegalArgumentException("Unknown UserUpdateType: " + userUpdate.getType());
        };
        return new UserUpdateDTO(
                typeDTO,
                UserDTO.fromProto(userUpdate.getUser())
        );
    }

    public UserUpdateTypeDTO getUpdateType() {
        return updateType;
    }

    public void setUpdateType(UserUpdateTypeDTO updateType) {
        this.updateType = updateType;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}
