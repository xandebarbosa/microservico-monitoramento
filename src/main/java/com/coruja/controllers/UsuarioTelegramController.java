package com.coruja.controllers;

import com.coruja.entities.UsuarioTelegram;
import com.coruja.repositories.UsuarioTelegramRepository;
import com.coruja.services.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios-telegram")
public class UsuarioTelegramController {

    private final TelegramService telegramService;
    private final UsuarioTelegramRepository usuarioTelegramRepository;

    public UsuarioTelegramController(TelegramService telegramService, UsuarioTelegramRepository usuarioTelegramRepository) {
        this.telegramService = telegramService;
        this.usuarioTelegramRepository = usuarioTelegramRepository;
    }

    /**
     * Endpoint para forçar o bot a ler novas mensagens e cadastrar quem falou com ele.
     * Retorna a lista de quem foi encontrado/atualizado.
     */
    // CORREÇÃO: Retorna Mono<ResponseEntity> para esperar o processamento
    @GetMapping("/sincronizar")
    public Mono<ResponseEntity<List<UsuarioTelegram>>> sincronizar() {
        return telegramService.processarNovosUsuarios()
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioTelegram>> listarRodos() {
        return ResponseEntity.ok(usuarioTelegramRepository.findAll());
    }
}
