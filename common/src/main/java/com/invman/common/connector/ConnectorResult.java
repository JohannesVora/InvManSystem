package com.invman.common.connector;

public record ConnectorResult(boolean success, String message) {

    public static ConnectorResult ok(String message) {
        return new ConnectorResult(true, message);
    }

    public static ConnectorResult failure(String message) {
        return new ConnectorResult(false, message);
    }
}
