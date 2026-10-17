package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.LigacaoDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de ligações para os outros módulos (API pública). */
@Service
@Transactional(readOnly = true)
public class ConsultaLigacaoService {

  private final LigacaoRepository ligacaoRepository;

  ConsultaLigacaoService(LigacaoRepository ligacaoRepository) {
    this.ligacaoRepository = ligacaoRepository;
  }

  public Optional<LigacaoDTO> find(UUID callId) {
    return ligacaoRepository.findById(callId).map(Ligacao::toDTO);
  }

  /** Ligações de um idoso entre duas datas (inclusive), na ordem em que aconteceram. */
  public List<LigacaoDTO> findByElderly(UUID elderlyId, LocalDate from, LocalDate to) {
    return ligacaoRepository
        .findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscCreatedAtAsc(elderlyId, from, to)
        .stream()
        .map(Ligacao::toDTO)
        .toList();
  }
}
