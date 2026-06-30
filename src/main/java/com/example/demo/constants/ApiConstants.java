package com.example.demo.constants;

/**
 * API response map keys — extracted to avoid duplicated string literals (Sonar java:S1192).
 */
public final class ApiConstants {

    public static final String KEY_ERROR = "error";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_SUCCESS = "success";

    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
