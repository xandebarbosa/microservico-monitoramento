package com.coruja.services;

import com.coruja.repositories.PlacaMonitoradaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RadarMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RadarMessageListener.class);
    private final PlacaMonitoradaRepository repository;
    private final TelegramService telegramService; // <-- Injeta o novo serviço
    // Formatador para a data no padrão dd/MM/yyyy
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // O Spring irá injetar os dois serviços automaticamente
    public RadarMessageListener(PlacaMonitoradaRepository repository, TelegramService telegramService) {
        this.repository = repository;
        this.telegramService = telegramService;
    }

    @RabbitListener(queues = "monitoramento_radares_queue")
    public void onRadarMessage(String message) {
        logger.debug("Mensagem de radar recebida para monitoramento: {}", message);
        try {
            // Assumimos o formato de 8 partes: CONCESS|DATA|HORA|PLACA|PRACA|RODOVIA|KM|SENTIDO
            String[] parts = message.split("\\|");
            if (parts.length < 8) {
                logger.warn("Mensagem de radar com formato inválido recebida: {}", message);
                return;
            }
            String placaDetectada = parts[3];

            // Verifica no banco se a placa recebida está na nossa lista de monitoramento ativo
            repository.findByPlacaAndStatusAtivo(placaDetectada, true)
                    .ifPresent(placaMonitorada -> {
                        // --- Extrai os dados da mensagem em tempo real ---
                        String concessionaria = parts[0];
                        String dataStr = parts[1];
                        String horaStr = parts[2];
                        String praca = parts[4];
                        String rodovia = parts[5];
                        String km = parts[6];
                        String sentido = parts[7];

                        // Formata a data para o padrão brasileiro
                        LocalDate data = LocalDate.parse(dataStr);
                        String dataFormatada = data.format(dateFormatter);

                        // =============================================================
                        // ##         MONTAGEM DA NOTIFICAÇÃO NO NOVO FORMATO         ##
                        // =============================================================
                        StringBuilder sb = new StringBuilder();

                        // Linha 1: Concessionária
                        sb.append("Concessionária ").append(concessionaria.toUpperCase()).append("\n");

                        // Linha 2: Data e Hora
                        sb.append("Data: ").append(dataFormatada).append(" - Horário: ").append(horaStr).append("\n");

                        // Linha 3: Placa, Marca e Cor
                        sb.append("Placa *").append(placaMonitorada.getPlaca()).append("*");
                        if (isValid(placaMonitorada.getMarcaModelo())) sb.append(" - ").append(placaMonitorada.getMarcaModelo());
                        if (isValid(placaMonitorada.getCor())) sb.append(", ").append(placaMonitorada.getCor());
                        sb.append("\n");

                        // Linha 4: Localização Completa
                        List<String> localizacaoParts = new ArrayList<>();
                        if (isValid(rodovia)) localizacaoParts.add(rodovia);
                        if (isValid(km)) localizacaoParts.add("km " + km);
                        if (isValid(sentido)) localizacaoParts.add("Sentido: " + sentido);
                        if (isValid(praca)) localizacaoParts.add(praca);
                        sb.append(String.join(" - ", localizacaoParts)).append("\n");

                        // Linha 5: Motivo
                        sb.append("Motivo: ").append(placaMonitorada.getMotivo()).append("\n");

                        // Linha 6: Interessado
                        sb.append("Interessado: ").append(placaMonitorada.getInteressado());

                        String notificacao = sb.toString();

                        logger.warn("ALERTA: Placa monitorada {} detectada! Enviando notificação para o Telegram.", placaDetectada);
                        telegramService.sendMessage(notificacao);
                    });
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de radar para monitoramento: {}", message, e);
        }
    }

    /**
     * Verifica se uma string contém uma informação útil.
     */
    private boolean isValid(String value) {
        return value != null && !value.isBlank() && !"N/A".equalsIgnoreCase(value);
    }
}
