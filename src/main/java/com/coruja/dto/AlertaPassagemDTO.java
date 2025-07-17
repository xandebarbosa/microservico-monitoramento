package com.coruja.dto;

import com.coruja.entities.AlertaPassagem;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class AlertaPassagemDTO {
    private Long id;
    private String concessionaria;

    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate data;

    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime hora;
    private String placa;
    private String praca;
    private String rodovia;
    private String km;
    private String sentido;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestampAlerta;

    private PlacaMonitoradaDTO placaMonitorada; // <-- DTO aninhado

    // O construtor que resolve o problema!
    public AlertaPassagemDTO(AlertaPassagem entity) {
        this.id = entity.getId();
        this.concessionaria = entity.getConcessionaria();
        this.data = entity.getData();
        this.hora = entity.getHora();
        this.placa = entity.getPlaca();
        this.praca = entity.getPraca();
        this.rodovia = entity.getRodovia();
        this.km = entity.getKm();
        this.sentido = entity.getSentido();
        this.timestampAlerta = entity.getTimestampAlerta();
        // Aqui, ao chamar o getPlacaMonitorada(), o JPA carrega os dados
        // enquanto a sessão ainda está aberta.
        if (entity.getPlacaMonitorada() != null) {
            this.placaMonitorada = new PlacaMonitoradaDTO(entity.getPlacaMonitorada());
        }
    }
}
