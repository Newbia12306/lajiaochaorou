package com.example.demo.controller;

import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.RegisterRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest) {

        System.out.println("收到注册请求: " + registerRequest.getMobileNumber());

        AuthResponse response = userService.registerUser(registerRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.loginUser(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }else{
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyToken(@RequestHeader("Authorization")String authHeader){
        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            return ResponseEntity.badRequest().body(AuthResponse.error("token格式错误"));
        }
        String token = authHeader.substring(7);

        if(jwtService.validateToken(token)){
            String mobileNumeber = jwtService.extractMobileNumber(token);
            return ResponseEntity.ok(AuthResponse.success("token有效",mobileNumeber,token));
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.error("token无效"));
        }
    }

}