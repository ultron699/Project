package com.spotify.widget.controller;

import com.spotify.widget.token.TokenStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@RestController
public class AuthController {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final WebClient webClient = WebClient.create();

    @GetMapping("/auth/callback")
    public Mono<String> handleCallback(@RequestParam("code") String code) {
        String creds = clientId + ":" + clientSecret;
        String base64Creds = Base64.getEncoder().encodeToString(creds.getBytes());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

        return webClient.post()
                .uri("https://accounts.spotify.com/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Creds)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String accessToken = (String) response.get("access_token");
                    String refreshToken = (String) response.get("refresh_token");

                    // TokenStorage
                    TokenStorage.setAccessToken(accessToken);
                    TokenStorage.setRefreshToken(refreshToken);

                    System.out.println("Access Token: " + accessToken);
                    System.out.println("Refresh Token: " + refreshToken);

                    return "Tokens stored. You can now <a href='/current-song'>check the current song</a>.";
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just("Error occurred: " + e.getMessage());
                });
    }
}
