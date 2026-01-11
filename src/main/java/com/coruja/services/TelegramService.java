package com.coruja.services;

import com.coruja.entities.UsuarioTelegram;
import com.coruja.repositories.UsuarioTelegramRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final WebClient webClient;
    private static final String TELEGRAM_API_URL_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage";

    private final UsuarioTelegramRepository usuarioTelegramRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String defaultChatId;

    public TelegramService(WebClient.Builder webClientBuilder, UsuarioTelegramRepository usuarioTelegramRepository) {
        // É boa prática configurar timeouts globais no Builder se necessário no futuro
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
        this.usuarioTelegramRepository = usuarioTelegramRepository;
    }

    /**
     * Envia para o Canal Geral (Monitoramento)
     */
    public void sendToGeneralChannel(String message) {
        enviarMensagem(message, defaultChatId);
    }

    public void enviarMensagem(String messagem, String chatId) {
        if (chatId == null || chatId.isBlank()) {
            logger.warn("Chat ID vazio. Tentando enviar para o canal padrão.");
            chatId = defaultChatId;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId.trim());
        body.put("text", messagem);
        body.put("parse_mode", "HTML");

        webClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
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
     * Método para buscar as últimas mensagens recebidas pelo Bot.
     * Útil para descobrir o Chat ID de novos usuários.
     */
    public Mono<List<String>> buscarMensagensRecentes() {
        return webClient.get()
                .uri("/bot" + botToken + "/getUpdates")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    List<String> interacoes = new ArrayList<>();
                    if (json.has("result")) {
                        for (JsonNode update : json.get("result")) {
                            if (update.has("message")) {
                                JsonNode msg = update.get("message");
                                String nome = msg.path("from").path("first_name").asText();
                                String sobrenome = msg.path("from").path("last_name").asText("");
                                String chatId = msg.path("chat").path("id").asText();
                                String texto = msg.path("text").asText();

                                interacoes.add(String.format("Nome: %s %s | ID: %s | Msg: %s", nome, sobrenome, chatId, texto));
                            }
                        }
                    }
                    return interacoes;
                });
    }

//    public void sendMessage(String mensagem) {
//        if (isConfiguracaoInvalida()) {
//            return;
//        }
//
//        String url = String.format(TELEGRAM_API_URL_TEMPLATE, botToken.trim());
//
//        Map<String, String> parametros = new HashMap<>();
//        parametros.put("chat_id", chatId.trim()); // Trim garante que não vá com espaços vazios
//        parametros.put("text", mensagem);
//        parametros.put("parse_mode", "HTML");
//
//        webClient.post()
//                .uri(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromValue(parametros)) // Usar BodyInserters
//                .retrieve()
//                .toBodilessEntity() // Usamos toBodilessEntity pois não precisamos do corpo da resposta
//                .doOnSuccess(response -> {
//                    if (response.getStatusCode().is2xxSuccessful()) {
//                        logger.info("Mensagem enviada com sucesso ao Telegram.");
//                    } else {
//                        logger.warn("Falha ao enviar mensagem. Código de status: {}", response.getStatusCode());
//                    }
//                })
//                .doOnError(error -> {
//                    logger.error("Erro ao enviar mensagem para o Telegram: {}", error.getMessage());
//                })
//                .onErrorResume(e -> Mono.empty()) // Evita que o erro se propague, apenas loga
//                .subscribe(); // Necessário para executar a chamada reativa
//    }

    /**
     * Valida se as configurações necessárias estão presentes.
     */
    private boolean isConfiguracaoInvalida() {
        if (botToken == null || botToken.trim().isEmpty()) {
            logger.error("ERRO CRÍTICO: Token do Bot Telegram não configurado.");
            return true;
        }
        if (defaultChatId == null || defaultChatId.trim().isEmpty()) {
            logger.error("ERRO CRÍTICO: Chat ID do Telegram não configurado.");
            return true;
        }
        return false;
    }

    // Novo método para processar e salvar usuários
    //@Scheduled(fixedRate = 60000)
    public Mono<List<UsuarioTelegram>> processarNovosUsuarios() {
        return webClient.get()
                .uri("/bot" + botToken + "/getUpdates")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    List<UsuarioTelegram> novosUsuariosAtualizados = new ArrayList<>();
                    if (json.has("result")) {
                        for (JsonNode item : json.get("result")) {
                            if (item.has("message")) {
                                JsonNode from = item.get("message").get("from");
                                String telegramId = String.valueOf(from.get("id").asLong());
                                String primeiroNome = from.path("first_name").asText();
                                String sobrenome = from.path("last_name").asText(null);
                                String username = from.path("username").asText(null);

                                // Lógica de "Upsert" (Salvar ou Atualizar)
                                UsuarioTelegram usuario = usuarioTelegramRepository.findByTelegramId(telegramId)
                                        .orElse(new UsuarioTelegram(telegramId, username, primeiroNome, sobrenome));

                                // Atualiza dados caso tenham mudado
                                usuario.setUltimoAcesso(LocalDateTime.now());
                                // usuario.setUsername(username); // opcional atualizar outros campos

                                usuarioTelegramRepository.save(usuario);
                                novosUsuariosAtualizados.add(usuario);

                            }
                        }
                    }
                    return novosUsuariosAtualizados;
                });
    }
}
