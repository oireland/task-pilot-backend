package com.taskpilot.dto.user;

// This DTO is used to safely transfer user data to the frontend.
// It excludes sensitive information like passwords and verification codes.
public class UserDTO {

    private Long id;
    private String email;
    private boolean enabled;
    private String notionWorkspaceName;
    private String notionWorkspaceIcon;
    private String notionTargetDatabaseId;
    private String notionTargetDatabaseName;

    // Constructors, Getters, and Setters

    public UserDTO(Long id, String email, boolean enabled, String notionWorkspaceName, String notionWorkspaceIcon, String notionTargetDatabaseId, String notionTargetDatabaseName) {
        this.id = id;
        this.email = email;
        this.enabled = enabled;
        this.notionWorkspaceName = notionWorkspaceName;
        this.notionWorkspaceIcon = notionWorkspaceIcon;
        this.notionTargetDatabaseId = notionTargetDatabaseId;
        this.notionTargetDatabaseName = notionTargetDatabaseName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getNotionTargetDatabaseId() {
        return notionTargetDatabaseId;
    }

    public void setNotionTargetDatabaseId(String notionTargetDatabaseId) {
        this.notionTargetDatabaseId = notionTargetDatabaseId;
    }

    public String getNotionTargetDatabaseName() {
        return notionTargetDatabaseName;
    }

    public void setNotionTargetDatabaseName(String notionTargetDatabaseName) {
        this.notionTargetDatabaseName = notionTargetDatabaseName;
    }
}
