package com.cheche.login.service;

import com.cheche.login.client.AdminServiceClient;
import com.cheche.login.domain.AccountType;
import com.cheche.login.domain.User;
import com.cheche.login.dto.*;
import com.cheche.login.repository.UserRepository;
import com.cheche.login.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AdminServiceClient adminServiceClient;

    public LoginService(UserRepository repository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, AdminServiceClient adminServiceClient) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminServiceClient = adminServiceClient;
    }

    @Transactional
    public RegisterResponse register(CredentialsRequest request) {
        String username = normalize(request.username());
        if (repository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        User saved = repository.save(new User(username, passwordEncoder.encode(request.password()), AccountType.ADMIN));
        return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public LoginResponse loginAdmin(CredentialsRequest request) {
        User user = authenticate(request, AccountType.ADMIN);
        AdminProfile admin = adminServiceClient.sync(user.getId(), user.getUsername());
        if (!"ACTIVE".equals(admin.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 관리자 계정입니다.");
        }

        String token = jwtService.issue(user);
        return new LoginResponse(user.getId(), user.getUsername(), "Bearer", token,
                jwtService.getExpirationSeconds(), admin.role(), admin.regionCode(), admin.regionName(),
                admin.initialSetupRequired());
    }

    @Transactional(readOnly = true)
    public UserLoginResponse loginUser(CredentialsRequest request) {
        User user = authenticate(request, AccountType.USER);
        String token = jwtService.issue(user);
        return new UserLoginResponse(user.getId(), user.getUsername(), AccountType.USER.name(),
                "Bearer", token, jwtService.getExpirationSeconds(),
                "사용자 로그인에 성공했습니다. 사용자 기능은 아직 제공되지 않습니다.");
    }

    private User authenticate(CredentialsRequest request, AccountType expectedType) {
        String username = normalize(request.username());
        User user = repository.findByUsername(username)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getAccountType() != expectedType) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    expectedType == AccountType.ADMIN
                            ? "관리자 계정이 아닙니다. 사용자 로그인 탭을 이용해 주세요."
                            : "사용자 계정이 아닙니다. 관리자 로그인 탭을 이용해 주세요.");
        }
        return user;
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
    }
}
