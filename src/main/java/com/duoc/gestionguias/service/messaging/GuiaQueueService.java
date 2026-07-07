package com.duoc.gestionguias.service.messaging;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.duoc.gestionguias.dto.mensaje.GuiaDespachoMessage;
import com.duoc.gestionguias.model.GuiaDespacho;
import com.duoc.gestionguias.repository.GuiaRepository;

@Service
public class GuiaQueueService {

    private final GuiaRepository guiaRepository;
    private final GuiaQueueProducer guiaQueueProducer;

    public GuiaQueueService(
            GuiaRepository guiaRepository,
            GuiaQueueProducer guiaQueueProducer
    ) {
        this.guiaRepository = guiaRepository;
        this.guiaQueueProducer = guiaQueueProducer;
    }

    /*
     * Envia una guia existente a la cola principal de RabbitMQ.
     */
    public void enviarGuiaACola(Long id) {
        GuiaDespacho guia = buscarGuia(id);
        GuiaDespachoMessage mensaje = crearMensajeDesdeGuia(guia, "API_ENVIAR_COLA");
        guiaQueueProducer.enviarGuiaPendiente(mensaje);
    }

    /*
     * Envia una guia existente directamente a la cola de errores.
     * Este metodo permite evidenciar la segunda cola solicitada en S8.
     */
    public void enviarGuiaAColaError(Long id) {
        GuiaDespacho guia = buscarGuia(id);
        GuiaDespachoMessage mensaje = crearMensajeDesdeGuia(guia, "API_SIMULAR_ERROR");
        guiaQueueProducer.enviarGuiaConError(mensaje, "Error simulado para evidencia de cola de errores");
    }

    private GuiaDespacho buscarGuia(Long id) {
        return guiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe la guia con ID: " + id));
    }

    private GuiaDespachoMessage crearMensajeDesdeGuia(GuiaDespacho guia, String origen) {
        return new GuiaDespachoMessage(
                guia.getId(),
                guia.getTransportista(),
                guia.getFecha(),
                guia.getCliente(),
                guia.getDireccionDestino(),
                guia.getDescripcionPedido(),
                guia.getEstado(),
                LocalDateTime.now(),
                origen
        );
    }
}