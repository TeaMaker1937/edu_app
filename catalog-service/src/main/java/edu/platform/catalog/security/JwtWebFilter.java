package edu.platform.catalog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtWebFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:edu-platform}")
    private String issuer;

    @Value("${jwt.audience:education-platform}")
    private String audience;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();

            exchange.getAttributes().put("jwt.subject", claims.getSubject());
            exchange.getAttributes().put("jwt.userId", claims.get("userId", Long.class));
            exchange.getAttributes().put("jwt.roles", claims.get("authorities", List.class));
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[CATALOG] Invalid JWT for {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublic(String path) {
        if (path.startsWith("/actuator")) return true;
        if (path.matches("/api/courses/\\d+/offer")) return true;
        if (path.matches("/api/courses/\\d+/notification-template")) return true;
        return false;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}