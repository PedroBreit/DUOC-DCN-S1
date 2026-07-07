package com.duoc.gestionguias.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GUIAS_COLA_PROCESADAS")
public class GuiaColaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guiaId;
    private String transportista;
    private LocalDate fechaGuia;
    private String cliente;
    private String direccionDestino;

    @Column(length = 1000)
    private String descripcionPedido;

    private String estado;
    private String origen;
    private LocalDateTime fechaEvento;
    private LocalDateTime fechaProcesamiento;

    public GuiaColaProcesada() {
    }

    public Long getId() {
        return id;
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

    public LocalDate getFechaGuia() {
        return fechaGuia;
    }

    public void setFechaGuia(LocalDate fechaGuia) {
        this.fechaGuia = fechaGuia;
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

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }
}