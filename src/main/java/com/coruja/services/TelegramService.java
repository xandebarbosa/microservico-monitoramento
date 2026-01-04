package com.coruja.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final WebClient webClient;
    private static final String TELEGRAM_API_URL_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage";

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    public TelegramService(WebClient.Builder webClientBuilder) {
        // É boa prática configurar timeouts globais no Builder se necessário no futuro
        this.webClient = webClientBuilder.build();
    }

    public void sendMessage(String mensagem) {
        if (isConfiguracaoInvalida()) {
            return;
        }

        String url = String.format(TELEGRAM_API_URL_TEMPLATE, botToken.trim());

        Map<String, String> parametros = new HashMap<>();
        parametros.put("chat_id", chatId.trim()); // Trim garante que não vá com espaços vazios
        parametros.put("text", mensagem);
        parametros.put("parse_mode", "HTML");

        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(parametros)) // Usar BodyInserters
                .retrieve()
                .toBodilessEntity() // Usamos toBodilessEntity pois não precisamos do corpo da resposta
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("Mensagem enviada com sucesso ao Telegram.");
                    } else {
                        logger.warn("Falha ao enviar mensagem. Código de status: {}", response.getStatusCode());
                    }
                })
                .doOnError(error -> {
                    logger.error("Erro ao enviar mensagem para o Telegram: {}", error.getMessage());
                })
                .onErrorResume(e -> Mono.empty()) // Evita que o erro se propague, apenas loga
                .subscribe(); // Necessário para executar a chamada reativa
    }

    /**
     * Valida se as configurações necessárias estão presentes.
     */
    private boolean isConfiguracaoInvalida() {
        if (botToken == null || botToken.trim().isEmpty()) {
            logger.error("ERRO CRÍTICO: Token do Bot Telegram não configurado.");
            return true;
        }
        if (chatId == null || chatId.trim().isEmpty()) {
            logger.error("ERRO CRÍTICO: Chat ID do Telegram não configurado.");
            return true;
        }
        return false;
    }
}
