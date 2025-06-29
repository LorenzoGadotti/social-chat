package com.socialChat;

public class User {
    public long id;
    public String username;
    public String passwordHash;
    public String systemPrompt;

    public String parentPassword;

    public User() {}

    public User(long id, String username, String passwordHash, String systemPrompt, String parentPassword) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.systemPrompt = systemPrompt;
        this.parentPassword = parentPassword;
    }
}
