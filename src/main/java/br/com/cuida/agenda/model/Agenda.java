package br.com.cuida.agenda.model;

import br.com.cuida.infra.persistencia.EntidadeAuditavel;
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
public class Agenda extends EntidadeAuditavel {

  public static final int DEFAULT_MAX_ATTEMPTS = 3;
  public static final int DEFAULT_RETRY_INTERVAL_MINUTES = 20;
  public static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

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
  protected Agenda() {}

  public Agenda(UUID elderlyId, LocalTime localTime, Set<DayOfWeek> daysOfWeek, boolean active) {
    super(UUID.randomUUID());
    this.elderlyId = Objects.requireNonNull(elderlyId, "elderlyId");
    update(localTime, daysOfWeek, active, DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_INTERVAL_MINUTES);
  }

  /** Agenda criada junto com o idoso: todos os dias às 9h, mas desligada até o tutor ativar. */
  public static Agenda initialFor(UUID elderlyId) {
    return new Agenda(elderlyId, DEFAULT_TIME, EnumSet.allOf(DayOfWeek.class), false);
  }

  public void update(
      LocalTime localTime,
      Set<DayOfWeek> daysOfWeek,
      boolean active,
      int maxAttempts,
      int retryIntervalMinutes) {
    this.localTime = Objects.requireNonNull(localTime, "localTime").withSecond(0).withNano(0);
    this.daysOfWeek =
        daysOfWeek.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(daysOfWeek);
    this.active = active;
    this.maxAttempts = maxAttempts;
    this.retryIntervalMinutes = retryIntervalMinutes;
  }

  public UUID getElderlyId() {
    return elderlyId;
  }

  public LocalTime getLocalTime() {
    return localTime;
  }

  public Set<DayOfWeek> getDaysOfWeek() {
    return Collections.unmodifiableSet(daysOfWeek);
  }

  public boolean isActive() {
    return active;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public int getRetryIntervalMinutes() {
    return retryIntervalMinutes;
  }

  public AgendaDTO toDTO() {
    return new AgendaDTO(
        elderlyId, localTime, daysOfWeek, active, maxAttempts, retryIntervalMinutes);
  }
}
