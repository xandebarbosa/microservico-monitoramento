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

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Setter
    @Getter
    @Column(nullable = false, unique = true, length = 7)
    private String placa;

    @Setter
    @Getter
    private String marcaModelo;
    @Setter
    @Getter
    private String cor;
    @Setter
    @Getter
    private String motivo;
    @Setter
    @Getter
    private boolean statusAtivo;

    @Setter
    @Getter
    @Column(length = 1000)
    private String observacao;

    @Setter
    @Getter
    private String interessado;

    @Getter
    @Setter
    @Column(name = "telefone")
    private String telefone;

    @Setter
    @Getter
    @CreatedDate // Marca este campo para receber a data e hora de criação
    @Column(name = "created_at", nullable = false, updatable = false) // Define a coluna no DB
    private LocalDateTime createdAt;

    @Setter
    @Getter
    @LastModifiedDate // Marca este campo para receber a data e hora da última atualização
    @Column(name = "updated_at", nullable = false) // Define a coluna no DB
    private LocalDateTime updatedAt;

    // Define o relacionamento reverso: Uma PlacaMonitorada pode ter muitos AlertasDePassagem.
    // cascade = CascadeType.ALL: Diz ao JPA: "Qualquer operação (salvar, deletar) que eu fizer
    //                                     nesta PlacaMonitorada, aplique também aos seus Alertas."
    // orphanRemoval = true: Garante que se um alerta for removido da lista, ele será deletado do banco.
    @Setter
    @Getter
    @OneToMany(mappedBy = "placaMonitorada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Exclui este campo do toString() para evitar loops infinitos
    private List<AlertaPassagem> alertas = new ArrayList<>();


}
