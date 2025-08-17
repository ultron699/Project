package com.spotify.widget.token;

public class TokenStorage {
    private static String accessToken;
    private static String refreshToken;

    public static String getAccessToken() {
        return accessToken;
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    public static String getRefreshToken() {
        return refreshToken;
    }

    public static void setRefreshToken(String token) {
        refreshToken = token;
    }

    public static boolean hasValidToken() {
        return accessToken != null && !accessToken.isEmpty();
    }

    public static void clearTokens() {
        accessToken = null;
        refreshToken = null;
    }
}