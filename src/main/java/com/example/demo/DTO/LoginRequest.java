package com.example.demo.dto;

public class LoginRequest  extends BaseRequest {


    public LoginRequest() {}

    public LoginRequest(String mobileNumber, String password) {

        super(mobileNumber, password);
    }
}
