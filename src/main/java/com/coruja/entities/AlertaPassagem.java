package com.coruja.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table( name = "alertas_passagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AlertaPassagem {

    //Esta entidade irá armazenar cada passagem de um veículo monitorado.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados da passagem em tempo real
    private String concessionaria;
    private LocalDate data;
    private LocalTime hora;
    private String placa;
    private String praca;
    private String rodovia;
    private String km;
    private String sentido;

    // Relacionamento com a placa que gerou o alerta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placa_monitorada_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PlacaMonitorada placaMonitorada;

    @CreatedDate
    @Column(name = "timestamp_alerta", nullable = false, updatable = false)
    private LocalDateTime timestampAlerta;
}
