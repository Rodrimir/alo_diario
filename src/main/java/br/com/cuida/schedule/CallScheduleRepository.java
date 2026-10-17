package br.com.cuida.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CallScheduleRepository extends JpaRepository<CallSchedule, UUID> {

  Optional<CallSchedule> findByElderlyId(UUID elderlyId);

  List<CallSchedule> findByActiveTrue();
}
