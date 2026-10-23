package edu.platform.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:edu-platform}")
    private String issuer;

    @Value("${jwt.audience:education-platform}")
    private String audience;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Добавляем в контекст exchange — чтобы downstream-фильтры
            // (или logging) могли использовать, если понадобится.
            exchange.getAttributes().put("jwt.subject", claims.getSubject());
            exchange.getAttributes().put("jwt.userId", claims.get("userId", Long.class));
            exchange.getAttributes().put("jwt.roles", claims.get("authorities", List.class));

            log.debug("[GATEWAY] {} {} allowed for user={}",
                    exchange.getRequest().getMethod(), path, claims.getSubject());
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[GATEWAY] Invalid token for {}: {}", path, e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token: " + e.getMessage());
        }
    }

    private boolean isPublic(String path) {
        if (path.startsWith("/api/auth/")) return true;
        if (path.startsWith("/api/demo/")) return true;
        if (path.startsWith("/actuator/")) return true;
        if (path.startsWith("/swagger-ui/")) return true;
        if (path.startsWith("/v3/api-docs/")) return true;
        if (path.matches("/api/courses/\\d+/offer")) return true;
        if (path.matches("/api/courses/\\d+/notification-template")) return true;
        if (path.equals("/api/enrollment-offers")
                || path.startsWith("/api/enrollment-offers/")) return true;
        return false;
    }
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] body = ("{\"code\":\"" + status.name() + "\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;   // до routing-фильтров
    }
}