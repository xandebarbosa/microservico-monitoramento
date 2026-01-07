package com.coruja.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
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

    /**
     * Executa ao iniciar o serviço: Garante que a instância exista e esteja pronta para conectar.
     * * Tenta conectar insistentemente até a API estar online.
     */
    @PostConstruct
    public void inicializarInstancia() {
        logger.info("🚀 Iniciando serviço de WhatsApp. Aguardando Evolution API...");
        verificarEstadoConexao()
                .subscribe(
                        success -> logger.info("✅ Processo de inicialização do WhatsApp concluído."),
                        error -> logger.error("❌ Falha crítica ao inicializar WhatsApp após várias tentativas: {}", error.getMessage())
                );
    }

    /**
     * CORREÇÃO APLICADA AQUI:
     * Usamos flatMap para garantir que o fluxo retorne Mono<Void> TANTO no sucesso QUANTO no erro.
     */
    public Mono<Void> verificarEstadoConexao() {
        String url = String.format("%s/instance/connectionState/%s", apiUrl, instanceName);

        return webClient.get()
                .uri(url)
                .header("apikey", apiToken)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Map<String, Object> instanceData = (Map<String, Object>) response.get("instance");
                    String state = (String) instanceData.get("state");
                    logger.info("📡 Instância '{}' encontrada. Estado: {}", instanceName, state);

                    if ("close".equalsIgnoreCase(state)) {
                        return conectarInstancia().then();
                    }
                    return Mono.empty();
                })
                // Se der erro (404 ou Connection Refused), tenta criar
                .onErrorResume(e -> {
                    logger.warn("⚠️ Instância não encontrada ou API indisponível. Tentando criar/conectar...");
                    return criarInstancia();
                })
                // === A MÁGICA DO RETRY ===
                // Tenta 10 vezes, esperando um pouco mais a cada tentativa (Backoff)
                // Isso dá tempo para a Evolution API subir se ela estiver lenta.
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(5))
                        .doBeforeRetry(signal -> logger.info("🔄 Tentativa {} de conectar na Evolution API...", signal.totalRetries() + 1))
                );
    }

    private Mono<Void> criarInstancia() {
        String url = String.format("%s/instance/create", apiUrl);

        Map<String, Object> body = new HashMap<>();
        body.put("instanceName", instanceName);
        body.put("token", java.util.UUID.randomUUID().toString());
        body.put("qrcode", true);
        body.put("integration", "WHATSAPP-BAILEYS");

        return webClient.post()
                .uri(url)
                .header("apikey", apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(resp -> {
                    logger.info("Instância '{}' CRIADA com sucesso! Iniciando conexão...", instanceName);
                    return conectarInstancia();
                })
                .then();
    }

    public Mono<String> conectarInstancia() {
        String connectUrl = String.format("%s/instance/connect/%s", apiUrl, instanceName);

        return webClient.get()
                .uri(connectUrl)
                .header("apikey", apiToken)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(r -> logger.info("Solicitação de conexão enviada! Verifique o QR Code."))
                .doOnError(e -> logger.error("Erro ao solicitar conexão: {}", e.getMessage()));
    }

    @Override
    public void enviarMensagem(String mensagem, String numeroTelefone) {
        if (numeroTelefone == null || numeroTelefone.isBlank()) return;

        String numeroLimpo = normalizarNumeroDestino(numeroTelefone);
        String urlCompleta = String.format("%s/message/sendText/%s", apiUrl, instanceName);

        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroLimpo);
        body.put("text", mensagem);
        body.put("delay", 1200);

        webClient.post()
                .uri(urlCompleta)
                .header("apikey", apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> logger.info("WhatsApp enviado para {}", numeroLimpo))
                .doOnError(e -> logger.error("Erro ao enviar WhatsApp: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    private String normalizarNumeroDestino(String telefone) {
        String numero = telefone.replaceAll("\\D", "");
        return numero.length() <= 11 ? "55" + numero : numero;
    }
}
