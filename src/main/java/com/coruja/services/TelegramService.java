package com.coruja.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final WebClient webClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    public TelegramService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void sendMessage(String text) {
        // Codifica o texto para ser seguro em uma URL e lida com caracteres especiais e quebras de linha
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

        // Monta a URL da API do Telegram
        String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                botToken, chatId, encodedText);

        // Envia a mensagem de forma assíncrona (não trava a aplicação)
        webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.info("Mensagem enviada com sucesso para o Telegram."))
                .doOnError(error -> logger.error("Falha ao enviar mensagem para o Telegram.", error))
                .subscribe(); // Inicia a chamada
    }
}
