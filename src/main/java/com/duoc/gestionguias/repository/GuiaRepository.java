package com.duoc.gestionguias.repository;

import com.duoc.gestionguias.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio JPA para acceder a las guias de despacho.
 */
public interface GuiaRepository extends JpaRepository<GuiaDespacho, Long> {

    // Busca guias filtrando por transportista y fecha.
    List<GuiaDespacho> findByTransportistaAndFecha(String transportista, LocalDate fecha);
}