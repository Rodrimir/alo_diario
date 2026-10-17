package br.com.cuida.questionnaire;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.shared.web.ApiError;
import br.com.cuida.shared.web.InvalidRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elderly/{elderlyId}/questionnaire")
class QuestionnaireController {

  static final int MAX_ITEMS = 12;

  private final QuestionnaireItemRepository repository;
  private final ElderlyDirectory elderlyDirectory;
  private final Validator validator;

  QuestionnaireController(
      QuestionnaireItemRepository repository,
      ElderlyDirectory elderlyDirectory,
      Validator validator) {
    this.repository = repository;
    this.elderlyDirectory = elderlyDirectory;
    this.validator = validator;
  }

  @GetMapping
  @Transactional(readOnly = true)
  List<QuestionnaireItemResponse> list(@PathVariable UUID elderlyId) {
    elderlyDirectory.requireAccessible(elderlyId);
    return repository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
        .map(QuestionnaireItemResponse::of)
        .toList();
  }

  /**
   * Substitui a lista inteira. Itens com a mesma {@code attributeKey} são atualizados (mantêm o
   * id), os novos são criados e os que saíram da lista são apagados. Ligações já feitas não mudam:
   * cada uma guarda o snapshot das perguntas da época.
   */
  @PutMapping
  @Transactional
  List<QuestionnaireItemResponse> replace(
      @PathVariable UUID elderlyId, @RequestBody List<QuestionnaireItemRequest> request) {
    elderlyDirectory.requireAccessible(elderlyId);
    validate(request);

    Map<String, QuestionnaireItem> existing =
        repository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
            .collect(
                Collectors.toMap(
                    QuestionnaireItem::getAttributeKey,
                    Function.identity(),
                    (a, b) -> a,
                    LinkedHashMap::new));
    Set<String> keep = new HashSet<>();
    List<QuestionnaireItem> toSave = new ArrayList<>();
    for (int position = 0; position < request.size(); position++) {
      QuestionnaireItemRequest item = request.get(position);
      boolean active = item.active() == null || item.active();
      keep.add(item.attributeKey());
      QuestionnaireItem current = existing.get(item.attributeKey());
      if (current != null) {
        current.update(item.questionText(), item.answerType(), position, active);
        toSave.add(current);
      } else {
        toSave.add(
            new QuestionnaireItem(
                elderlyId,
                item.attributeKey(),
                item.questionText().trim(),
                item.answerType(),
                position,
                active));
      }
    }
    existing.values().stream()
        .filter(item -> !keep.contains(item.getAttributeKey()))
        .forEach(repository::delete);
    repository.flush();
    repository.saveAllAndFlush(toSave);

    return repository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
        .map(QuestionnaireItemResponse::of)
        .toList();
  }

  private void validate(List<QuestionnaireItemRequest> request) {
    if (request == null) {
      throw InvalidRequestException.of("items", "envie a lista de perguntas");
    }
    List<ApiError.FieldError> errors = new ArrayList<>();
    if (request.size() > MAX_ITEMS) {
      errors.add(new ApiError.FieldError("items", "no máximo " + MAX_ITEMS + " perguntas"));
    }
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < request.size(); i++) {
      QuestionnaireItemRequest item = request.get(i);
      if (item == null) {
        errors.add(new ApiError.FieldError("[" + i + "]", "item vazio"));
        continue;
      }
      for (ConstraintViolation<QuestionnaireItemRequest> violation : validator.validate(item)) {
        errors.add(
            new ApiError.FieldError(
                "[" + i + "]." + violation.getPropertyPath(), violation.getMessage()));
      }
      if (item.attributeKey() != null && !keys.add(item.attributeKey())) {
        errors.add(new ApiError.FieldError("[" + i + "].attributeKey", "chave repetida"));
      }
    }
    if (!errors.isEmpty()) {
      throw new InvalidRequestException(errors);
    }
  }
}
