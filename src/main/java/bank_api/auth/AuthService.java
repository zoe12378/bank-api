package bank_api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import bank_api.security.JwtService;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "使用者名稱已被使用");
        }

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()));

        return RegisterResponse.from(userRepository.save(user));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return createLoginResponse(user);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        AppUser user = refreshTokenService.rotate(request.refreshToken());
        return createLoginResponse(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private LoginResponse createLoginResponse(AppUser user) {
        return new LoginResponse(
                jwtService.generateToken(user),
                refreshTokenService.issue(user),
                "Bearer",
                jwtService.getExpirationMinutes(),
                refreshTokenService.getExpirationDays());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
    }
}
