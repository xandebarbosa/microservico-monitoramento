package com.coruja.repositories;

import com.coruja.entities.PlacaMonitorada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacaMonitoradaRepository extends JpaRepository<PlacaMonitorada, Long> {
    // Método para verificar se uma placa ativa está sendo monitorada
    Optional<PlacaMonitorada> findByPlacaAndStatusAtivo(String placa, boolean statusAtivo);

    // Método para verificar se uma placa já existe.
    Optional<PlacaMonitorada> findByPlaca(String placa);
}
