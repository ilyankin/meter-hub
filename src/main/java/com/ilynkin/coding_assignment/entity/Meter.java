package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "METERS", indexes = {@Index(name = "IDX_METERS_USER",
        columnList = "USER_ID")}, uniqueConstraints = {@UniqueConstraint(name = "CONSTRAINT_87",
        columnNames = {"SERIAL_NUMBER"})})
public class Meter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meters_seq")
    @SequenceGenerator(name = "meters_seq", sequenceName = "meters_seq", allocationSize = 50)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Size(max = 100)
    @NotNull
    @Column(name = "SERIAL_NUMBER", nullable = false, length = 100)
    private String serialNumber;

    @Size(max = 100)
    @Column(name = "INVENTORY_NUMBER", length = 100)
    private String inventoryNumber;

    @Column(name = "MANUFACTURE_YEAR")
    private Integer manufactureYear;

    @Column(name = "TRANSFORMATION_RATIO", precision = 10, scale = 2)
    private BigDecimal transformationRatio;

    @Column(name = "INSTALLATION_DATE")
    private LocalDate installationDate;

    @Size(max = 100)
    @Column(name = "SEAL_NUMBER", length = 100)
    private String sealNumber;

    @Size(max = 100)
    @Column(name = "ANTIMAGNETIC_SEAL_NUMBER", length = 100)
    private String antimagneticSealNumber;

    @Size(max = 500)
    @Column(name = "INSTALLATION_LOCATION", length = 500)
    private String installationLocation;

    @Column(name = "NOTES", columnDefinition = "text")
    private String notes;

    @Size(max = 100)
    @Column(name = "GIS_ID", length = 100)
    private String gisId;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;
}