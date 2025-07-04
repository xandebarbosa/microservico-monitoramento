package com.coruja.dto;

import com.coruja.entities.PlacaMonitorada;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
public class PlacaMonitoradaDTO {
    private Long id;
    private String placa;
    private String marcaModelo;
    private String cor;
    private String motivo;
    private boolean statusAtivo;
    private String observacao;
    private String interessado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PlacaMonitoradaDTO() {}

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
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarcaModelo() {
        return marcaModelo;
    }

    public void setMarcaModelo(String marcaModelo) {
        this.marcaModelo = marcaModelo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isStatusAtivo() {
        return statusAtivo;
    }

    public void setStatusAtivo(boolean statusAtivo) {
        this.statusAtivo = statusAtivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getInteressado() {
        return interessado;
    }

    public void setInteressado(String interessado) {
        this.interessado = interessado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
