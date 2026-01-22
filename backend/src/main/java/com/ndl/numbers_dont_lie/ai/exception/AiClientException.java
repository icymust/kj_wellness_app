package com.ndl.numbers_dont_lie.ai.exception;

/**
 * User-friendly AI client exception for operational errors (timeouts, rate limits, invalid key,
 * malformed responses). Designed to surface clear messages to the caller without leaking vendor specifics.
 */
public class AiClientException extends RuntimeException {
    private final String userFriendlyMessage;

    public AiClientException(String userFriendlyMessage) {
        super(userFriendlyMessage);
        this.userFriendlyMessage = userFriendlyMessage;
    }

    public AiClientException(String userFriendlyMessage, Throwable cause) {
        super(userFriendlyMessage, cause);
        this.userFriendlyMessage = userFriendlyMessage;
    }

    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }
}
