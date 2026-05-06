package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class Users {

    @Id
    @Column(name = "id", nullable = false)
    private String id; // 存储手机号

    @Column(name = "password", nullable = false)
    private String password;

    // 必须有无参构造方法
    public Users() {}

    public Users(String id, String password) {
        this.id = id;
        this.password = password;
    }

    // Getter和Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}