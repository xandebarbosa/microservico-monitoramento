package com.coruja.health;

import com.coruja.services.EvolutionWhatsappService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppHealthIndicator implements HealthIndicator {

    private final EvolutionWhatsappService whatsAppService;

    public WhatsAppHealthIndicator(EvolutionWhatsappService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @Override
    public Health health() {
        if (whatsAppService.isReady()) {
            return Health.up()
                    .withDetail("service", "whatsapp")
                    .withDetail("status", "connected")
                    .withDetail("instance", "RadarBot")
                    .build();
        } else {
            return Health.down()
                    .withDetail("service", "whatsapp")
                    .withDetail("status", "disconnected")
                    .withDetail("instance", "RadarBot")
                    .withDetail("message", "WhatsApp service is connecting or failed")
                    .build();
        }
    }
}