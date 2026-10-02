package com.zaalima.iam_server.dto;

public class LoginResponse {

    private String message;
    private String token;
    private boolean mfaRequired;

    public LoginResponse() {
    }

    public LoginResponse(
            String message,
            String token,
            boolean mfaRequired) {

        this.message = message;
        this.token = token;
        this.mfaRequired = mfaRequired;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }
}