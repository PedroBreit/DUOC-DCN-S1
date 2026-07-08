package com.duoc.gestionguias.service.messaging;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.duoc.gestionguias.dto.CrearGuiaRequest;
import com.duoc.gestionguias.dto.mensaje.GuiaDespachoMessage;

@Service
public class GuiaQueueService {

    private final GuiaQueueProducer guiaQueueProducer;

    public GuiaQueueService(GuiaQueueProducer guiaQueueProducer) {
        this.guiaQueueProducer = guiaQueueProducer;
    }

    /*
     * Flujo principal S8:
     * Recibe la solicitud de creacion de guia y la envia a RabbitMQ.
     * La guia real sera creada despues por el consumidor.
     */
    public void enviarSolicitudCreacionGuia(CrearGuiaRequest request) {
        GuiaDespachoMessage mensaje = new GuiaDespachoMessage(
                null,
                request.getTransportista(),
                request.getFecha(),
                request.getCliente(),
                request.getDireccionDestino(),
                request.getDescripcionPedido(),
                "SOLICITADA",
                LocalDateTime.now(),
                "API_CREAR_GUIA",
                false
        );

        guiaQueueProducer.enviarGuiaPendiente(mensaje);
    }

    /*
     * Flujo de error controlado S8:
     * Envia una solicitud marcada para fallar durante el consumo.
     * Esto permite demostrar que el mensaje termina en la DLQ.
     */
    public void enviarSolicitudCreacionGuiaConError(CrearGuiaRequest request) {
        GuiaDespachoMessage mensaje = new GuiaDespachoMessage(
                null,
                request.getTransportista(),
                request.getFecha(),
                request.getCliente(),
                request.getDireccionDestino(),
                request.getDescripcionPedido(),
                "SOLICITADA_ERROR",
                LocalDateTime.now(),
                "API_CREAR_GUIA_ERROR",
                true
        );

        guiaQueueProducer.enviarGuiaPendiente(mensaje);
    }
}