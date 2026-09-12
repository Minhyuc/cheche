package com.cheche.login.controller;

import com.cheche.login.dto.CredentialsRequest;
import com.cheche.login.dto.LoginResponse;
import com.cheche.login.dto.RegisterResponse;
import com.cheche.login.dto.UserLoginResponse;
import com.cheche.login.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {
    private final LoginService service;

    public LoginController(LoginService service) {
        this.service = service;
    }

    @PostMapping({"/register", "/admin/register"})
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping({"/login", "/admin/login"})
    public ResponseEntity<LoginResponse> loginAdmin(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.ok(service.loginAdmin(request));
    }

    @PostMapping("/user/login")
    public ResponseEntity<UserLoginResponse> loginUser(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.ok(service.loginUser(request));
    }

    @PostMapping("/user/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody CredentialsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerUser(request));
    }
}
