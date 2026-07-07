package com.duoc.gestionguias.service.messaging;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duoc.gestionguias.dto.mensaje.GuiaDespachoMessage;
import com.duoc.gestionguias.model.GuiaColaProcesada;
import com.duoc.gestionguias.repository.GuiaColaProcesadaRepository;

@Service
public class GuiaQueueConsumerService {

    private final RabbitTemplate rabbitTemplate;
    private final GuiaColaProcesadaRepository guiaColaProcesadaRepository;
    private final GuiaQueueProducer guiaQueueProducer;

    @Value("${app.rabbitmq.queue.pendientes}")
    private String pendientesQueueName;

    public GuiaQueueConsumerService(
            RabbitTemplate rabbitTemplate,
            GuiaColaProcesadaRepository guiaColaProcesadaRepository,
            GuiaQueueProducer guiaQueueProducer
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.guiaColaProcesadaRepository = guiaColaProcesadaRepository;
        this.guiaQueueProducer = guiaQueueProducer;
    }

    /*
     * Consume un mensaje desde la cola principal y lo guarda
     * en la tabla GUIAS_COLA_PROCESADAS.
     */
    public GuiaColaProcesada procesarUnMensajePendiente() {
        Object mensajeRecibido = rabbitTemplate.receiveAndConvert(pendientesQueueName);

        if (mensajeRecibido == null) {
            throw new RuntimeException("No existen mensajes pendientes en la cola principal");
        }

        try {
            GuiaDespachoMessage mensaje = (GuiaDespachoMessage) mensajeRecibido;
            GuiaColaProcesada registro = convertirARegistroProcesado(mensaje);
            return guiaColaProcesadaRepository.save(registro);

        } catch (Exception ex) {
            if (mensajeRecibido instanceof GuiaDespachoMessage mensajeConError) {
                guiaQueueProducer.enviarGuiaConError(mensajeConError, ex.getMessage());
            }

            throw new RuntimeException("Error al procesar mensaje de RabbitMQ: " + ex.getMessage(), ex);
        }
    }

    private GuiaColaProcesada convertirARegistroProcesado(GuiaDespachoMessage mensaje) {
        GuiaColaProcesada registro = new GuiaColaProcesada();
        registro.setGuiaId(mensaje.getGuiaId());
        registro.setTransportista(mensaje.getTransportista());
        registro.setFechaGuia(mensaje.getFecha());
        registro.setCliente(mensaje.getCliente());
        registro.setDireccionDestino(mensaje.getDireccionDestino());
        registro.setDescripcionPedido(mensaje.getDescripcionPedido());
        registro.setEstado(mensaje.getEstado());
        registro.setOrigen(mensaje.getOrigen());
        registro.setFechaEvento(mensaje.getFechaEvento());
        registro.setFechaProcesamiento(LocalDateTime.now());
        return registro;
    }
}