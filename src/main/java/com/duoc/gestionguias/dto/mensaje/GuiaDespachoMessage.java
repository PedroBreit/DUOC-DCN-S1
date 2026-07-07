package com.duoc.gestionguias.dto.mensaje;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuiaDespachoMessage implements Serializable {

    private Long guiaId;
    private String transportista;
    private LocalDate fecha;
    private String cliente;
    private String direccionDestino;
    private String descripcionPedido;
    private String estado;
    private LocalDateTime fechaEvento;
    private String origen;

    public GuiaDespachoMessage() {
    }

    public GuiaDespachoMessage(
            Long guiaId,
            String transportista,
            LocalDate fecha,
            String cliente,
            String direccionDestino,
            String descripcionPedido,
            String estado,
            LocalDateTime fechaEvento,
            String origen
    ) {
        this.guiaId = guiaId;
        this.transportista = transportista;
        this.fecha = fecha;
        this.cliente = cliente;
        this.direccionDestino = direccionDestino;
        this.descripcionPedido = descripcionPedido;
        this.estado = estado;
        this.fechaEvento = fechaEvento;
        this.origen = origen;
    }

    public Long getGuiaId() {
        return guiaId;
    }

    public void setGuiaId(Long guiaId) {
        this.guiaId = guiaId;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getDireccionDestino() {
        return direccionDestino;
    }

    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    public String getDescripcionPedido() {
        return descripcionPedido;
    }

    public void setDescripcionPedido(String descripcionPedido) {
        this.descripcionPedido = descripcionPedido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }
}