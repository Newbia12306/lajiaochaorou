package com.example.demo.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
public class RegisterRequest extends BaseRequest{


    @NotBlank(message = "请确认密码")
    private String confirmPassword;

    public RegisterRequest(){}

    public RegisterRequest(String mobileNumber, String password , String confirmPassword){
        super(mobileNumber, password);
        this.confirmPassword = confirmPassword;
    }

    public String getConfirmPassword() {return confirmPassword;}
    public void setConfirmPassword(String confirmPassword) {this.confirmPassword = confirmPassword;}
}