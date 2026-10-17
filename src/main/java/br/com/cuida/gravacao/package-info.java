/**
 * Armazenamento das gravações (porta {@code ArmazenamentoDeAudio}: S3/MinIO ou disco), cópia da
 * gravação do provedor, leitura para o player do tutor, exclusão LGPD e retenção.
 */
@ApplicationModule(
    displayName = "Gravação",
    allowedDependencies = {"infra", "ligacao", "ligacao :: model", "idoso :: model"})
package br.com.cuida.gravacao;

import org.springframework.modulith.ApplicationModule;
