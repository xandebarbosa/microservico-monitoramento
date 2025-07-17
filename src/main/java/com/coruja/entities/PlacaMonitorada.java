package com.coruja.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "placas_monitoradas")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EntityListeners(AuditingEntityListener.class) // <-- NOVO: Habilita os "ouvintes" de auditoria para esta entidade
public class PlacaMonitorada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false, unique = true, length = 7)
    private String placa;

    private String marcaModelo;
    private String cor;
    private String motivo;
    private boolean statusAtivo;

    @Column(length = 1000)
    private String observacao;

    private String interessado;

    @CreatedDate // Marca este campo para receber a data e hora de criação
    @Column(name = "created_at", nullable = false, updatable = false) // Define a coluna no DB
    private LocalDateTime createdAt;

    @LastModifiedDate // Marca este campo para receber a data e hora da última atualização
    @Column(name = "updated_at", nullable = false) // Define a coluna no DB
    private LocalDateTime updatedAt;

    // Define o relacionamento reverso: Uma PlacaMonitorada pode ter muitos AlertasDePassagem.
    // cascade = CascadeType.ALL: Diz ao JPA: "Qualquer operação (salvar, deletar) que eu fizer
    //                                     nesta PlacaMonitorada, aplique também aos seus Alertas."
    // orphanRemoval = true: Garante que se um alerta for removido da lista, ele será deletado do banco.
    @OneToMany(mappedBy = "placaMonitorada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Exclui este campo do toString() para evitar loops infinitos
    private List<AlertaPassagem> alertas = new ArrayList<>();


    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
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

    public List<AlertaPassagem> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<AlertaPassagem> alertas) {
        this.alertas = alertas;
    }
}
