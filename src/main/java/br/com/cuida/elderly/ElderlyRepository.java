package br.com.cuida.elderly;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ElderlyRepository extends JpaRepository<Elderly, UUID> {

  List<Elderly> findByTutorIdOrderByNameAsc(UUID tutorId);

  List<Elderly> findAllByOrderByNameAsc();

  Optional<Elderly> findByIdAndTutorId(UUID id, UUID tutorId);
}
