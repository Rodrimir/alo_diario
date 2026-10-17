package br.com.cuida.questionario;

import br.com.cuida.questionario.model.ItemQuestionario;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ItemQuestionarioRepository extends JpaRepository<ItemQuestionario, UUID> {

  List<ItemQuestionario> findByElderlyIdOrderByPositionAsc(UUID elderlyId);

  boolean existsByElderlyId(UUID elderlyId);
}
