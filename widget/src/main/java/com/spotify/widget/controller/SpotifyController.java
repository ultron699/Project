package com.spotify.widget.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import com.spotify.widget.token.TokenStorage;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SpotifyController {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final WebClient webClient = WebClient.create();

    @GetMapping("/login")
    public Mono<ResponseEntity<Void>> login() {
        String scope = "user-read-currently-playing user-read-playback-state user-modify-playback-state";

        String spotifyAuthUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + UriUtils.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + UriUtils.encode(scope, StandardCharsets.UTF_8);

        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, spotifyAuthUrl)
                .build());
    }

    @GetMapping(value = "/current-song", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentSong() {
        String accessToken = TokenStorage.getAccessToken();

        if (accessToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in. Please /login first.")));
        }

        return webClient.get()
                .uri("https://api.spotify.com/v1/me/player/currently-playing")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.value() == 204, clientResponse -> {
                    return Mono.error(new RuntimeException("No content"));
                })
                .bodyToMono(Map.class)
                .map(body -> ResponseEntity.ok().body((Map<String, Object>) body))
                .onErrorResume(e -> {
                    if (e.getMessage().contains("No content")) {
                        return Mono.just(ResponseEntity.ok(Map.of("message", "No song currently playing")));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    @PostMapping("/skip-next")
    public Mono<ResponseEntity<Map<String, Object>>> skipToNext() {
        String accessToken = TokenStorage.getAccessToken();

        if (accessToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in. Please /login first.")));
        }

        return webClient.post()
                .uri("https://api.spotify.com/v1/me/player/next")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> ResponseEntity.ok(Map.<String, Object>of("message", "Skipped to next song")))
                .onErrorResume(e -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Failed to get track info");
                    errorResponse.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    @PostMapping("/skip-previous")
    public Mono<ResponseEntity<Map<String, Object>>> skipToPrevious() {
        String accessToken = TokenStorage.getAccessToken();

        if (accessToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in. Please /login first.")));
        }

        return webClient.post()
                .uri("https://api.spotify.com/v1/me/player/previous")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> ResponseEntity.ok(Map.<String, Object>of("message", "Skipped to previous song")))
                .onErrorResume(e -> {
                    System.err.println("Skip previous error: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to skip to previous song: " + e.getMessage())));
                });
    }

    @PostMapping("/play-pause")
    public Mono<ResponseEntity<Map<String, Object>>> togglePlayPause() {
        String accessToken = TokenStorage.getAccessToken();

        if (accessToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in. Please /login first.")));
        }

        // First check current playback state
        return webClient.get()
                .uri("https://api.spotify.com/v1/me/player")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(playbackState -> {
                    Boolean isPlaying = (Boolean) playbackState.get("is_playing");
                    String endpoint = (isPlaying != null && isPlaying) ? "pause" : "play";

                    return webClient.put()
                            .uri("https://api.spotify.com/v1/me/player/" + endpoint)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .toBodilessEntity()
                            .map(response -> ResponseEntity.ok(Map.<String, Object>of("message",
                                    endpoint.equals("pause") ? "Playback paused" : "Playback resumed")));
                })
                .onErrorResume(e -> {
                    System.err.println("Play/Pause error: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to toggle playback: " + e.getMessage())));
                });
    }

    @PostMapping("/set-volume")
    public Mono<ResponseEntity<Map<String, Object>>> setVolume(@RequestParam int volume) {
        String accessToken = TokenStorage.getAccessToken();

        if (accessToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not logged in. Please /login first.")));
        }

        // Ensure volume is between 0 and 100
        int clampedVolume = Math.max(0, Math.min(100, volume));

        return webClient.put()
                .uri("https://api.spotify.com/v1/me/player/volume?volume_percent=" + clampedVolume)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> ResponseEntity.ok(Map.<String, Object>of("message", "Volume set to " + clampedVolume + "%")))
                .onErrorResume(e -> {
                    System.err.println("Volume error: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to set volume: " + e.getMessage())));
                });
    }
}