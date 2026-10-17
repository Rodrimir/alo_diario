/** Recepção idempotente de webhooks dos provedores (gravados antes de processar). */
@ApplicationModule(displayName = "Webhook", allowedDependencies = "infra")
package br.com.cuida.webhook;

import org.springframework.modulith.ApplicationModule;
