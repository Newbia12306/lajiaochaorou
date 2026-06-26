package com.example.demo.service;

import com.example.demo.DTO.RegisterRequest;
import com.example.demo.DTO.LoginRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.model.Users;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    @Autowired
    private JwtService jwtService;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse registerUser(RegisterRequest request) {
        // 1. 手机号格式验证
        if (!isValidMobileNumber(request.getMobileNumber())) {
            return new AuthResponse(false, "手机号格式不正确");
        }

        // 2. 检查手机号是否已存在
        if (userRepository.existsById(request.getMobileNumber())) {
            return new AuthResponse(false, "该手机号已被注册");
        }

        // 3. 检查密码长度
        if (request.getPassword().length() < 8) {
            return new AuthResponse(false, "密码长度至少8位");
        }

        // 4. 检查密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new AuthResponse(false, "两次输入的密码不一致");
        }

        try {
            // 5. 创建用户（密码加密存储）
            Users user = new Users();
            user.setId(request.getMobileNumber());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            Users savedUser = userRepository.save(user);

            return new AuthResponse(true, "注册成功", savedUser.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResponse(false, "系统错误，请稍后重试");
        }
    }

    public AuthResponse loginUser(LoginRequest request) {
        // 1. 手机号格式验证
        if (!isValidMobileNumber(request.getMobileNumber())) {
            return AuthResponse.error("手机号格式不正确");
        }

        // 2. 检查用户是否存在
        if (!userRepository.existsById(request.getMobileNumber())) {
            return AuthResponse.error("用户不存在");
        }

        try {
            // 3. 获取用户信息
            Optional<Users> userOptional = userRepository.findById(request.getMobileNumber());
            if (userOptional.isEmpty()) {
                return AuthResponse.error("用户不存在");
            }
            Users user = userOptional.get();

            // 4. 验证密码（BCrypt + 旧密码兼容）
            boolean passwordMatch = false;
            String storedPassword = user.getPassword();

            // BCrypt 哈希密码以 $2a$ 开头
            if (storedPassword.startsWith("$2a$")) {
                passwordMatch = passwordEncoder.matches(request.getPassword(), storedPassword);
            } else {
                // 兼容旧明文密码：比较后自动升级为 BCrypt
                if (storedPassword.equals(request.getPassword())) {
                    passwordMatch = true;
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    userRepository.save(user);
                    System.out.println("已为用户 " + user.getId() + " 升级密码为 BCrypt 加密");
                }
            }

            if (!passwordMatch) {
                return AuthResponse.error("密码错误");
            }

            // 5. 生成token
            String token = jwtService.generateToken(user.getId());

            //6.返回响应
            return AuthResponse.success("登录成功",user.getId(),token);

        } catch (Exception e) {
            e.printStackTrace();
            return AuthResponse.error("系统错误，请稍后重试");
        }
    }

    private boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber != null && mobileNumber.matches("^1[3-9]\\d{9}$");
    }
}