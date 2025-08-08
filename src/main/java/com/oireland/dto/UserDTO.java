package com.oireland.dto;

// This DTO is used to safely transfer user data to the frontend.
// It excludes sensitive information like passwords and verification codes.
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String notionWorkspaceName;
    private String notionWorkspaceIcon;

    // Constructors, Getters, and Setters

    public UserDTO(Long id, String username, String email, String notionWorkspaceName, String notionWorkspaceIcon) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.notionWorkspaceName = notionWorkspaceName;
        this.notionWorkspaceIcon = notionWorkspaceIcon;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNotionWorkspaceName() {
        return notionWorkspaceName;
    }

    public void setNotionWorkspaceName(String notionWorkspaceName) {
        this.notionWorkspaceName = notionWorkspaceName;
    }

    public String getNotionWorkspaceIcon() {
        return notionWorkspaceIcon;
    }

    public void setNotionWorkspaceIcon(String notionWorkspaceIcon) {
        this.notionWorkspaceIcon = notionWorkspaceIcon;
    }
}
