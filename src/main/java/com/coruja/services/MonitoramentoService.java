package com.coruja.services;

import com.coruja.dto.PlacaMonitoradaDTO;
import com.coruja.entities.PlacaMonitorada;
import com.coruja.repositories.PlacaMonitoradaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class MonitoramentoService {

    private final PlacaMonitoradaRepository repository;

    @Autowired
    public MonitoramentoService(PlacaMonitoradaRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista todas as placas monitoradas de forma paginada.
     */
    @Transactional(readOnly = true)
    public Page<PlacaMonitoradaDTO> findAll(Pageable pageable) {
        Page<PlacaMonitorada> page = repository.findAll(pageable);
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
        return repository.findById(id)
                .map(PlacaMonitoradaDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado para o ID: " + id));
    }

    /**
     * Cadastra uma nova placa para monitoramento.
     */
    @Transactional
    public PlacaMonitoradaDTO create(PlacaMonitoradaDTO dto) {
        // 1. Antes de salvar, verifica se a placa já existe no banco.
        repository.findByPlaca(dto.getPlaca().toUpperCase().trim())
                .ifPresent(existingEntity -> {
                    // 2. Se o 'Optional' contiver um valor, significa que a placa foi encontrada,
                    //    então lançamos uma exceção clara.
                    throw new IllegalArgumentException("A placa " + dto.getPlaca() + " já está cadastrada no monitoramento.");
                });

        // 3. Se a placa não existe, o código continua normalmente.
        PlacaMonitorada entity = new PlacaMonitorada();
        // Garante que a placa seja salva em maiúsculas e sem espaços
        dto.setPlaca(dto.getPlaca().toUpperCase().trim());
        mapDtoToEntity(dto, entity);

        entity = repository.save(entity);
        return new PlacaMonitoradaDTO(entity);
    }

    /**
     * Atualiza uma placa monitorada existente.
     */
    @Transactional
    public PlacaMonitoradaDTO update(Long id, PlacaMonitoradaDTO dto) {
        // Busca a entidade no banco, lança exceção se não encontrar
        PlacaMonitorada entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Placa com ID " + id + " não encontrada."));

        mapDtoToEntity(dto, entity);
        entity = repository.save(entity);
        return new PlacaMonitoradaDTO(entity);
    }

    /**
     * Deleta uma placa do monitoramento.
     */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Placa com ID " + id + " não encontrada para exclusão.");
        }
        repository.deleteById(id);
    }



    // Método auxiliar para mapear os dados do DTO para a Entidade
    private void mapDtoToEntity(PlacaMonitoradaDTO dto, PlacaMonitorada entity) {
        entity.setPlaca(dto.getPlaca());
        entity.setMarcaModelo(dto.getMarcaModelo());
        entity.setCor(dto.getCor());
        entity.setMotivo(dto.getMotivo());
        entity.setStatusAtivo(dto.isStatusAtivo());
        entity.setObservacao(dto.getObservacao());
        entity.setInteressado(dto.getInteressado());
    }
}
