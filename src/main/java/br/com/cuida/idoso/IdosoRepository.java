package br.com.cuida.idoso;

import br.com.cuida.idoso.model.Idoso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface IdosoRepository extends JpaRepository<Idoso, UUID> {

  List<Idoso> findByTutorIdOrderByNameAsc(UUID tutorId);

  List<Idoso> findAllByOrderByNameAsc();

  Optional<Idoso> findByIdAndTutorId(UUID id, UUID tutorId);
}
