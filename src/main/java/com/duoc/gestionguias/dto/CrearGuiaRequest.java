package com.duoc.gestionguias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO usado para recibir los datos al crear una guia.
 */
@Getter
@Setter
public class CrearGuiaRequest {

    @NotBlank
    private String transportista;

    @NotNull
    private LocalDate fecha;

    @NotBlank
    private String cliente;

    @NotBlank
    private String direccionDestino;

    @NotBlank
    private String descripcionPedido;
}