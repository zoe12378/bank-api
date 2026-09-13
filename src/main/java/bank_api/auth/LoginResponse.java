package bank_api.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInMinutes) {
}
