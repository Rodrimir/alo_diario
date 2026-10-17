package br.com.cuida.questionario;

import br.com.cuida.questionario.model.ItemQuestionarioRequestDTO;
import br.com.cuida.questionario.model.ItemQuestionarioResponseDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elderly/{elderlyId}/questionnaire")
class QuestionarioController {

  private final QuestionarioService questionarioService;

  QuestionarioController(QuestionarioService questionarioService) {
    this.questionarioService = questionarioService;
  }

  @GetMapping
  List<ItemQuestionarioResponseDTO> list(@PathVariable UUID elderlyId) {
    return questionarioService.listar(elderlyId);
  }

  /** Substitui a lista inteira de perguntas (a ordem da lista é a ordem da conversa). */
  @PutMapping
  List<ItemQuestionarioResponseDTO> replace(
      @PathVariable UUID elderlyId, @RequestBody List<ItemQuestionarioRequestDTO> request) {
    return questionarioService.substituir(elderlyId, request);
  }
}
