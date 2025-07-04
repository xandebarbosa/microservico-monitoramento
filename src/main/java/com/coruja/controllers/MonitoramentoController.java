package com.coruja.controllers;

import com.coruja.dto.PlacaMonitoradaDTO;
import com.coruja.services.MonitoramentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoramento")
public class MonitoramentoController {

    private final MonitoramentoService service;

    @Autowired
    public MonitoramentoController(MonitoramentoService service) {
        this.service = service;
    }

    /**
     * Endpoint para buscar uma única placa monitorada pelo seu ID.
     * Ex: GET /api/monitoramento/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlacaMonitoradaDTO> findById(@PathVariable Long id) {
        PlacaMonitoradaDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
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
        dto = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Endpoint para atualizar uma placa monitorada pelo seu ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlacaMonitoradaDTO> update(@PathVariable Long id, @RequestBody PlacaMonitoradaDTO dto) {
        dto = service.update(id, dto);
        return ResponseEntity.ok(dto);
    }

    /**
     * Endpoint para deletar uma placa do monitoramento pelo seu ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content, indicando sucesso
    }
}
