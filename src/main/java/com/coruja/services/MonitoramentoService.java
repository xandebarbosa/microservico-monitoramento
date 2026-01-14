package com.coruja.services;

import com.coruja.dto.AlertaPassagemDTO;
import com.coruja.dto.PlacaMonitoradaDTO;
import com.coruja.entities.AlertaPassagem;
import com.coruja.entities.PlacaMonitorada;
import com.coruja.repositories.AlertaPassagemRepository;
import com.coruja.repositories.PlacaMonitoradaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MonitoramentoService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoramentoService.class);

    private final PlacaMonitoradaRepository placaRepository;
    private final AlertaPassagemRepository alertaRepository;

    // Serviço de Notificação
    private final TelegramService telegramService;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    public MonitoramentoService(PlacaMonitoradaRepository placaRepository,
                                TelegramService telegramService,
                                AlertaPassagemRepository alertaPassagemRepository,
                                RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper) {
        this.placaRepository = placaRepository;
        this.telegramService = telegramService;
        this.alertaRepository = alertaPassagemRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Ouve a fila do RabbitMQ, verifica se a placa é de interesse e, se for,
     * salva o alerta e envia a notificação para o Telegram.
     * Notifica o Grupo Geral
     * Notifica Usuário (se cadastrado)
     */
    @RabbitListener(queues = "monitoramento_radares_queue")
    public void onRadarMessage(String message) {
        logger.debug("Mensagem de radar recebida para monitoramento: {}", message);

        try {
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.warn("Mensagem de radar com formato muito curto: {}", message);
                return;
            }
            String placaDetectada = parts[3];

            // Busca a placa APENAS se estiver ativa
            placaRepository.findByPlacaAndStatusAtivo(placaDetectada, true)
                    .ifPresent(placaMonitorada -> processarAlerta(message, placaMonitorada));

        } catch (Exception e) {
            logger.error("Erro inesperado ao processar mensagem do RabbitMQ: {}", message, e);
        }
    }

    private void processarAlerta(String message, PlacaMonitorada placaMonitorada) {
        // 1. Salvar Alerta
        AlertaPassagem alertaSalvo = alertaRepository.save(criarAlertaDaMensagem(message, placaMonitorada));

        // 2. Formatar Texto
        String textoNotificacao = formatTelegramMessage(alertaSalvo);

        // 3. Notificar Grupo Geral (Sempre envia)
        telegramService.sendToGeneralChannel(textoNotificacao);

        // 4. Notificar Usuário Específico (Se configurado)
        notificarUsuarioEspecifico(placaMonitorada, textoNotificacao);

        // 5. Publicar evento de confirmação
        publicarAlertaConfirmado(alertaSalvo);
    }

    private void notificarUsuarioEspecifico(PlacaMonitorada placa, String textoBase) {
        String chatIdPessoal = placa.getTelegramChatId();

        if (StringUtils.hasText(chatIdPessoal)) {
            String textoPessoal = "⚠️ <b>SEU VEÍCULO FOI DETECTADO!</b> ⚠️\n\n" + textoBase;
            telegramService.enviarMensagem(textoPessoal, chatIdPessoal);
            logger.info("Notificação privada enviada para interessado: {} (ChatId: {})",
                    placa.getInteressado(), chatIdPessoal);
        } else {
            logger.info("Nenhum Chat ID privado configurado para a placa {}. Apenas grupo notificado.",
                    placa.getPlaca());
        }
    }

    // Método auxiliar para publicar no RabbitMQ
    private void publicarAlertaConfirmado(AlertaPassagem alerta) {
        try {
            // Cria um DTO a partir da entidade JÁ SALVA
            AlertaPassagemDTO alertaDTO = new AlertaPassagemDTO(alerta);
            // Converte o DTO para uma string JSON
            String alertaJson = objectMapper.writeValueAsString(alertaDTO);
            // Publica na exchange com uma routing key específica para alertas
            rabbitTemplate.convertAndSend("radares_exchange", "alerta.confirmado", alertaJson);
            logger.info("Alerta da placa {} processado e notificado.", alerta.getPlaca());
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar alerta", e);
        }
    }

    private AlertaPassagem criarAlertaDaMensagem(String message, PlacaMonitorada placaMonitorada) {
        String[] parts = message.split("\\|");
        String concessionaria = parts[0].toUpperCase();

        LocalDate data = LocalDate.parse(parts[1]);
        LocalTime hora = LocalTime.parse(parts[2]);
        String praca = "N/A", rodovia = "N/A", km = "N/A", sentido = "N/A";

        switch (concessionaria) {
            case "RONDON":
                if (parts.length >= 7) {
                    rodovia = parts[4];
                    km = parts[5];
                    sentido = parts[6];
                }
                break;
            default: // CART, EIXO, ENTREVIAS e outros com 8 partes
                if (parts.length >= 8) {
                    praca = parts[4];
                    rodovia = parts[5];
                    km = parts[6];
                    sentido = parts[7];
                }
        }

        return AlertaPassagem.builder()
                .concessionaria(concessionaria)
                .data(data).hora(hora).placa(placaMonitorada.getPlaca())
                .praca(praca).rodovia(rodovia).km(km).sentido(sentido)
                .placaMonitorada(placaMonitorada)
                .build();
    }

    /**
     * NOVO: Método auxiliar privado para formatar a mensagem do Telegram.
     */
    private String formatTelegramMessage(AlertaPassagem alerta) {
        PlacaMonitorada placaMonitorada = alerta.getPlacaMonitorada();
        String dataFormatada = alerta.getData().format(dateFormatter);
        String horaFormatada = alerta.getHora().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        List<String> placaInfoParts = new ArrayList<>();
        if (isValid(placaMonitorada.getMarcaModelo())) placaInfoParts.add(placaMonitorada.getMarcaModelo());
        if (isValid(placaMonitorada.getCor())) placaInfoParts.add(placaMonitorada.getCor());
        String placaInfoAdicional = String.join(", ", placaInfoParts);

        List<String> localizacaoParts = new ArrayList<>();
        if (isValid(alerta.getRodovia())) localizacaoParts.add(alerta.getRodovia());
        if (isValid(alerta.getKm())) localizacaoParts.add("km " + alerta.getKm());
        if (isValid(alerta.getSentido())) localizacaoParts.add("Sentido: " + alerta.getSentido());
        if (isValid(alerta.getPraca())) localizacaoParts.add(alerta.getPraca());
        String localizacaoCompleta = String.join(" - ", localizacaoParts);

        return String.format(
                "🚨 <b>Concessionária %s</b> 🚨\n" +
                        "🗓️ Data: %s\n" +
                        "⏰ Horário: %s\n" +
                        "🚨 Placa: <b>%s</b> \n" +
                        "🚗 Marca/Modelo: %s \n" +
                        "📍 Local: %s\n\n" +
                        "⚠️ Motivo: %s\n" +
                        "👤 Interessado: %s",
                alerta.getConcessionaria(), dataFormatada, horaFormatada,
                placaMonitorada.getPlaca(), placaInfoAdicional,
                localizacaoCompleta,
                placaMonitorada.getMotivo(), placaMonitorada.getInteressado()
        );
    }

    /**
     * Verifica se uma string contém uma informação útil.
     */
    private boolean isValid(String value) {
        return value != null && !value.isBlank() && !"N/A".equalsIgnoreCase(value);
    }

    /**
     * Lista todas as placas monitoradas de forma paginada.
     */
    @Transactional(readOnly = true)
    public Page<PlacaMonitoradaDTO> findAll(Pageable pageable) {
        Page<PlacaMonitorada> page = placaRepository.findAll(pageable);
        return page.map(PlacaMonitoradaDTO::new); // Converte cada entidade para DTO
    }

    /**
     * Busca um único monitoramento pelo seu ID.
     * @param id O ID da placa monitorada a ser buscada.
     * @return um DTO da placa encontrada.
     * @throws EntityNotFoundException se nenhuma placa for encontrada com o ID fornecido.
     */
    @Transactional(readOnly = true)
    public PlacaMonitoradaDTO findById(Long id) {
        // O método .findById(id) do repositório retorna um Optional.
        // Usamos .map(PlacaMonitoradaDTO::new) para converter a entidade em DTO se ela for encontrada.
        // E usamos .orElseThrow() para lançar uma exceção se o Optional estiver vazio.
        return placaRepository.findById(id)
                .map(PlacaMonitoradaDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado para o ID: " + id));
    }

    /**
     * Cadastra uma nova placa para monitoramento.
     */
    public PlacaMonitoradaDTO create(PlacaMonitoradaDTO dto) {
        // 1. Validação para evitar placas duplicadas
        String placaFormatada = dto.getPlaca().toUpperCase().trim();
        placaRepository.findByPlaca(placaFormatada)
                .ifPresent(existingEntity -> {
                    throw new IllegalArgumentException("A placa " + placaFormatada + " já está cadastrada.");
                });

        // 2. Mapeia o DTO para uma nova entidade
        PlacaMonitorada novaEntidade = new PlacaMonitorada();
        dto.setPlaca(placaFormatada); // Garante que o DTO também tenha a placa formatada
        mapDtoToEntity(dto, novaEntidade);

        // =======================================================
        // ##           CORREÇÃO PRINCIPAL APLICADA AQUI        ##
        // =======================================================

        // 3. SALVA a entidade primeiro e captura a instância retornada com o ID.
        PlacaMonitorada entidadeSalva = placaRepository.save(novaEntidade);

        // 4. AGORA o log mostrará o ID correto.
        logger.info("Nova placa monitorada salva com sucesso. ID: {}, Placa: {}", entidadeSalva.getId(), entidadeSalva.getPlaca());

        // 5. Retorna um NOVO DTO criado a partir da entidade JÁ SALVA.
        return new PlacaMonitoradaDTO(entidadeSalva);
    }

    /**
     * Atualiza uma placa monitorada existente.
     */
    public PlacaMonitoradaDTO update(Long id, PlacaMonitoradaDTO dto) {
        // Busca a entidade no banco, lança exceção se não encontrar
        PlacaMonitorada entity = placaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Placa com ID " + id + " não encontrada."));

        mapDtoToEntity(dto, entity);
        entity = placaRepository.save(entity);
        return new PlacaMonitoradaDTO(entity);
    }

    /**
     * Deleta uma placa do monitoramento.
     */
    public void delete(Long id) {
        if (!placaRepository.existsById(id)) {
            throw new EntityNotFoundException("Placa com ID " + id + " não encontrada para exclusão.");
        }
        placaRepository.deleteById(id);
        logger.info("Placa monitorada com ID {} e todos os seus alertas associados foram deletados.", id);
    }

    /**
     * Busca o histórico de alertas e já converte para DTO.
     */
    @Transactional(readOnly = true)
    public Page<AlertaPassagemDTO> findAlerts(Pageable pageable) {
        Page<AlertaPassagem> page = alertaRepository.findAll(pageable);
        // Usa .map() para converter cada AlertaPassagem em um AlertaPassagemDTO
        return page.map(AlertaPassagemDTO::new);
    }

    // Método auxiliar para mapear os dados do DTO para a Entidade
    private void mapDtoToEntity(PlacaMonitoradaDTO dto, PlacaMonitorada entity) {
        entity.setPlaca(dto.getPlaca().toUpperCase().trim());
        entity.setMarcaModelo(dto.getMarcaModelo());
        entity.setCor(dto.getCor());
        entity.setMotivo(dto.getMotivo());
        entity.setStatusAtivo(dto.isStatusAtivo());
        entity.setObservacao(dto.getObservacao());
        entity.setInteressado(dto.getInteressado());
        entity.setTelefone(dto.getTelefone());
        entity.setTelegramChatId(dto.getTelegramChatId());
    }
}
