package org.example.chatservice.chatbackend.dto;

import com.example.chat.proto.User;

public class UserDTO {
    private String userId;
    private String username;
    private String displayName;

    public UserDTO() {
    }

    public UserDTO(String userId, String username, String displayName) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
    }

    public static UserDTO fromProto(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getDisplayName()
        );
    }

    public User toProto() {
        return User.newBuilder()
                .setUserId(this.userId)
                .setUsername(this.username)
                .setDisplayName(this.displayName)
                .build();
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
