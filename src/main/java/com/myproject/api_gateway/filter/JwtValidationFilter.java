package com.myproject.api_gateway.filter;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private final SecretKey key;

    private final List<String> openApiEndpoints = List.of(
            "/v1/auth/login",
            "/v1/accounts/register"
    );

    public JwtValidationFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. On laisse passer les routes publiques
        if (isSecured(request)) {
            if (!request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Token d'authentification manquant", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Format du token invalide. Doit commencer par 'Bearer '", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.replace("Bearer ", "").trim();

            try {
                // 2. Vérification cryptographique et date d'expiration
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token);
            } catch (JwtException e) {
                return onError(exchange, "Token invalide ou expiré", HttpStatus.UNAUTHORIZED);
            }
        }

        // 3. Tout est ok, on laisse la Gateway router la requête
        return chain.filter(exchange);
    }

    private boolean isSecured(ServerHttpRequest request) {
        return openApiEndpoints.stream()
                .noneMatch(uri -> request.getURI().getPath().contains(uri));
    }

        private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"status\": %d, \"error\": \"Unauthorized\", \"message\": \"%s\"}",
                status.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Priorité haute : s'exécute avant le routage
    }
}