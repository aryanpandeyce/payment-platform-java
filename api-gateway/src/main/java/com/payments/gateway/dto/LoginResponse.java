package com.payments.gateway.dto;

public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private String username;

    public LoginResponse() {
    }

    public LoginResponse(String token, String tokenType, long expiresIn, String username) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getUsername() {
        return username;
    }
}
