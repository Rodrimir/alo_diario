/** Armazenamento de áudio (porta {@code AudioStorage} + S3/MinIO), URL pré-assinada e retenção. */
@ApplicationModule(
    displayName = "Recording",
    allowedDependencies = {"shared", "call", "elderly"})
package br.com.cuida.recording;

import org.springframework.modulith.ApplicationModule;
