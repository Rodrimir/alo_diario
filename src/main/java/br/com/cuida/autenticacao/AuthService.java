package br.com.cuida.autenticacao;

import br.com.cuida.autenticacao.model.AuthResponseDTO;
import br.com.cuida.autenticacao.model.Usuario;
import br.com.cuida.infra.exception.ConflitoException;
import br.com.cuida.infra.exception.NaoAutenticadoException;
import br.com.cuida.infra.exception.ValidacaoException;
import br.com.cuida.infra.jwt.TokenEmitido;
import br.com.cuida.infra.jwt.TokenService;
import br.com.cuida.infra.security.PapelUsuario;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

  /** O BCrypt ignora o que passa de 72 bytes; senhas maiores são recusadas na entrada. */
  static final int MAX_PASSWORD_BYTES = 72;

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  /**
   * Hash usado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos (não
   * revela quais e-mails estão cadastrados).
   */
  private final String dummyHash;

  AuthService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.dummyHash = passwordEncoder.encode("cuida-dummy-password");
  }

  @Transactional
  AuthResponseDTO register(String name, String email, String password) {
    requirePasswordWithinLimit(password);
    String normalized = Usuario.normalizeEmail(email);
    if (usuarioRepository.existsByEmail(normalized)) {
      throw new ConflitoException("E-mail já cadastrado");
    }
    Usuario user =
        usuarioRepository.save(
            new Usuario(name, normalized, passwordEncoder.encode(password), PapelUsuario.TUTOR));
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  AuthResponseDTO login(String email, String password) {
    Optional<Usuario> user = usuarioRepository.findByEmail(Usuario.normalizeEmail(email));
    String hash = user.map(Usuario::getPasswordHash).orElse(dummyHash);
    boolean matches = passwordEncoder.matches(password, hash);
    if (user.isEmpty() || !matches) {
      throw new NaoAutenticadoException("E-mail ou senha inválidos");
    }
    return toResponse(user.get());
  }

  /** Cria um administrador se o e-mail ainda não existir. Usado só na inicialização. */
  @Transactional
  boolean createAdminIfAbsent(String name, String email, String password) {
    requirePasswordWithinLimit(password);
    String normalized = Usuario.normalizeEmail(email);
    if (usuarioRepository.existsByEmail(normalized)) {
      return false;
    }
    usuarioRepository.save(
        new Usuario(name, normalized, passwordEncoder.encode(password), PapelUsuario.ADMIN));
    return true;
  }

  private AuthResponseDTO toResponse(Usuario user) {
    TokenEmitido token = tokenService.issue(user.getId(), user.getRole());
    return new AuthResponseDTO(
        token.token(),
        token.expiresIn(),
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole());
  }

  private static void requirePasswordWithinLimit(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
      throw ValidacaoException.of(
          "password", "a senha deve ter no máximo " + MAX_PASSWORD_BYTES + " bytes");
    }
  }
}
