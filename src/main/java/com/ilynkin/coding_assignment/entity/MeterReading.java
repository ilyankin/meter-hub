package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "METER_READINGS", indexes = {
        @Index(name = "IDX_METER_READINGS_METER",
                columnList = "METER_ID"),
        @Index(name = "IDX_METER_READINGS_DATE",
                columnList = "READING_DATE")})
public class MeterReading {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "ID", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "METER_ID", nullable = false)
    private Meter meter;

    @NotNull
    @Column(name = "READING_DATE", nullable = false)
    private Instant readingDate;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    // Setter скрыт намеренно: Hibernate отслеживает этот конкретный экземпляр коллекции
    // и через него удаляет осиротевшие строки (orphanRemoval). Если обновлять через setter, то потеряется
    // отслеживание и будет возникать ошибка "collection ... was no longer referenced".
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "reading", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeterReadingValue> values = new ArrayList<>();

    public List<MeterReadingValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    // Заменяет значения, мутируя тот же экземпляр коллекции, чтобы orphanRemoval удалил прежние строки
    public void replaceValues(Collection<MeterReadingValue> newValues) {
        this.values.clear();
        this.values.addAll(newValues);
    }
}