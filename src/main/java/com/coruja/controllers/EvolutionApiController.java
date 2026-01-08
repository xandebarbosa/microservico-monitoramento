package com.coruja.controllers;

import com.coruja.services.EvolutionWhatsappService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evolution")
public class EvolutionApiController {

    private final EvolutionWhatsappService evolutionService;

    public EvolutionApiController(EvolutionWhatsappService evolutionService) {
        this.evolutionService = evolutionService;
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok(evolutionService.isReady() ? "READY" : "NOT_READY");
    }

    @PostMapping("/reconnect")
    public ResponseEntity<String> reconnect() {
        evolutionService.reconnect();
        return ResponseEntity.ok("Reconexão iniciada");
    }

    @GetMapping("/test-connection")
    public ResponseEntity<String> testConnection() {
        // Endpoint simples para testar conexão
        return ResponseEntity.ok("Evolution API test endpoint");
    }
}