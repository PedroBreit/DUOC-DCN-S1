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
     * Busca una guia existente en la base de datos y envia sus datos
     * a la cola principal de RabbitMQ.
     */
    public void enviarGuiaACola(Long id) {
        GuiaDespacho guia = guiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe la guia con ID: " + id));

        GuiaDespachoMessage mensaje = new GuiaDespachoMessage(
                guia.getId(),
                guia.getTransportista(),
                guia.getFecha(),
                guia.getCliente(),
                guia.getDireccionDestino(),
                guia.getDescripcionPedido(),
                guia.getEstado(),
                LocalDateTime.now(),
                "API_ENVIAR_COLA"
        );

        guiaQueueProducer.enviarGuiaPendiente(mensaje);
    }
}