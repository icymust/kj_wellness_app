package com.ndl.numbers_dont_lie.model;

public class User {
    private final String email;
    private final String password; // пока в открытом виде — позже захэшируем
    private boolean emailVerified;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.emailVerified = false;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
}
