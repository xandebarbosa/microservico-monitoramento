package com.coruja.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EvolutionWhatsappService implements NotificacaoService {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionWhatsappService.class);
    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean inicializacaoEmAndamento = new AtomicBoolean(false);
    private final AtomicBoolean instanciaPronta = new AtomicBoolean(false);

    @Value("${evolution.api.url}") // http://evolution-api:8080
    private String apiUrl;

    @Value("${evolution.api.token}")
    private String apiToken;

    @Value("${evolution.api.instance:RadarBot}")
    private String instanceName;

    @Value("${evolution.max.retries:20}")
    private int maxRetries;

    @Value("${evolution.retry.delay:5}")
    private int retryDelaySeconds;

    @Value("${evolution.timeout.seconds:30}")
    private int timeoutSeconds;

    // Construtor sem WebClient - será configurado no @PostConstruct
    public EvolutionWhatsappService() {
    }

    @PostConstruct
    public void init() {
        logger.info("🌐 Configurando Evolution API com URL: {}", apiUrl);
        if (apiToken != null && apiToken.length() > 8) {
            logger.info("🔑 Token: {}...", apiToken.substring(0, 8));
        } else {
            logger.error("❌ Token da Evolution API não configurado!");
        }
        logger.info("🤖 Instância: {}", instanceName);

        // Configurar WebClient CORRETAMENTE com a URL base
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("apikey", apiToken)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();

        logger.info("✅ WebClient configurado para URL base: {}", apiUrl);

        // Aguardar 5 segundos antes de iniciar para garantir que a Evolution API esteja pronta
        Mono.delay(Duration.ofSeconds(5))
                .subscribe(v -> {
                    logger.info("⏰ Iniciando verificação da instância após delay...");
                    inicializarInstancia();
                });
    }

    public void inicializarInstancia() {
        if (inicializacaoEmAndamento.get()) {
            logger.warn("Inicialização já em andamento, ignorando chamada duplicada.");
            return;
        }

        inicializacaoEmAndamento.set(true);
        logger.info("🚀 Iniciando serviço de WhatsApp. Verificando instância: {}", instanceName);

        verificarEConfigurarInstancia()
                .doOnSuccess(success -> {
                    instanciaPronta.set(true);
                    logger.info("✅ Instância do WhatsApp configurada e pronta.");
                    inicializacaoEmAndamento.set(false);
                })
                .doOnError(error -> {
                    logger.error("❌ Falha ao configurar instância do WhatsApp: {}", error.getMessage());
                    inicializacaoEmAndamento.set(false);
                    // Tentar novamente após 60 segundos
                    Mono.delay(Duration.ofSeconds(60))
                            .subscribe(v -> {
                                logger.info("🔄 Tentando nova conexão após falha...");
                                inicializarInstancia();
                            });
                })
                .subscribe();
    }

    private Mono<Void> verificarEConfigurarInstancia() {
        logger.debug("Iniciando verificação e configuração da instância");

        return verificarInstanciaExistente()
                .flatMap(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        logger.info("✅ Instância '{}' encontrada, verificando estado...", instanceName);
                        return verificarEstadoEConectar();
                    } else {
                        logger.info("📝 Instância '{}' não encontrada, criando nova...", instanceName);
                        return criarInstancia()
                                .then(Mono.delay(Duration.ofSeconds(2)))
                                .then(verificarEstadoEConectar());
                    }
                })
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(retryDelaySeconds))
                        .filter(this::isRecoverableError)
                        .doBeforeRetry(retrySignal ->
                                logger.warn("🔄 Tentativa {}/{} para configurar instância. Erro: {}",
                                        retrySignal.totalRetries() + 1,
                                        maxRetries,
                                        retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("❌ Excedido número máximo de tentativas ({}) para configurar instância", maxRetries);
                            return new RuntimeException("Excedido número máximo de tentativas para configurar instância");
                        })
                )
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        logger.error("⏰ Timeout após {} segundos tentando configurar instância", timeoutSeconds);
                    } else {
                        logger.error("💥 Erro fatal ao configurar instância: {}", e.getMessage());
                    }
                });
    }

    private Mono<Boolean> verificarInstanciaExistente() {
        String url = "/instance/fetchInstances";

        logger.debug("Verificando se a instância existe: GET {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        logger.debug("Resposta bruta da Evolution API ({} chars): {}",
                                responseBody.length(),
                                responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

                        JsonNode rootNode = objectMapper.readTree(responseBody);

                        // Verificar se a resposta é um array
                        if (rootNode.isArray()) {
                            ArrayNode instancesArray = (ArrayNode) rootNode;
                            logger.info("📡 API retornou array com {} instâncias", instancesArray.size());

                            for (JsonNode instanceNode : instancesArray) {
                                if (instanceNode.isTextual()) {
                                    String instanceNameFromArray = instanceNode.asText();
                                    if (instanceName.equals(instanceNameFromArray)) {
                                        logger.info("✅ Instância '{}' encontrada no array", instanceName);
                                        return Mono.just(true);
                                    }
                                } else if (instanceNode.isObject()) {
                                    String nomeInstancia = instanceNode.has("name") ?
                                            instanceNode.get("name").asText() :
                                            (instanceNode.has("instanceName") ?
                                                    instanceNode.get("instanceName").asText() : null);

                                    if (instanceName.equals(nomeInstancia)) {
                                        logger.info("✅ Instância '{}' encontrada no array de objetos", instanceName);
                                        return Mono.just(true);
                                    }
                                }
                            }
                            logger.info("❌ Instância '{}' não encontrada no array", instanceName);
                            return Mono.just(false);
                        }
                        // Verificar se a resposta é um objeto com propriedade "instances"
                        else if (rootNode.isObject()) {
                            JsonNode instancesNode = rootNode.get("instances");

                            if (instancesNode != null && instancesNode.isArray()) {
                                ArrayNode instancesArray = (ArrayNode) instancesNode;
                                logger.info("📡 API retornou objeto com {} instâncias", instancesArray.size());

                                for (JsonNode instanceNode : instancesArray) {
                                    if (instanceNode.isTextual()) {
                                        String instanceNameFromArray = instanceNode.asText();
                                        if (instanceName.equals(instanceNameFromArray)) {
                                            logger.info("✅ Instância '{}' encontrada em objeto.instances", instanceName);
                                            return Mono.just(true);
                                        }
                                    } else if (instanceNode.isObject()) {
                                        String nomeInstancia = null;

                                        if (instanceNode.has("name")) {
                                            nomeInstancia = instanceNode.get("name").asText();
                                        } else if (instanceNode.has("instanceName")) {
                                            nomeInstancia = instanceNode.get("instanceName").asText();
                                        } else if (instanceNode.has("instance")) {
                                            JsonNode instanceData = instanceNode.get("instance");
                                            if (instanceData.has("instanceName")) {
                                                nomeInstancia = instanceData.get("instanceName").asText();
                                            }
                                        }

                                        if (instanceName.equals(nomeInstancia)) {
                                            logger.info("✅ Instância '{}' encontrada em objeto.instances", instanceName);
                                            return Mono.just(true);
                                        }
                                    }
                                }
                            }
                            // Verificar se é um objeto com a instância como chave
                            else if (rootNode.has(instanceName)) {
                                logger.info("✅ Instância '{}' encontrada como chave do objeto", instanceName);
                                return Mono.just(true);
                            }
                        }

                        logger.info("❌ Instância '{}' não encontrada na resposta. Resposta: {}",
                                instanceName, rootNode.toPrettyString());
                        return Mono.just(false);

                    } catch (JsonProcessingException e) {
                        logger.error("Erro ao processar JSON da resposta: {}", e.getMessage());
                        return Mono.error(new RuntimeException("Erro ao processar resposta da API: " + e.getMessage()));
                    }
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        logger.info("Endpoint não encontrado (404) ao verificar instância");
                        return Mono.just(false);
                    }
                    logger.warn("Erro HTTP {} ao verificar instância: {}", e.getStatusCode(), e.getMessage());
                    return Mono.error(e);
                })
                .onErrorResume(e -> {
                    logger.warn("Erro de conexão ao verificar instância: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Não foi possível conectar na Evolution API: " + e.getMessage()));
                })
                .doOnError(e -> logger.error("❌ Falha na verificação da instância: {}", e.getMessage()))
                .doOnSuccess(existe -> logger.debug("Resultado da verificação: {}", existe));
    }

    private Mono<Void> verificarEstadoEConectar() {
        String url = "/instance/connectionState/" + instanceName;

        logger.debug("Verificando estado da instância: GET {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        logger.debug("Resposta do estado da instância: {}", responseBody);

                        JsonNode rootNode = objectMapper.readTree(responseBody);

                        String state = null;

                        if (rootNode.isObject()) {
                            if (rootNode.has("state")) {
                                state = rootNode.get("state").asText();
                            } else if (rootNode.has("instance")) {
                                JsonNode instanceNode = rootNode.get("instance");
                                if (instanceNode.has("state")) {
                                    state = instanceNode.get("state").asText();
                                }
                            } else if (rootNode.has("connection")) {
                                JsonNode connectionNode = rootNode.get("connection");
                                if (connectionNode.has("state")) {
                                    state = connectionNode.get("state").asText();
                                }
                            }
                        }

                        if (state != null) {
                            logger.info("📱 Estado da instância '{}': {}", instanceName, state);

                            if ("open".equalsIgnoreCase(state) || "connected".equalsIgnoreCase(state)) {
                                logger.info("✅ Instância já está conectada e pronta");
                                return Mono.empty();
                            } else if ("close".equalsIgnoreCase(state) || "disconnected".equalsIgnoreCase(state)) {
                                logger.info("🔗 Instância desconectada, solicitando conexão...");
                                return conectarInstancia().then(Mono.delay(Duration.ofSeconds(2)).then());
                            } else if ("connecting".equalsIgnoreCase(state)) {
                                logger.info("⏳ Instância está conectando, aguardando...");
                                return Mono.delay(Duration.ofSeconds(5)).then();
                            }
                        }

                        logger.warn("Estado da instância não reconhecido ou não encontrado, tentando conectar...");
                        return conectarInstancia().then();

                    } catch (JsonProcessingException e) {
                        logger.error("Erro ao processar JSON do estado: {}", e.getMessage());
                        return Mono.error(new RuntimeException("Erro ao processar estado da instância: " + e.getMessage()));
                    }
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    logger.warn("Instância não encontrada ao verificar estado");
                    return Mono.error(e);
                });
    }

    private Mono<Void> criarInstancia() {
        String url = "/instance/create";

        Map<String, Object> body = new HashMap<>();
        body.put("instanceName", instanceName);
        body.put("token", UUID.randomUUID().toString());
        body.put("qrcode", true);
        body.put("integration", "WHATSAPP-BAILEYS");

        logger.debug("Criando instância: POST {} com body: {}", url, body);

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response ->
                        logger.info("✅ Instância '{}' criada com sucesso! Resposta: {}", instanceName, response))
                .doOnError(e ->
                        logger.error("❌ Erro ao criar instância: {}", e.getMessage()))
                .then();
    }

    private Mono<Void> conectarInstancia() {
        String url = "/instance/connect/" + instanceName;

        logger.debug("Conectando instância: GET {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response ->
                        logger.info("📲 QR Code solicitado para a instância '{}'. " +
                                "Acesse o Evolution Manager em http://localhost:8091 para escanear.", instanceName))
                .doOnError(e ->
                        logger.error("Erro ao conectar instância: {}", e.getMessage()))
                .then();
    }

    @Override
    public void enviarMensagem(String mensagem, String numeroTelefone) {
        if (!instanciaPronta.get()) {
            logger.warn("Instância do WhatsApp não está pronta. Ignorando envio para: {}", numeroTelefone);
            return;
        }

        if (numeroTelefone == null || numeroTelefone.isBlank()) {
            logger.warn("Número de telefone inválido para envio de WhatsApp");
            return;
        }

        String numeroLimpo = normalizarNumeroDestino(numeroTelefone);
        String url = "/message/sendText/" + instanceName;

        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroLimpo);
        body.put("text", mensagem);
        body.put("delay", 1200);

        logger.debug("Enviando mensagem WhatsApp: POST {} para {}", url, numeroLimpo);

        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response ->
                        logger.info("✅ WhatsApp enviado para {}", numeroLimpo))
                .doOnError(e ->
                        logger.error("❌ Erro ao enviar WhatsApp para {}: {}",
                                numeroLimpo, e.getMessage()))
                .onErrorResume(e -> {
                    logger.warn("Falha ao enviar WhatsApp, marcando instância como não pronta");
                    instanciaPronta.set(false);
                    return Mono.empty();
                })
                .subscribe();
    }

    private boolean isRecoverableError(Throwable throwable) {
        // Erros de conexão são recuperáveis
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException e = (WebClientResponseException) throwable;
            return e.getStatusCode().is5xxServerError() ||
                    e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE ||
                    e.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT ||
                    e.getStatusCode() == HttpStatus.NOT_FOUND;
        }

        String message = throwable.getMessage();
        if (message != null) {
            return message.contains("Connection refused") ||
                    message.contains("connect timed out") ||
                    message.contains("Connection reset") ||
                    message.contains("IoException") ||
                    message.contains("PrematureCloseException") ||
                    message.contains("0:0:0:0:0:0:0:1") || // IPv6 localhost
                    message.contains("localhost");
        }

        return true;
    }

    private String normalizarNumeroDestino(String telefone) {
        String numero = telefone.replaceAll("\\D", "");
        if (numero.length() <= 11) {
            return "55" + numero;
        }
        return numero;
    }

    public boolean isReady() {
        return instanciaPronta.get();
    }

    public void reconnect() {
        if (inicializacaoEmAndamento.compareAndSet(false, true)) {
            logger.info("Forçando reconexão da instância WhatsApp...");
            verificarEConfigurarInstancia()
                    .doOnSuccess(success -> {
                        instanciaPronta.set(true);
                        logger.info("✅ Reconexão bem-sucedida");
                        inicializacaoEmAndamento.set(false);
                    })
                    .doOnError(error -> {
                        logger.error("❌ Falha na reconexão: {}", error.getMessage());
                        inicializacaoEmAndamento.set(false);
                    })
                    .subscribe();
        }
    }
}