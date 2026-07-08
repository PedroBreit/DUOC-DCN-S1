package com.duoc.gestionguias.service.messaging;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duoc.gestionguias.dto.CrearGuiaRequest;
import com.duoc.gestionguias.dto.mensaje.GuiaDespachoMessage;
import com.duoc.gestionguias.model.GuiaColaProcesada;
import com.duoc.gestionguias.model.GuiaDespacho;
import com.duoc.gestionguias.repository.oracle.GuiaColaProcesadaOracleRepository;
import com.duoc.gestionguias.service.GuiaService;
import com.duoc.gestionguias.exception.ColaVaciaException;

@Service
public class GuiaQueueConsumerService {

    private final RabbitTemplate rabbitTemplate;
    private final GuiaService guiaService;
    private final GuiaColaProcesadaOracleRepository guiaColaProcesadaOracleRepository;
    private final GuiaQueueProducer guiaQueueProducer;

    @Value("${app.rabbitmq.queue.pendientes}")
    private String pendientesQueueName;

    public GuiaQueueConsumerService(
            RabbitTemplate rabbitTemplate,
            GuiaService guiaService,
            GuiaColaProcesadaOracleRepository guiaColaProcesadaOracleRepository,
            GuiaQueueProducer guiaQueueProducer
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.guiaService = guiaService;
        this.guiaColaProcesadaOracleRepository = guiaColaProcesadaOracleRepository;
        this.guiaQueueProducer = guiaQueueProducer;
    }

    /*
     * Flujo corregido S8:
     * Consume una solicitud desde RabbitMQ, crea la guia real,
     * genera el archivo, lo sube a S3 y registra el procesamiento en Oracle Cloud.
     */
    public GuiaColaProcesada procesarUnMensajePendiente() {
        Object mensajeRecibido = rabbitTemplate.receiveAndConvert(pendientesQueueName);

        if (mensajeRecibido == null) {
            throw new ColaVaciaException("No existen mensajes pendientes en la cola principal");
        }

        GuiaDespachoMessage mensaje = null;

        try {
            mensaje = (GuiaDespachoMessage) mensajeRecibido;

            if (Boolean.TRUE.equals(mensaje.getForzarError())) {
                throw new RuntimeException("Error forzado para demostrar envio a la DLQ");
            }

            CrearGuiaRequest request = convertirMensajeACrearGuiaRequest(mensaje);

            /*
             * 1. Crea la guia en la base de datos local.
             * 2. Genera el archivo de guia.
             */
            GuiaDespacho guiaCreada = guiaService.crearGuia(request);

            /*
             * 3. Sube el archivo generado a S3.
             */
            GuiaDespacho guiaSubidaAS3 = guiaService.subirGuiaAS3(guiaCreada.getId());

            /*
             * 4. Guarda evidencia del procesamiento en Oracle Cloud.
             */
            GuiaColaProcesada registro = convertirARegistroProcesado(guiaSubidaAS3, mensaje);

            return guiaColaProcesadaOracleRepository.save(registro);

        } catch (Exception ex) {
            if (mensaje != null) {
                guiaQueueProducer.enviarGuiaConError(mensaje, ex.getMessage());
            }

            throw new RuntimeException("Error al procesar mensaje de RabbitMQ: " + ex.getMessage(), ex);
        }
    }

    private CrearGuiaRequest convertirMensajeACrearGuiaRequest(GuiaDespachoMessage mensaje) {
        CrearGuiaRequest request = new CrearGuiaRequest();
        request.setTransportista(mensaje.getTransportista());
        request.setFecha(mensaje.getFecha());
        request.setCliente(mensaje.getCliente());
        request.setDireccionDestino(mensaje.getDireccionDestino());
        request.setDescripcionPedido(mensaje.getDescripcionPedido());
        return request;
    }

    private GuiaColaProcesada convertirARegistroProcesado(
            GuiaDespacho guia,
            GuiaDespachoMessage mensaje
    ) {
        GuiaColaProcesada registro = new GuiaColaProcesada();
        registro.setGuiaId(guia.getId());
        registro.setTransportista(guia.getTransportista());
        registro.setFechaGuia(guia.getFecha());
        registro.setCliente(guia.getCliente());
        registro.setDireccionDestino(guia.getDireccionDestino());
        registro.setDescripcionPedido(guia.getDescripcionPedido());
        registro.setEstado(guia.getEstado());
        registro.setOrigen(mensaje.getOrigen());
        registro.setFechaEvento(mensaje.getFechaEvento());
        registro.setFechaProcesamiento(LocalDateTime.now());
        return registro;
    }
}