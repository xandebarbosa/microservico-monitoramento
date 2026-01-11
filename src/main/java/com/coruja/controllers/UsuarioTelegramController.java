package com.coruja.controllers;

import com.coruja.entities.UsuarioTelegram;
import com.coruja.repositories.UsuarioTelegramRepository;
import com.coruja.services.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/sincronizar")
    public ResponseEntity<String> sincronizar() {
        // Dispara a busca na API do Telegram e salva no banco
        telegramService.processarNovosUsuarios().subscribe();
        return ResponseEntity.ok("Sincronizaçao iniciada em background.");
    }

    @GetMapping
    public ResponseEntity<List<UsuarioTelegram>> listarRodos() {
        return ResponseEntity.ok(usuarioTelegramRepository.findAll());
    }
}
