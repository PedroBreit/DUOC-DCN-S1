package com.duoc.gestionguias.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa una guia de despacho en la base de datos.
 */
@Entity
@Table(name = "guias_despacho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transportista;

    private LocalDate fecha;

    private String cliente;

    private String direccionDestino;

    private String descripcionPedido;

    private String estado;

    private String nombreArchivo;

    // Ruta temporal del archivo generado. En EC2 esta ruta apunta a EFS.
    private String rutaLocal;

    // Ruta del archivo dentro del bucket S3.
    private String s3Key;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}