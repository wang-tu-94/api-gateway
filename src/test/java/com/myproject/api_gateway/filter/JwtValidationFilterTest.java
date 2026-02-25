package com.myproject.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class JwtValidationFilterTest {

    @Autowired
    private WebTestClient webClient;

    @Value("${jwt.secret}")
    private String secret;

    private static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        // On force le port défini dans ton application-test.yml
        mockBackEnd.start(8082);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void shouldAllowPublicRoutesWithoutToken() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));
        webClient.get().uri("/ms-auth/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk(); // Preuve que le filtre a laissé passer la requête
    }

    @Test
    void shouldRejectSecuredRoutesWithoutToken() {
        webClient.get().uri("/product-backend/api/v1/products")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Token d'authentification manquant");
    }

    @Test
    void shouldRejectSecuredRoutesWithInvalidToken() {
        webClient.get().uri("/product-backend/api/v1/products")
                .header("Authorization", "Bearer faux_token_pourri")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Token invalide ou expiré");
    }

    @Test
    void shouldAllowSecuredRoutesWithValidToken() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));

        String validToken = generateValidToken();

        webClient.get().uri("/product-backend/api/v1/products")
                .header("Authorization", "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk();
    }

    private String generateValidToken() {
        return Jwts.builder()
                .setSubject("test-user")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 10)) // Valide 10 min
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), SignatureAlgorithm.HS256)
                .compact();
    }
}