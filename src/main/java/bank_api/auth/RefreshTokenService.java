package bank_api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Refresh token 採用輪替：每次刷新都撤銷舊 token，並發出新的一組 token。
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issue(AppUser user) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        refreshTokenRepository.save(new RefreshToken(
                user,
                hash(rawToken),
                LocalDateTime.now().plusDays(refreshExpirationDays)));
        return rawToken;
    }

    public AppUser rotate(String rawToken) {
        RefreshToken storedToken = findActiveToken(rawToken);
        storedToken.revoke();
        return storedToken.getUser();
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                hash(rawToken), LocalDateTime.now())
                .ifPresent(RefreshToken::revoke);
    }

    public long getExpirationDays() {
        return refreshExpirationDays;
    }

    private RefreshToken findActiveToken(String rawToken) {
        return refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                hash(rawToken), LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token 無效或已過期"));
    }

    private String hash(String rawToken) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 演算法不可用", exception);
        }
    }
}
