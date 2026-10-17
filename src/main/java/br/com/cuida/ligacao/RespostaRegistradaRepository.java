package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.RespostaRegistrada;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RespostaRegistradaRepository extends JpaRepository<RespostaRegistrada, UUID> {

  Optional<RespostaRegistrada> findByCallIdAndAttributeKey(UUID callId, String attributeKey);

  List<RespostaRegistrada> findByCallIdOrderByRecordedAtAsc(UUID callId);
}
