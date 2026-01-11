package com.coruja.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "usuarios_telegram")
public class UsuarioTelegram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", unique = true, nullable = false)
    private String telegramId;

    private String username;

    @Column(name = "primeiro_nome")
    private String primeiroNome;

    @Column(name = "sobrenome")
    private String sobrenome;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    public UsuarioTelegram(String telegramId, String username, String primeiroNome, String sobrenome) {
        this.telegramId = telegramId;
        this.username = username;
        this.primeiroNome = primeiroNome;
        this.sobrenome = sobrenome;
    }

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        ultimoAcesso = LocalDateTime.now();
    }


}
