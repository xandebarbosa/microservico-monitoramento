package com.coruja.controllers;

import com.coruja.services.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private final TelegramService telegramService;

    public TelegramController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    // 1. Endpoint para descobrir IDs (Admin usa isso)
    // URL: GET http://localhost:8089/api/telegram/updates
    @GetMapping("/updates")
    public Mono<ResponseEntity<List<String>>> listarIdsRecentes() {
        return telegramService.buscarMensagensRecentes()
                .map(ResponseEntity::ok);
    }

    // 2. Endpoint para testar envio
    // URL: POST http://localhost:8089/api/telegram/teste?chatId=123456&msg=Ola
    public ResponseEntity<String> enviarTeste(@RequestParam String chatId, @RequestParam String msg) {
        telegramService.enviarMensagem(msg, chatId);
        return ResponseEntity.ok("Mensagem enviada para a fila de processamento.");
    }
}
