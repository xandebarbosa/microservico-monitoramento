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
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private final WebClient webClient;
    private static final String TELEGRAM_API_URL_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage";

    private final UsuarioTelegramRepository usuarioTelegramRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String defaultChatId; // ID do Grupo Geral

    // Controle do offset para não processar mensagens repetidas
    private final AtomicLong lastUpdateId = new AtomicLong(0);

    public TelegramService(WebClient.Builder webClientBuilder, UsuarioTelegramRepository usuarioTelegramRepository) {
        // É boa prática configurar timeouts globais no Builder se necessário no futuro
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
        this.usuarioTelegramRepository = usuarioTelegramRepository;
    }

    /**
     * Envia mensagem para o Canal Geral (Monitoramento)
     */
    public void sendToGeneralChannel(String message) {
        enviarMensagem(message, defaultChatId);
    }

    /**
     * Envia mensagem genérica para um Chat ID específico
     */
    public void enviarMensagem(String messagem, String chatId) {
        if (chatId == null || chatId.isBlank()) {
            logger.warn("Tentativa de enviar mensagem sem Chat ID. Redirecionando para canal padrão.");
            chatId = defaultChatId;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId.trim());
        body.put("text", messagem);
        body.put("parse_mode", "HTML");

        logger.info("Enviando mensagem para Telegram ID: {}", chatId);

        webClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity() // Usamos toBodilessEntity pois não precisamos do corpo da resposta
                .doOnSuccess(response -> logger.debug("Mensagem Telegram enviada. Status: {}", response.getStatusCode()))
                .doOnError(error -> logger.error("Erro ao enviar mensagem Telegram: {}", error.getMessage()))
                .onErrorResume(e -> Mono.empty()) // Evita que o erro se propague, apenas loga
                .subscribe(); // Necessário para executar a chamada reativa
    }

    /**
     * Busca atualizações (novas mensagens) para cadastrar usuários.
     * Usa o offset para pegar apenas mensagens novas desde a última verificação.
     */
//    public Mono<List<UsuarioTelegram>> processarNovosUsuarios() {
//        long offset = lastUpdateId.get() + 1;
//
//        return webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/bot" + botToken + "/getUpdates")
//                        .queryParam("offset", offset)
//                        .build())
//                .retrieve()
//                .bodyToMono(JsonNode.class)
//                .map(this::parseAndSaveUsers);
//    }
    // --- Processamento de Usuários (REFATORADO) ---

    public Mono<List<UsuarioTelegram>> processarNovosUsuarios() {
        // Offset 0 força o Telegram a mandar as mensagens não confirmadas (pendentes)
        // Se quisermos pular as lidas, usaríamos: lastUpdateId.get() + 1
        // Por segurança no teste, vamos pedir tudo que estiver pendente.
        long offsetRequest = lastUpdateId.get() + 1;

        logger.info("Solicitando updates ao Telegram. Offset: {}", offsetRequest);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot" + botToken + "/getUpdates")
                        .queryParam("offset", offsetRequest)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    // Log do JSON bruto para conferência
                    logger.info("JSON Recebido do Telegram: {}", json.toString());
                    return parseAndSaveUsers(json);
                });
    }

    private List<UsuarioTelegram> parseAndSaveUsers(JsonNode json) {
        List<UsuarioTelegram> usuariosProcessados = new ArrayList<>();

        // Verifica se o JSON é válido
        if (json == null || !json.has("result") || !json.get("result").isArray()) {
            return usuariosProcessados;
        }

        for (JsonNode update : json.get("result")) {
            // Atualiza o offset para não ler a mesma mensagem novamente
            long updateId = update.path("update_id").asLong();

            // Atualizamos o offset sempre para garantir que na próxima chamada não venha repetido
            if (updateId >= lastUpdateId.get()) {
                lastUpdateId.set(updateId);
            }

            // Verificamos se tem mensagem e se tem remetente ('from')
            if (update.has("message")) {
                JsonNode message = update.get("message");
                if (message.has("from")) {
                    JsonNode from = message.get("from");

                    try {
                        UsuarioTelegram usuario = salvarUsuario(from);
                        usuariosProcessados.add(usuario);
                    } catch (Exception e) {
                        logger.error("Erro ao salvar usuário do update {}: {}", updateId, e.getMessage());
                    }
                } else {
                    logger.warn("Mensagem sem campo 'from'. Ignorando.");
                }
            }
        }
        return usuariosProcessados;
    }

    private UsuarioTelegram salvarUsuario(JsonNode fromNode) {
        // Extração robusta usando .path() que evita NullPointerException
        long idLong = fromNode.path("id").asLong();
        String telegramId = String.valueOf(idLong);

        String primeiroNome = fromNode.path("first_name").asText("Desconhecido");
        String sobrenome = fromNode.path("last_name").asText("");
        String username = fromNode.path("username").asText("");

        logger.info("Tentando salvar -> ID: {}, Nome: {}, User: {}", telegramId, primeiroNome, username);

        if (idLong == 0) {
            throw new IllegalArgumentException("ID do Telegram veio zerado ou inválido.");
        }

        return usuarioTelegramRepository.findByTelegramId(telegramId)
                .map(existente -> {
                    existente.setPrimeiroNome(primeiroNome);
                    existente.setSobrenome(sobrenome);
                    existente.setUsername(username);
                    existente.setUltimoAcesso(LocalDateTime.now());
                    UsuarioTelegram salvo = usuarioTelegramRepository.save(existente);
                    logger.info("Usuário ATUALIZADO com sucesso: {}", salvo.getTelegramId());
                    return salvo;
                })
                .orElseGet(() -> {
                    UsuarioTelegram novo = new UsuarioTelegram();
                    novo.setTelegramId(telegramId);
                    novo.setPrimeiroNome(primeiroNome);
                    novo.setSobrenome(sobrenome);
                    novo.setUsername(username);
                    novo.setDataCadastro(LocalDateTime.now());
                    novo.setUltimoAcesso(LocalDateTime.now());
                    UsuarioTelegram salvo = usuarioTelegramRepository.save(novo);
                    logger.info("Usuário NOVO salvo com sucesso: {}", salvo.getTelegramId());
                    return salvo;
                });
    }

//    private UsuarioTelegram salvarOuAtualizarUsuario(JsonNode fromNode) {
//        // 1. Extração segura dos dados do JSON
//        String idNoTelegram = String.valueOf(fromNode.get("id").asLong()); // Mapeia 'id' -> telegramId
//        String primeiroNome = fromNode.path("first_name").asText();
//        String sobrenome = fromNode.path("last_name").asText("");
//        String username = fromNode.path("username").asText("");
//
//        // 2. Busca no banco ou cria novo
//        UsuarioTelegram usuario = usuarioTelegramRepository.findByTelegramId(idNoTelegram)
//                .orElse(new UsuarioTelegram(idNoTelegram, username, primeiroNome, sobrenome));
//
//        // 3. Atualiza dados cadastrais (caso a pessoa tenha mudado o nome/user)
//        usuario.setPrimeiroNome(primeiroNome);
//        usuario.setSobrenome(sobrenome);
//        usuario.setUsername(username);
//        usuario.setUltimoAcesso(LocalDateTime.now());
//
//        // 4. Salva e retorna
//        UsuarioTelegram salvo = usuarioTelegramRepository.save(usuario);
//        logger.info("Usuário Sincronizado: {} {} (Telegram ID: {})", primeiroNome, sobrenome, idNoTelegram);
//
//        return salvo;
//    }

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
}
