package com.cheche.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** JWT identity verification plus live authorization-context lookup. */
@Component
public class AuthenticationBridgeFilter implements GlobalFilter, Ordered {
    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";
    private static final String USER_REGION = "X-User-Region";

    private final SecretKey signingKey;
    private final String issuer;
    private final WebClient adminClient;
    private final WebClient userClient;

    public AuthenticationBridgeFilter(
            WebClient.Builder builder,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:cheche-login-service}") String issuer,
            @Value("${services.admin.base-url:http://localhost:8082}") String adminBaseUrl,
            @Value("${services.user.base-url:http://localhost:8081}") String userBaseUrl) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET은 32바이트 이상이어야 합니다.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.adminClient = builder.baseUrl(adminBaseUrl).build();
        this.userClient = builder.baseUrl(userBaseUrl).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        ServerWebExchange sanitized = removeUntrustedIdentityHeaders(exchange);
        // Browser preflight requests do not carry the application JWT.
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(sanitized);
        }
        if (isPublic(path)) return chain.filter(sanitized);

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).requireIssuer(issuer).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        Number userIdClaim = claims.get("userId", Number.class);
        if (userIdClaim == null) return reject(exchange, HttpStatus.UNAUTHORIZED);
        String accountType = claims.get("accountType", String.class);
        String userId = String.valueOf(userIdClaim.longValue());

        if (isUserProfilePath(path)) {
            if (!"USER".equals(accountType)) return reject(exchange, HttpStatus.FORBIDDEN);
            ServerWebExchange authenticatedUser = sanitized.mutate().request(request ->
                    request.header(USER_ID, userId)).build();
            return chain.filter(authenticatedUser);
        }

        if (isUserAppPath(path)) {
            if (!"USER".equals(accountType)) return reject(exchange, HttpStatus.FORBIDDEN);
            return userClient.get()
                    .uri("/api/users/me")
                    .header(USER_ID, userId)
                    .retrieve()
                    .bodyToMono(UserContext.class)
                    .flatMap(context -> {
                        if (context.initialSetupRequired() || context.regionCode() == null) {
                            return reject(exchange, HttpStatus.PRECONDITION_REQUIRED);
                        }
                        ServerWebExchange authenticatedUser = sanitized.mutate().request(request -> {
                            request.header(USER_ID, userId);
                            request.header(USER_REGION, context.regionCode());
                        }).build();
                        return chain.filter(authenticatedUser);
                    })
                    .onErrorResume(error -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE));
        }

        if (!"ADMIN".equals(accountType)) {
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        return adminClient.get()
                .uri("/api/admins/me")
                .header(USER_ID, userId)
                .retrieve()
                .bodyToMono(AdminContext.class)
                .flatMap(context -> {
                    if (!"ACTIVE".equals(context.status())) {
                        return reject(exchange, HttpStatus.FORBIDDEN);
                    }
                    ServerWebExchange authenticated = sanitized.mutate().request(request -> {
                        request.header(USER_ID, userId);
                        request.header(USER_ROLE, context.role());
                        if (context.regionCode() != null && !context.regionCode().isBlank()) {
                            request.header(USER_REGION, context.regionCode());
                        }
                    }).build();
                    return chain.filter(authenticated);
                })
                .onErrorResume(error -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE));
    }

    private ServerWebExchange removeUntrustedIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate().request(request -> request.headers(headers -> {
            headers.remove(USER_ID);
            headers.remove(USER_ROLE);
            headers.remove(USER_REGION);
        })).build();
    }

    private boolean isPublic(String path) {
        return path.startsWith("/auth/")
                || path.equals("/")
                || path.equals("/index.html")
                || path.equals("/login.css")
                || path.equals("/login.js")
                || path.equals("/actuator/health")
                || path.startsWith("/inspection-photos/")
                || path.startsWith("/api/inspections/public/");
    }

    private boolean isUserProfilePath(String path) {
        return path.startsWith("/api/users/");
    }

    private boolean isUserAppPath(String path) {
        return path.startsWith("/api/user/");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }

    private record AdminContext(String role, String status, String regionCode) {}
    private record UserContext(String regionCode, boolean initialSetupRequired) {}
}
