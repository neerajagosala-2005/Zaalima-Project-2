package com.zaalima.iam_server.dto;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private boolean mfaEnabled;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String username,
            String email,
            boolean enabled,
            boolean mfaEnabled) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.mfaEnabled = mfaEnabled;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }
}