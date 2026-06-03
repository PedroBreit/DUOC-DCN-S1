package com.duoc.gestionguias.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarGuiaRequest {

    @NotBlank
    private String cliente;

    @NotBlank
    private String direccionDestino;

    @NotBlank
    private String descripcionPedido;

    @NotBlank
    private String estado;
}