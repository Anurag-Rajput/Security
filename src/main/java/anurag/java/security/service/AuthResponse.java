package anurag.java.security.service;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
}
