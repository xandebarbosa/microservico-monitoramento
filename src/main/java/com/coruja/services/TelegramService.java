package com.coruja.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

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

        String url = String.format(TELEGRAM_API_URL, botToken.trim());

        Map<String, String> parametros = new HashMap<>();
        parametros.put("chat_id", chatId);
        parametros.put("text", mensagem);
        parametros.put("parse_mode", "HTML");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(parametros, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Mensagem enviada com sucesso ao Telegram.");
            } else {
                logger.warn("Falha ao enviar mensagem. Código de status: {}", response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            logger.error("Erro HTTP ao enviar mensagem para o Telegram: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            logger.error("Erro ao se comunicar com a API do Telegram: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao enviar mensagem ao Telegram: ", e);
        }
    }
}
