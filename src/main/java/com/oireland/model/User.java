package com.oireland.model;


import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    // --- Core App Properties ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Using IDENTITY for better compatibility with most SQL DBs
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean enabled;

    // --- Email Verification Properties ---

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;


    // --- Notion Integration Properties ---

    @Column(name = "notion_access_token", length = 1024) // Increased length for encrypted token
    private String notionAccessToken;

    @Column(name = "notion_workspace_id")
    private String notionWorkspaceId;

    @Column(name = "notion_bot_id")
    private String notionBotId;

    @Column(name = "notion_target_database_id") // The DB where to-do items will be created
    private String notionTargetDatabaseId;

    @Column(name = "notion_workspace_name")
    private String notionWorkspaceName;

    @Column(name = "notion_workspace_icon")
    private String notionWorkspaceIcon;


    // --- Timestamps ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // --- Constructors ---

    /**
     * Constructor for creating a new, unverified user.
     */
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = false; // A new user should start as not enabled
    }

    /**
     * Default constructor required by JPA.
     */
    public User() {
    }


    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    // We are using email address for signing in, so this method needs to return the email even though it's called getUsername
    @Override
    public String getUsername() {
        return email;
    }

    public String getDisplayUsername() {
        return this.username;
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

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) {
        this.verificationCodeExpiresAt = verificationCodeExpiresAt;
    }

    public String getNotionAccessToken() {
        return notionAccessToken;
    }

    public void setNotionAccessToken(String notionAccessToken) {
        this.notionAccessToken = notionAccessToken;
    }

    public String getNotionWorkspaceId() {
        return notionWorkspaceId;
    }

    public void setNotionWorkspaceId(String notionWorkspaceId) {
        this.notionWorkspaceId = notionWorkspaceId;
    }

    public String getNotionBotId() {
        return notionBotId;
    }

    public void setNotionBotId(String notionBotId) {
        this.notionBotId = notionBotId;
    }

    public String getNotionTargetDatabaseId() {
        return notionTargetDatabaseId;
    }

    public void setNotionTargetDatabaseId(String notionTargetDatabaseId) {
        this.notionTargetDatabaseId = notionTargetDatabaseId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // You can expand this to support roles (e.g., ROLE_USER, ROLE_ADMIN)
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Or implement logic for account expiration
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Or implement logic for account locking
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Or implement logic for password expiration
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}