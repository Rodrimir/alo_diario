/**
 * Infraestrutura transversal: segurança, JWT, exceções, documentação, persistência, agendamento,
 * eventos e utilitários.
 *
 * <p>Módulo aberto (todas as classes podem ser usadas pelas funcionalidades), mas sem nenhuma
 * dependência de funcionalidade: {@code allowedDependencies = {}}.
 */
@ApplicationModule(
    displayName = "Infra",
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = {})
package br.com.cuida.infra;

import org.springframework.modulith.ApplicationModule;
