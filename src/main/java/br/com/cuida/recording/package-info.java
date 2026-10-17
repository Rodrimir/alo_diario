/**
 * Armazenamento das gravações (porta {@code AudioStorage}: S3/MinIO ou disco local), cópia da
 * gravação do provedor, leitura para o player do tutor, exclusão LGPD e retenção.
 */
@ApplicationModule(
    displayName = "Recording",
    allowedDependencies = {"shared", "call", "elderly"})
package br.com.cuida.recording;

import org.springframework.modulith.ApplicationModule;
