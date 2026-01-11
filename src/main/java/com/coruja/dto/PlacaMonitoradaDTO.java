package com.coruja.dto;

import com.coruja.entities.PlacaMonitorada;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PlacaMonitoradaDTO {
    private Long id;
    private String placa;
    private String marcaModelo;
    private String cor;
    private String motivo;
    private boolean statusAtivo;
    private String observacao;
    private String interessado;
    private String telefone;
    private String telegramChatId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Construtor para converter a Entidade em DTO
    public PlacaMonitoradaDTO(PlacaMonitorada entity) {
        this.id = entity.getId();
        this.placa = entity.getPlaca();
        this.marcaModelo = entity.getMarcaModelo();
        this.cor = entity.getCor();
        this.motivo = entity.getMotivo();
        this.statusAtivo = entity.isStatusAtivo();
        this.observacao = entity.getObservacao();
        this.interessado = entity.getInteressado();
        this.telefone = entity.getTelefone();
        this.telegramChatId = entity.getTelegramChatId();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
