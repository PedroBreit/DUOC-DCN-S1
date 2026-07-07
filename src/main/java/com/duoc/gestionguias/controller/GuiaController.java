package com.duoc.gestionguias.controller;

import com.duoc.gestionguias.dto.ActualizarGuiaRequest;
import com.duoc.gestionguias.dto.CrearGuiaRequest;
import com.duoc.gestionguias.model.GuiaColaProcesada;
import com.duoc.gestionguias.model.GuiaDespacho;
import com.duoc.gestionguias.service.GuiaService;
import com.duoc.gestionguias.service.messaging.GuiaQueueConsumerService;
import com.duoc.gestionguias.service.messaging.GuiaQueueService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para gestionar las guias de despacho.
 */
@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;
    private final GuiaQueueService guiaQueueService;
    private final GuiaQueueConsumerService guiaQueueConsumerService;

    public GuiaController(
            GuiaService guiaService,
            GuiaQueueService guiaQueueService,
            GuiaQueueConsumerService guiaQueueConsumerService
    ) {
        this.guiaService = guiaService;
        this.guiaQueueService = guiaQueueService;
        this.guiaQueueConsumerService = guiaQueueConsumerService;
    }

    // Crea una guia y genera su archivo temporal.
    @PostMapping
    public ResponseEntity<GuiaDespacho> crearGuia(@Valid @RequestBody CrearGuiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.crearGuia(request));
    }

    // Lista todas las guias registradas.
    @GetMapping
    public ResponseEntity<List<GuiaDespacho>> listarGuias() {
        return ResponseEntity.ok(guiaService.listarGuias());
    }

    // Busca una guia por ID.
    @GetMapping("/{id}")
    public ResponseEntity<GuiaDespacho> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.buscarPorId(id));
    }

    // Busca guias por transportista y fecha.
    @GetMapping("/buscar")
    public ResponseEntity<List<GuiaDespacho>> buscarPorTransportistaYFecha(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(guiaService.buscarPorTransportistaYFecha(transportista, fecha));
    }

    // Actualiza una guia existente.
    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespacho> actualizarGuia(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGuiaRequest request
    ) {
        return ResponseEntity.ok(guiaService.actualizarGuia(id, request));
    }

    // Elimina una guia y sus archivos asociados.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable Long id) {
        guiaService.eliminarGuia(id);
        return ResponseEntity.noContent().build();
    }

    // Descarga el archivo de la guia desde EFS o S3.
    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargarGuia(@PathVariable Long id) {
        GuiaDespacho guia = guiaService.buscarPorId(id);
        Resource archivo = guiaService.descargarGuia(id);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + guia.getNombreArchivo() + "\""
                )
                .body(archivo);
    }

    // Sube la guia generada a S3.
    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<GuiaDespacho> subirGuiaAS3(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.subirGuiaAS3(id));
    }

    /*
     * Envia una guia existente a la cola principal de RabbitMQ.
     * Este endpoint permite evidenciar el comportamiento asincrono solicitado en S8.
     */
    @PostMapping("/{id}/enviar-cola")
    public ResponseEntity<String> enviarGuiaACola(@PathVariable Long id) {
        guiaQueueService.enviarGuiaACola(id);
        return ResponseEntity.ok("Guia enviada correctamente a la cola principal de RabbitMQ");
    }

    /*
    * Envia una guia existente a la cola de errores de RabbitMQ.
    * Este endpoint se usa para evidenciar el manejo de errores solicitado en S8.
    */
    @PostMapping("/{id}/enviar-cola-error")
    public ResponseEntity<String> enviarGuiaAColaError(@PathVariable Long id) {
        guiaQueueService.enviarGuiaAColaError(id);
        return ResponseEntity.ok("Guia enviada correctamente a la cola de errores de RabbitMQ");
    }
    
    /*
     * Consume un mensaje desde la cola principal de RabbitMQ
     * y lo guarda en la tabla GUIAS_COLA_PROCESADAS.
     */
    @PostMapping("/colas/procesar")
    public ResponseEntity<GuiaColaProcesada> procesarMensajeCola() {
        return ResponseEntity.ok(guiaQueueConsumerService.procesarUnMensajePendiente());
    }
}