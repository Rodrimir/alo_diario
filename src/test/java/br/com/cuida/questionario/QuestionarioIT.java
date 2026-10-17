package br.com.cuida.questionario;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@AutoConfigureMockMvc
class QuestionarioIT {

  @Autowired MockMvcTester mvc;

  @Autowired QuestionarioService questionarioService;

  ApiTestClient api;
  String token;
  UUID elderlyId;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
    elderlyId = TestData.createElderly(api, token, "11987654321");
  }

  @Test
  void replaceKeepsIdsOfExistingKeysAndRemovesTheRest() {
    String sleepId =
        ApiTestClient.read(api.get(token, "/api/elderly/{id}/questionnaire", elderlyId), "$[0].id");

    MvcTestResult result =
        api.put(
            token,
            "/api/elderly/{id}/questionnaire",
            """
            [
              {"attributeKey": "water", "questionText": "Bebeu água hoje?", "answerType": "YES_NO"},
              {"attributeKey": "sleep", "questionText": "Dormiu bem?", "answerType": "SCALE_1_5"},
              {"attributeKey": "walk", "questionText": "Fez a caminhada?", "answerType": "YES_NO",
               "active": false}
            ]
            """,
            elderlyId);

    assertThat(result).hasStatusOk();
    assertThat(result)
        .bodyJson()
        .extractingPath("$[*].attributeKey")
        .asArray()
        .containsExactly("water", "sleep", "walk");
    assertThat(result).bodyJson().extractingPath("$[1].id").isEqualTo(sleepId);
    assertThat(result).bodyJson().extractingPath("$[1].position").isEqualTo(1);
    assertThat(questionarioService.snapshot(elderlyId).items())
        .extracting(QuestionarioSnapshot.Item::attributeKey)
        .containsExactly("water", "sleep");
  }

  @Test
  void rejectsDuplicateKeysWithIndexedFieldErrors() {
    MvcTestResult result =
        api.put(
            token,
            "/api/elderly/{id}/questionnaire",
            """
            [
              {"attributeKey": "sleep", "questionText": "Dormiu?", "answerType": "YES_NO"},
              {"attributeKey": "sleep", "questionText": "", "answerType": "YES_NO"}
            ]
            """,
            elderlyId);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactlyInAnyOrder("[1].questionText", "[1].attributeKey");
  }

  @Test
  void rejectsMoreThanTwelveItems() {
    String items =
        IntStream.range(0, 13)
            .mapToObj(
                i ->
                    """
                    {"attributeKey": "q%d", "questionText": "Pergunta %d", "answerType": "YES_NO"}
                    """
                        .formatted(i, i))
            .collect(Collectors.joining(",", "[", "]"));

    MvcTestResult result = api.put(token, "/api/elderly/{id}/questionnaire", items, elderlyId);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("items");
  }
}
