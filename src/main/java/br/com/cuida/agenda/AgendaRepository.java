package br.com.cuida.agenda;

import br.com.cuida.agenda.model.Agenda;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgendaRepository extends JpaRepository<Agenda, UUID> {

  Optional<Agenda> findByElderlyId(UUID elderlyId);

  List<Agenda> findByActiveTrue();
}
