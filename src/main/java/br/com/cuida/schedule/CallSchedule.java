package br.com.cuida.schedule;

import br.com.cuida.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Agenda diária de ligações de uma pessoa idosa (uma por idoso). */
@Entity
@Table(name = "call_schedules")
class CallSchedule extends AuditableEntity {

  static final int DEFAULT_MAX_ATTEMPTS = 3;
  static final int DEFAULT_RETRY_INTERVAL_MINUTES = 20;

  @Column(name = "elderly_id", nullable = false, updatable = false, unique = true)
  private UUID elderlyId;

  /** Horário no fuso da pessoa idosa. */
  @Column(name = "local_time", nullable = false)
  private LocalTime localTime;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "days_of_week", nullable = false)
  private Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

  @Column(nullable = false)
  private boolean active;

  @Column(name = "max_attempts", nullable = false)
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  @Column(name = "retry_interval_minutes", nullable = false)
  private int retryIntervalMinutes = DEFAULT_RETRY_INTERVAL_MINUTES;

  /** Exigido pelo JPA. */
  protected CallSchedule() {}

  CallSchedule(UUID elderlyId, LocalTime localTime, Set<DayOfWeek> daysOfWeek, boolean active) {
    super(UUID.randomUUID());
    this.elderlyId = Objects.requireNonNull(elderlyId, "elderlyId");
    this.localTime = Objects.requireNonNull(localTime, "localTime");
    this.daysOfWeek = EnumSet.copyOf(daysOfWeek);
    this.active = active;
  }

  UUID getElderlyId() {
    return elderlyId;
  }

  LocalTime getLocalTime() {
    return localTime;
  }

  Set<DayOfWeek> getDaysOfWeek() {
    return Collections.unmodifiableSet(daysOfWeek);
  }

  boolean isActive() {
    return active;
  }

  int getMaxAttempts() {
    return maxAttempts;
  }

  int getRetryIntervalMinutes() {
    return retryIntervalMinutes;
  }
}
