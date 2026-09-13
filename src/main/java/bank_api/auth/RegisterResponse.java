package bank_api.auth;

public record RegisterResponse(Long id, String username, String role) {

    public static RegisterResponse from(AppUser user) {
        return new RegisterResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
