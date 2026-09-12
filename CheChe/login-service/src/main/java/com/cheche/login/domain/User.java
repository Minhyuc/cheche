package com.cheche.login.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true)
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ADMIN'")
    private AccountType accountType;

    @Column(length = 20)
    private String regionCode;

    @Column(length = 80)
    private String regionName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {}

    public User(String username, String passwordHash) {
        this(username, passwordHash, AccountType.ADMIN);
    }

    public User(String username, String passwordHash, AccountType accountType) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public AccountType getAccountType() { return accountType; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateRegion(String regionCode, String regionName) {
        this.regionCode = regionCode;
        this.regionName = regionName;
    }
}
