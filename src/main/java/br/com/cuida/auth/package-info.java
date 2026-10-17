/**
 * Registro e login de tutores, emissão de JWT e papéis ({@code ROLE_TUTOR}, {@code ROLE_ADMIN}).
 */
@ApplicationModule(displayName = "Auth", allowedDependencies = "shared")
package br.com.cuida.auth;

import org.springframework.modulith.ApplicationModule;
