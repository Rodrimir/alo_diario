package br.com.cuida.questionario;

import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoCadastrado;
import br.com.cuida.infra.exception.ErroResponseDTO;
import br.com.cuida.infra.exception.ValidacaoException;
import br.com.cuida.questionario.model.ItemQuestionario;
import br.com.cuida.questionario.model.ItemQuestionarioRequestDTO;
import br.com.cuida.questionario.model.ItemQuestionarioResponseDTO;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perguntas do idoso: leitura e substituição pelo tutor, perguntas sugeridas no cadastro e o
 * snapshot do questionário vigente, gravado em cada ligação.
 */
@Service
@Transactional(readOnly = true)
public class QuestionarioService {

  static final int MAX_ITEMS = 12;

  private final ItemQuestionarioRepository itemQuestionarioRepository;
  private final AcessoIdosoService acessoIdosoService;
  private final Validator validator;

  QuestionarioService(
      ItemQuestionarioRepository itemQuestionarioRepository,
      AcessoIdosoService acessoIdosoService,
      Validator validator) {
    this.itemQuestionarioRepository = itemQuestionarioRepository;
    this.acessoIdosoService = acessoIdosoService;
    this.validator = validator;
  }

  /** Perguntas ativas, na ordem em que devem ser feitas. */
  public QuestionarioSnapshot snapshot(UUID elderlyId) {
    List<QuestionarioSnapshot.Item> items =
        itemQuestionarioRepository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
            .filter(ItemQuestionario::isActive)
            .map(ItemQuestionario::toSnapshotItem)
            .toList();
    return new QuestionarioSnapshot(items);
  }

  /** Todo idoso nasce com as perguntas sugeridas. */
  @EventListener
  @Transactional
  void on(IdosoCadastrado event) {
    if (itemQuestionarioRepository.existsByElderlyId(event.elderlyId())) {
      return;
    }
    int position = 0;
    for (PerguntasSugeridas.Question question : PerguntasSugeridas.ITEMS) {
      itemQuestionarioRepository.save(
          new ItemQuestionario(
              event.elderlyId(),
              question.attributeKey(),
              question.questionText(),
              question.answerType(),
              position++,
              true));
    }
  }

  @Transactional(readOnly = true)
  List<ItemQuestionarioResponseDTO> listar(UUID elderlyId) {
    acessoIdosoService.requireAccessible(elderlyId);
    return itemQuestionarioRepository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
        .map(ItemQuestionarioResponseDTO::of)
        .toList();
  }

  /**
   * Substitui a lista inteira. Itens com a mesma {@code attributeKey} são atualizados (mantêm o
   * id), os novos são criados e os que saíram da lista são apagados. Ligações já feitas não mudam:
   * cada uma guarda o snapshot das perguntas da época.
   */
  @Transactional
  List<ItemQuestionarioResponseDTO> substituir(
      UUID elderlyId, List<ItemQuestionarioRequestDTO> request) {
    acessoIdosoService.requireAccessible(elderlyId);
    validate(request);

    Map<String, ItemQuestionario> existing =
        itemQuestionarioRepository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
            .collect(
                Collectors.toMap(
                    ItemQuestionario::getAttributeKey,
                    Function.identity(),
                    (a, b) -> a,
                    LinkedHashMap::new));
    Set<String> keep = new HashSet<>();
    List<ItemQuestionario> toSave = new ArrayList<>();
    for (int position = 0; position < request.size(); position++) {
      ItemQuestionarioRequestDTO item = request.get(position);
      boolean active = item.active() == null || item.active();
      keep.add(item.attributeKey());
      ItemQuestionario current = existing.get(item.attributeKey());
      if (current != null) {
        current.update(item.questionText(), item.answerType(), position, active);
        toSave.add(current);
      } else {
        toSave.add(
            new ItemQuestionario(
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
        .forEach(itemQuestionarioRepository::delete);
    itemQuestionarioRepository.flush();
    itemQuestionarioRepository.saveAllAndFlush(toSave);

    return itemQuestionarioRepository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
        .map(ItemQuestionarioResponseDTO::of)
        .toList();
  }

  private void validate(List<ItemQuestionarioRequestDTO> request) {
    if (request == null) {
      throw ValidacaoException.of("items", "envie a lista de perguntas");
    }
    List<ErroResponseDTO.FieldError> errors = new ArrayList<>();
    if (request.size() > MAX_ITEMS) {
      errors.add(new ErroResponseDTO.FieldError("items", "no máximo " + MAX_ITEMS + " perguntas"));
    }
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < request.size(); i++) {
      ItemQuestionarioRequestDTO item = request.get(i);
      if (item == null) {
        errors.add(new ErroResponseDTO.FieldError("[" + i + "]", "item vazio"));
        continue;
      }
      for (ConstraintViolation<ItemQuestionarioRequestDTO> violation : validator.validate(item)) {
        errors.add(
            new ErroResponseDTO.FieldError(
                "[" + i + "]." + violation.getPropertyPath(), violation.getMessage()));
      }
      if (item.attributeKey() != null && !keys.add(item.attributeKey())) {
        errors.add(new ErroResponseDTO.FieldError("[" + i + "].attributeKey", "chave repetida"));
      }
    }
    if (!errors.isEmpty()) {
      throw new ValidacaoException(errors);
    }
  }
}
