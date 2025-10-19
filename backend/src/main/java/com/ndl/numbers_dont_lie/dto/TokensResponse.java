package com.ndl.numbers_dont_lie.dto;

public class TokensResponse {
    public String accessToken;
    public String refreshToken;

    public TokensResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
