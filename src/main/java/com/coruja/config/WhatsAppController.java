package com.coruja.config;

import com.coruja.services.EvolutionWhatsappService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final EvolutionWhatsappService whatsAppService;

    public WhatsAppController(EvolutionWhatsappService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/reconnect")
    public ResponseEntity<String> reconnect() {
        whatsAppService.reconnect();
        return ResponseEntity.ok("Reconexão do WhatsApp iniciada");
    }

    @PostMapping("/status")
    public ResponseEntity<String> status() {
        boolean isReady = whatsAppService.isReady();
        return ResponseEntity.ok(isReady ? "CONNECTED" : "DISCONNECTED");
    }
}