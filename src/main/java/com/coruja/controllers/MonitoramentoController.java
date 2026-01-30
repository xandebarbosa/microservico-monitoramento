package com.coruja.controllers;

import com.coruja.dto.AlertaPassagemDTO;
import com.coruja.dto.PlacaMonitoradaDTO;
import com.coruja.entities.AlertaPassagem;
import com.coruja.services.MonitoramentoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoramento")
public class MonitoramentoController {

    private final MonitoramentoService service;

    @Autowired
    public MonitoramentoController(MonitoramentoService service) {
        this.service = service;
    }

    @GetMapping("/ultimos")
    public ResponseEntity<List<AlertaPassagemDTO>> getUltimos() {
        List<AlertaPassagemDTO> ultimos = service.buscarUltimosAlertas();
        return ResponseEntity.ok(ultimos);
    }

    /**
     * Endpoint para buscar uma única placa monitorada pelo seu ID.
     * Ex: GET /api/monitoramento/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlacaMonitoradaDTO> findById(@PathVariable Long id) {
        try {
            PlacaMonitoradaDTO dto = service.findById(id);
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint para listar todas as placas monitoradas com paginação.
     * Ex: GET /api/monitoramento?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<PlacaMonitoradaDTO>> findAll(Pageable pageable) {
        Page<PlacaMonitoradaDTO> list = service.findAll(pageable);
        return ResponseEntity.ok(list);
    }

    /**
     * Endpoint para cadastrar uma nova placa para monitoramento.
     * Envia os dados no corpo da requisição em formato JSON.
     */
    @PostMapping
    public ResponseEntity<PlacaMonitoradaDTO> create(@RequestBody PlacaMonitoradaDTO dto) {
        try {
            PlacaMonitoradaDTO created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            // Retorna 400 Bad Request (ou 409 Conflict) com a mensagem de "Placa já existe"
            Map<String, String> errorResponse = Collections.singletonMap("message", e.getMessage());
            return ResponseEntity.badRequest().body((PlacaMonitoradaDTO) errorResponse);
        }
    }

    /**
     * Endpoint para atualizar uma placa monitorada pelo seu ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlacaMonitoradaDTO> update(@PathVariable Long id, @RequestBody PlacaMonitoradaDTO dto) {
        try {
            PlacaMonitoradaDTO updated = service.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            // ✅ SIMPLIFICADO
            Map<String, String> errorResponse = Collections.singletonMap("message", e.getMessage());
            return ResponseEntity.badRequest().body((PlacaMonitoradaDTO) errorResponse);
        }
    }

    /**
     * Endpoint para deletar uma placa do monitoramento pelo seu ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/alertas")
    public ResponseEntity<Page<AlertaPassagemDTO>> findAlerts(Pageable pageable) {
        return ResponseEntity.ok(service.findAlerts(pageable));
    }

}
