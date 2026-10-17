package br.com.cuida.auth;

import br.com.cuida.shared.security.IssuedToken;
import br.com.cuida.shared.security.TokenIssuer;
import br.com.cuida.shared.security.UserRole;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AuthService {

  /** O BCrypt ignora o que passa de 72 bytes; senhas maiores são recusadas na entrada. */
  static final int MAX_PASSWORD_BYTES = 72;

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final TokenIssuer tokenIssuer;

  /**
   * Hash usado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos (não
   * revela quais e-mails estão cadastrados).
   */
  private final String dummyHash;

  AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.tokenIssuer = tokenIssuer;
    this.dummyHash = passwordEncoder.encode("cuida-dummy-password");
  }

  @Transactional
  AuthResponse register(String name, String email, String password) {
    requirePasswordWithinLimit(password);
    String normalized = User.normalizeEmail(email);
    if (users.existsByEmail(normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
    }
    User user =
        users.save(new User(name, normalized, passwordEncoder.encode(password), UserRole.TUTOR));
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  AuthResponse login(String email, String password) {
    Optional<User> user = users.findByEmail(User.normalizeEmail(email));
    String hash = user.map(User::getPasswordHash).orElse(dummyHash);
    boolean matches = passwordEncoder.matches(password, hash);
    if (user.isEmpty() || !matches) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
    }
    return toResponse(user.get());
  }

  /** Cria um administrador se o e-mail ainda não existir. Usado só na inicialização. */
  @Transactional
  boolean createAdminIfAbsent(String name, String email, String password) {
    requirePasswordWithinLimit(password);
    String normalized = User.normalizeEmail(email);
    if (users.existsByEmail(normalized)) {
      return false;
    }
    users.save(new User(name, normalized, passwordEncoder.encode(password), UserRole.ADMIN));
    return true;
  }

  private AuthResponse toResponse(User user) {
    IssuedToken token = tokenIssuer.issue(user.getId(), user.getRole());
    return new AuthResponse(
        token.token(),
        token.expiresIn(),
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole());
  }

  private static void requirePasswordWithinLimit(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A senha deve ter no máximo " + MAX_PASSWORD_BYTES + " bytes");
    }
  }
}
