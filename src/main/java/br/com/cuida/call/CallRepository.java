package br.com.cuida.call;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CallRepository extends JpaRepository<Call, UUID> {

  Optional<Call> findByProviderCallId(String providerCallId);

  List<Call> findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscAttemptAsc(
      UUID elderlyId, LocalDate from, LocalDate to);
}
