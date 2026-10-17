package br.com.cuida.call;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import br.com.cuida.questionnaire.Questionnaires;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * "Ligar agora": sempre permitido, exceto sem consentimento (422) ou com outra ligação em andamento
 * para o mesmo idoso (409). Não conta para o limite diário nem cancela a ligação agendada.
 */
@Service
class ManualCallService {

  private final ElderlyDirectory elderlyDirectory;
  private final Questionnaires questionnaires;
  private final CallRepository repository;
  private final CallDispatcher dispatcher;
  private final TransactionTemplate transactions;
  private final Clock clock;

  ManualCallService(
      ElderlyDirectory elderlyDirectory,
      Questionnaires questionnaires,
      CallRepository repository,
      CallDispatcher dispatcher,
      TransactionTemplate transactions,
      Clock clock) {
    this.elderlyDirectory = elderlyDirectory;
    this.questionnaires = questionnaires;
    this.repository = repository;
    this.dispatcher = dispatcher;
    this.transactions = transactions;
    this.clock = clock;
  }

  CallResponse callNow(UUID elderlyId) {
    ElderlyView elderly = elderlyDirectory.requireAccessible(elderlyId);
    if (!elderly.recordingConsent()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_CONTENT,
          "Sem consentimento de gravação: registre em PUT /api/elderly/{id}/consent");
    }
    if (repository.existsByElderlyIdAndStatusIn(elderlyId, Call.ACTIVE)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Já existe uma ligação em andamento para este idoso");
    }
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, elderly.zone());
    Call call =
        transactions.execute(
            status -> {
              int attempt =
                  (int)
                          repository.countByElderlyIdAndLocalDateAndTrigger(
                              elderlyId, today, CallTrigger.MANUAL)
                      + 1;
              return repository.saveAndFlush(
                  new Call(
                      elderlyId,
                      CallTrigger.MANUAL,
                      now,
                      today,
                      attempt,
                      questionnaires.snapshot(elderlyId)));
            });
    dispatcher.dispatch(call.getId(), now);
    CallView view = repository.findById(call.getId()).map(Call::toView).orElseThrow();
    return CallResponse.of(view, elderly.zone());
  }
}
