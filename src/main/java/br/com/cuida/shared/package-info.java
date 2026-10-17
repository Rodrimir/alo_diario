/**
 * Infraestrutura transversal: segurança, web/erros, OpenAPI, configuração, persistência e logging.
 *
 * <p>Módulo aberto (todas as classes podem ser usadas pelas features), mas sem nenhuma dependência
 * de feature: {@code allowedDependencies = {}}.
 */
@ApplicationModule(
    displayName = "Shared",
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = {})
package br.com.cuida.shared;

import org.springframework.modulith.ApplicationModule;
