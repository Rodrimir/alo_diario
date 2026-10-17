package br.com.cuida.notificacao;

import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.infra.exception.RecursoNaoEncontradoException;
import br.com.cuida.infra.exception.RegraDeNegocioException;
import br.com.cuida.ligacao.ConsultaLigacaoService;
import br.com.cuida.ligacao.FormatadorDeResumo;
import br.com.cuida.ligacao.LigacaoProperties;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.LigacaoProcessada;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.TentativasEsgotadas;
import br.com.cuida.notificacao.model.Notificacao;
import br.com.cuida.notificacao.model.NotificacaoResponseDTO;
import br.com.cuida.notificacao.model.TipoNotificacao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Avisa a família pelo WhatsApp: o resumo de cada ligação processada e o "não conseguimos falar"
 * quando as tentativas do dia acabam. Cada aviso é registrado antes do envio, com uma chave única
 * que impede envio duplicado (o outbox pode entregar o mesmo evento mais de uma vez).
 */
@Service
public class NotificacaoService {

  private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
  private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

  private final NotificacaoRepository notificacaoRepository;
  private final WhatsAppService whatsAppService;
  private final ConsultaLigacaoService consultaLigacaoService;
  private final AcessoIdosoService acessoIdosoService;
  private final FormatadorDeResumo formatadorDeResumo;
  private final LigacaoProperties ligacaoProperties;
  private final NotificacaoProperties notificacaoProperties;
  private final TransactionTemplate transactions;
  private final Clock clock;

  NotificacaoService(
      NotificacaoRepository notificacaoRepository,
      WhatsAppService whatsAppService,
      ConsultaLigacaoService consultaLigacaoService,
      AcessoIdosoService acessoIdosoService,
      FormatadorDeResumo formatadorDeResumo,
      LigacaoProperties ligacaoProperties,
      NotificacaoProperties notificacaoProperties,
      TransactionTemplate transactions,
      Clock clock) {
    this.notificacaoRepository = notificacaoRepository;
    this.whatsAppService = whatsAppService;
    this.consultaLigacaoService = consultaLigacaoService;
    this.acessoIdosoService = acessoIdosoService;
    this.formatadorDeResumo = formatadorDeResumo;
    this.ligacaoProperties = ligacaoProperties;
    this.notificacaoProperties = notificacaoProperties;
    this.transactions = transactions;
    this.clock = clock;
  }

  @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
  void on(LigacaoProcessada event) {
    avisarResumo(event.callId(), TipoNotificacao.CALL_SUMMARY.name() + ":" + event.callId());
  }

  @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
  void on(TentativasEsgotadas event) {
    Optional<IdosoDTO> found = acessoIdosoService.find(event.elderlyId());
    if (found.isEmpty()) {
      return;
    }
    IdosoDTO idoso = found.get();
    String nome = idoso.nameForConversation();
    String data = event.localDate().format(DIA_MES);
    String conteudo =
        cabecalho(nome, event.localDate())
            + "\n❗ Não conseguimos falar com "
            + nome
            + " hoje, depois de "
            + event.attempts()
            + " tentativa(s). Vale a pena entrar em contato.";
    Map<String, String> variaveis = new LinkedHashMap<>();
    variaveis.put("nome", nome);
    variaveis.put("data", data);
    variaveis.put("tentativas", String.valueOf(event.attempts()));
    registrarEEnviar(
        idoso,
        event.lastCallId(),
        TipoNotificacao.NO_CONTACT,
        notificacaoProperties.templates().semContato(),
        conteudo,
        variaveis,
        TipoNotificacao.NO_CONTACT.name() + ":" + event.elderlyId() + ":" + event.localDate());
  }

  /** Avisos de uma ligação, para o tutor/admin com acesso ao idoso. */
  List<NotificacaoResponseDTO> listar(UUID callId) {
    IdosoDTO idoso = requireAccess(callId);
    return notificacaoRepository.findByCallIdOrderByCreatedAtAsc(callId).stream()
        .map(n -> NotificacaoResponseDTO.of(n, idoso.zone()))
        .toList();
  }

  /** Envia de novo o resumo de uma ligação já processada (ex.: o responsável trocou o WhatsApp). */
  NotificacaoResponseDTO reenviar(UUID callId) {
    requireAccess(callId);
    LigacaoDTO ligacao = consultaLigacaoService.find(callId).orElseThrow();
    if (ligacao.summary() == null) {
      throw new RegraDeNegocioException("A ligação ainda não tem resumo para enviar");
    }
    UUID id =
        avisarResumo(
                callId,
                TipoNotificacao.CALL_SUMMARY.name() + ":" + callId + ":" + UUID.randomUUID())
            .orElseThrow();
    Notificacao notificacao = notificacaoRepository.findById(id).orElseThrow();
    IdosoDTO idoso = acessoIdosoService.find(ligacao.elderlyId()).orElseThrow();
    return NotificacaoResponseDTO.of(notificacao, idoso.zone());
  }

  /** Último aviso de resumo de uma ligação (para o detalhe da ligação). */
  public Optional<NotificacaoResponseDTO> ultimoResumo(UUID callId, ZoneId zone) {
    return notificacaoRepository
        .findFirstByCallIdAndKindOrderByCreatedAtDesc(callId, TipoNotificacao.CALL_SUMMARY)
        .map(n -> NotificacaoResponseDTO.of(n, zone));
  }

  /** Tenta de novo um aviso que falhou (usado pelo job). */
  void reenviarFalha(UUID notificacaoId) {
    notificacaoRepository.findById(notificacaoId).ifPresent(this::enviar);
  }

  private Optional<UUID> avisarResumo(UUID callId, String dedupKey) {
    Optional<LigacaoDTO> found = consultaLigacaoService.find(callId);
    if (found.isEmpty() || found.get().summary() == null) {
      return Optional.empty();
    }
    LigacaoDTO ligacao = found.get();
    Optional<IdosoDTO> idoso = acessoIdosoService.find(ligacao.elderlyId());
    if (idoso.isEmpty()) {
      return Optional.empty();
    }
    ResumoLigacao resumo = ligacao.summary();
    String nome = idoso.get().nameForConversation();
    String conteudo = formatadorDeResumo.formatar(resumo, nome, ligacao.localDate()).report();
    Map<String, String> variaveis = new LinkedHashMap<>();
    variaveis.put("nome", nome);
    variaveis.put("data", ligacao.localDate().format(DIA_MES));
    variaveis.put("alerta", alerta(resumo));
    variaveis.put("itens", formatadorDeResumo.checklistEmUmaLinha(resumo));
    variaveis.put(
        "recado",
        resumo.familyMessage() == null || resumo.familyMessage().isBlank()
            ? "Sem recado"
            : resumo.familyMessage());
    return registrarEEnviar(
        idoso.get(),
        callId,
        TipoNotificacao.CALL_SUMMARY,
        notificacaoProperties.templates().resumo(),
        conteudo,
        variaveis,
        dedupKey);
  }

  private Optional<UUID> registrarEEnviar(
      IdosoDTO idoso,
      UUID callId,
      TipoNotificacao tipo,
      String template,
      String conteudo,
      Map<String, String> variaveis,
      String dedupKey) {
    Map<String, String> limpas = new LinkedHashMap<>();
    variaveis.forEach((k, v) -> limpas.put(k, umaLinha(v)));
    Notificacao notificacao =
        new Notificacao(
            idoso.id(),
            callId,
            tipo,
            idoso.guardianWhatsapp(),
            template,
            conteudo,
            limpas,
            dedupKey);
    if (idoso.guardianWhatsapp() == null) {
      notificacao.markSkipped("Idoso sem WhatsApp do responsável cadastrado");
    }
    try {
      Boolean nova =
          transactions.execute(
              status -> {
                if (notificacaoRepository.existsByDedupKey(dedupKey)) {
                  return false;
                }
                notificacaoRepository.saveAndFlush(notificacao);
                return true;
              });
      if (!Boolean.TRUE.equals(nova)) {
        return Optional.empty();
      }
    } catch (DataIntegrityViolationException jaRegistrada) {
      return Optional.empty();
    }
    if (idoso.guardianWhatsapp() != null) {
      enviar(notificacao);
    }
    return Optional.of(notificacao.getId());
  }

  private void enviar(Notificacao notificacao) {
    String id = null;
    String erro = null;
    try {
      id = whatsAppService.enviarTemplate(notificacao.toMensagem());
    } catch (RuntimeException e) {
      erro = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      log.warn("Falha ao enviar o aviso {} pelo WhatsApp: {}", notificacao.getId(), erro);
    }
    String providerId = id;
    String falha = erro;
    transactions.executeWithoutResult(
        status ->
            notificacaoRepository
                .findById(notificacao.getId())
                .ifPresent(
                    n -> {
                      if (falha == null) {
                        n.markSent(providerId, clock.instant());
                      } else {
                        n.markFailed(falha);
                      }
                      notificacaoRepository.saveAndFlush(n);
                    }));
  }

  private IdosoDTO requireAccess(UUID callId) {
    RecursoNaoEncontradoException naoEncontrada =
        new RecursoNaoEncontradoException("Ligação não encontrada: " + callId);
    LigacaoDTO ligacao = consultaLigacaoService.find(callId).orElseThrow(() -> naoEncontrada);
    try {
      return acessoIdosoService.requireAccessible(ligacao.elderlyId());
    } catch (RecursoNaoEncontradoException e) {
      throw naoEncontrada;
    }
  }

  private String cabecalho(String nome, LocalDate data) {
    return ligacaoProperties.productName().toUpperCase(Locale.forLanguageTag("pt-BR"))
        + " – "
        + nome
        + "\nData: "
        + data.format(DIA_MES);
  }

  private static String alerta(ResumoLigacao resumo) {
    return switch (resumo.alertLevel()) {
      case URGENT -> "🚨 Atenção: " + String.join("; ", resumo.alertReasons());
      case ATTENTION -> "⚠️ Há pontos de atenção";
      case NONE -> "✅ Tudo bem";
    };
  }

  /** A Meta não aceita quebra de linha, tab nem mais de 4 espaços seguidos num parâmetro. */
  static String umaLinha(String valor) {
    if (valor == null || valor.isBlank()) {
      return "-";
    }
    return valor.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" {2,}", " ").trim();
  }
}
