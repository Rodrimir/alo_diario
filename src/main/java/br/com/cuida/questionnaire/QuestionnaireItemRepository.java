package br.com.cuida.questionnaire;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface QuestionnaireItemRepository extends JpaRepository<QuestionnaireItem, UUID> {

  List<QuestionnaireItem> findByElderlyIdOrderByPositionAsc(UUID elderlyId);

  boolean existsByElderlyId(UUID elderlyId);
}
