package com.example.demo.DTO;

public class AuthResponse {
    private boolean success;
    private String message;
    private String mobileNumber;
    private String token;

    // 构造方法
    public AuthResponse() {}

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String mobileNumber) {
        this.success = success;
        this.message = message;
        this.mobileNumber = mobileNumber;
    }

    public AuthResponse(boolean success, String message, String mobileNumber , String token) {
        this.success = success;
        this.message = message;
        this.mobileNumber = mobileNumber;
        this.token = token;
    }

    public static AuthResponse success(String message, String mobileNumber) {
        return new AuthResponse(true, message, mobileNumber);
    }

    // 静态工厂方法 - 成功响应（登录用）
    public static AuthResponse success(String message, String mobileNumber, String token) {
        return new AuthResponse(true, message, mobileNumber, token);
    }

    // 静态工厂方法 - 失败响应
    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
    }

    // Getter和Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}