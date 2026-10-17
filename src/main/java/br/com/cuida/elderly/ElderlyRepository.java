package br.com.cuida.elderly;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ElderlyRepository extends JpaRepository<Elderly, UUID> {

  Page<Elderly> findByTutorId(UUID tutorId, Pageable pageable);

  Optional<Elderly> findByIdAndTutorId(UUID id, UUID tutorId);
}
