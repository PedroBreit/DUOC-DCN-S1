package com.duoc.gestionguias.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private String rutaLocal;

    private String s3Key;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}