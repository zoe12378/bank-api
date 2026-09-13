package bank_api.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMinutes,
        long refreshExpiresInDays,
        String username,
        String role) {
}
