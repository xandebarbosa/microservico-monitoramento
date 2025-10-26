package com.coruja.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final WebClient webClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    private static final String TELEGRAM_API_URL_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage";
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";
    private final RestTemplate restTemplate = new RestTemplate();

    public TelegramService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void sendMessage(String mensagem) {
        if (botToken == null || botToken.isEmpty()) {
            logger.error("Erro: O botToken do Telegram está vazio ou não foi configurado.");
            return;
        }

        if (chatId == null || chatId.isEmpty()) {
            logger.error("Erro: O chatId do Telegram está vazio ou não foi configurado.");
            return;
        }

        String url = String.format(TELEGRAM_API_URL_TEMPLATE, botToken.trim());

        Map<String, String> parametros = new HashMap<>();
        parametros.put("chat_id", chatId);
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
}
