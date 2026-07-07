package com.duoc.gestionguias.service.messaging;

import com.duoc.gestionguias.dto.mensaje.GuiaDespachoMessage;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuiaQueueProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing.pendientes}")
    private String pendientesRoutingKey;

    @Value("${app.rabbitmq.routing.errores}")
    private String erroresRoutingKey;

    public GuiaQueueProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /*
     * Envia una guia a la cola principal de procesamiento.
     * Esta cola representa los mensajes pendientes de ser consumidos.
     */
    public void enviarGuiaPendiente(GuiaDespachoMessage mensaje) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, pendientesRoutingKey, mensaje);
        } catch (Exception ex) {
            enviarGuiaConError(mensaje, ex.getMessage());
        }
    }

    /*
     * Envia una guia a la cola de errores.
     * Se utiliza cuando ocurre un problema al enviar o procesar el mensaje.
     */
    public void enviarGuiaConError(GuiaDespachoMessage mensaje, String motivoError) {
        mensaje.setEstado("ERROR_COLA");
        mensaje.setOrigen("ERROR: " + motivoError);

        rabbitTemplate.convertAndSend(exchangeName, erroresRoutingKey, mensaje);
    }
}