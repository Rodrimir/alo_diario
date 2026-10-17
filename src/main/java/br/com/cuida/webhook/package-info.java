/** Recepção idempotente de webhooks dos provedores (inbox em {@code webhook_events}). */
@ApplicationModule(displayName = "Webhook", allowedDependencies = "shared")
package br.com.cuida.webhook;

import org.springframework.modulith.ApplicationModule;
