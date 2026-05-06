package com.example.demo.DTO;

public class LoginRequest  extends BaseRequest {


    public LoginRequest() {}

    public LoginRequest(String mobileNumber, String password) {

        super(mobileNumber, password);
    }
}
