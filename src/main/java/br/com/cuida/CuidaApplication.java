package br.com.cuida;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do backend.
 *
 * <p>O código é organizado por funcionalidade (package by feature): cada subpacote direto de {@code
 * br.com.cuida} é um módulo do Spring Modulith, e {@code infra} contém apenas o que é transversal.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CuidaApplication {

  public static void main(String[] args) {
    SpringApplication.run(CuidaApplication.class, args);
  }
}
