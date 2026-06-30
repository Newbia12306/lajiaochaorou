package com.example.demo.service;

import com.example.demo.DTO.RegisterRequest;
import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.model.Users;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(JwtService jwtService, UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse registerUser(RegisterRequest request) {
        if (!isValidMobileNumber(request.getMobileNumber())) {
            return new AuthResponse(false, "手机号格式不正确");
        }

        if (userRepository.existsById(request.getMobileNumber())) {
            return new AuthResponse(false, "该手机号已被注册");
        }

        if (request.getPassword().length() < 8) {
            return new AuthResponse(false, "密码长度至少8位");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new AuthResponse(false, "两次输入的密码不一致");
        }

        try {
            Users user = new Users();
            user.setId(request.getMobileNumber());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            Users savedUser = userRepository.save(user);
            return new AuthResponse(true, "注册成功", savedUser.getId());

        } catch (Exception e) {
            log.error("注册用户失败: {}", request.getMobileNumber(), e);
            return new AuthResponse(false, "系统错误，请稍后重试");
        }
    }

    public AuthResponse loginUser(LoginRequest request) {
        if (!isValidMobileNumber(request.getMobileNumber())) {
            return AuthResponse.error("手机号格式不正确");
        }

        if (!userRepository.existsById(request.getMobileNumber())) {
            return AuthResponse.error("用户不存在");
        }

        try {
            Optional<Users> userOptional = userRepository.findById(request.getMobileNumber());
            if (userOptional.isEmpty()) {
                return AuthResponse.error("用户不存在");
            }
            Users user = userOptional.get();

            boolean passwordMatch = false;
            String storedPassword = user.getPassword();

            if (storedPassword.startsWith("$2a$")) {
                passwordMatch = passwordEncoder.matches(request.getPassword(), storedPassword);
            } else {
                // Compatible with old plaintext passwords: auto-upgrade to BCrypt
                if (storedPassword.equals(request.getPassword())) {
                    passwordMatch = true;
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    userRepository.save(user);
                    log.info("已为用户 {} 升级密码为 BCrypt 加密", user.getId());
                }
            }

            if (!passwordMatch) {
                return AuthResponse.error("密码错误");
            }

            String token = jwtService.generateToken(user.getId());
            return AuthResponse.success("登录成功", user.getId(), token);

        } catch (Exception e) {
            log.error("登录失败: {}", request.getMobileNumber(), e);
            return AuthResponse.error("系统错误，请稍后重试");
        }
    }

    private boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber != null && mobileNumber.matches("^1[3-9]\\\\d{9}$");
    }
}
