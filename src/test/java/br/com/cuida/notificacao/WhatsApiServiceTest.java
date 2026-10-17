package br.com.cuida.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.cuida.notificacao.model.MensagemWhatsAppDTO;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhatsApiServiceTest {

  static final NotificacaoProperties PROPERTIES =
      new NotificacaoProperties(
          "whatsapi",
          new NotificacaoProperties.WhatsApi("https://whats.exemplo.com", "chave-empresa", "pt_BR"),
          new NotificacaoProperties.Templates(
              "alo_diario_resumo", "alo_diario_sem_contato", "alo_diario_alerta"));

  MockRestServiceServer server;
  WhatsApiService service;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    service = new WhatsApiService(builder, PROPERTIES);
  }

  @Test
  void sendsTheTemplateInTheWhatsApiFormat() {
    server
        .expect(requestTo("https://whats.exemplo.com/api/messages/template"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer chave-empresa"))
        .andExpect(jsonPath("$.phoneNumber").value("+5511912345678"))
        .andExpect(jsonPath("$.templateName").value("alo_diario_resumo"))
        .andExpect(jsonPath("$.languageCode").value("pt_BR"))
        .andExpect(jsonPath("$.variables[0].key").value("nome"))
        .andExpect(jsonPath("$.variables[0].value").value("Dona Maria"))
        .andRespond(
            withSuccess(
                "{\"id\": \"3f2c\", \"whatsappMessageId\": \"wamid.ABC\", \"status\": \"SENT\"}",
                MediaType.APPLICATION_JSON));

    Map<String, String> variaveis = new LinkedHashMap<>();
    variaveis.put("nome", "Dona Maria");
    variaveis.put("data", "17/10");

    assertThat(
            service.enviarTemplate(
                new MensagemWhatsAppDTO("+5511912345678", "alo_diario_resumo", variaveis)))
        .isEqualTo("wamid.ABC");
    server.verify();
  }

  @Test
  void rejectionBecomesEnvioWhatsAppException() {
    server
        .expect(requestTo("https://whats.exemplo.com/api/messages/template"))
        .andRespond(withBadRequest());

    assertThatThrownBy(
            () ->
                service.enviarTemplate(
                    new MensagemWhatsAppDTO("+5511912345678", "alo_diario_resumo", Map.of())))
        .isInstanceOf(EnvioWhatsAppException.class);
  }

  @Test
  void requiresUrlAndApiKey() {
    NotificacaoProperties incompleta =
        new NotificacaoProperties(
            "whatsapi",
            new NotificacaoProperties.WhatsApi("", "", "pt_BR"),
            PROPERTIES.templates());

    assertThatThrownBy(() -> new WhatsApiService(RestClient.builder(), incompleta))
        .hasMessageContaining("WHATSAPI_BASE_URL");
  }

  @Test
  void templateParametersHaveNoLineBreaks() {
    assertThat(NotificacaoService.umaLinha("linha 1\nlinha 2\t\tfim     ok"))
        .isEqualTo("linha 1 linha 2 fim ok");
    assertThat(NotificacaoService.umaLinha("  ")).isEqualTo("-");
  }
}
