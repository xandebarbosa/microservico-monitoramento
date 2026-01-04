package com.coruja.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class EvolutionWhatsappService implements NotificacaoService {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionWhatsappService.class);
    private final WebClient webClient;

    @Value("${evolution.api.url}") // http://evolution-api:8080
    private String apiUrl;

    @Value("${evolution.api.token}")  // O token definido no docker-compose
    private String apiToken;

    @Value("${evolution.api.instance}") // O nome da instância (ex: RadarBot)
    private String instanceName;

    public EvolutionWhatsappService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public void enviarMensagem(String mensagem, String numeroTelefone) {
        if (numeroTelefone == null || numeroTelefone.isBlank()) {
            logger.warn("Tentativa de envio de WhatsApp sem número de destino.");
            return;
        }

        // Limpeza básica do número (apenas números)
        String numeroLimpo = numeroTelefone.replaceAll("\\D", "");

        // Garante o código do país (55 para Brasil) se não houver
        if (numeroLimpo.length() <= 11) {
            numeroLimpo = "55" + numeroLimpo;
        }

        // Monta a URL: /message/sendText/{instance}
        String urlCompleta = String.format("%s/message/sendText/%s", apiUrl, instanceName);

        // Corpo JSON esperado pela Evolution API v2
        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroLimpo);
        body.put("text", mensagem);
        body.put("delay", 1200); // Delay simulado de digitação (Opicional)

        String finalNumeroLimpo = numeroLimpo;

        webClient.post()
                .uri(urlCompleta)
                .header("apiKey", apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> logger.info("WhatsApp enviado para {}", finalNumeroLimpo))
                .doOnError(e -> logger.error("Erro ao enviar WhatsApp para {}: {}", finalNumeroLimpo, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}
