package br.com.cuida.autenticacao;

import br.com.cuida.autenticacao.model.AuthResponseDTO;
import br.com.cuida.autenticacao.model.LoginRequestDTO;
import br.com.cuida.autenticacao.model.RegisterRequestDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class AuthController {

  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  AuthResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
    return authService.register(request.name(), request.email(), request.password());
  }

  @PostMapping("/login")
  AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
    return authService.login(request.email(), request.password());
  }
}
