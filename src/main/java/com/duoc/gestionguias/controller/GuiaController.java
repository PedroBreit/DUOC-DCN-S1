package com.duoc.gestionguias.controller;

import com.duoc.gestionguias.dto.ActualizarGuiaRequest;
import com.duoc.gestionguias.dto.CrearGuiaRequest;
import com.duoc.gestionguias.model.GuiaDespacho;
import com.duoc.gestionguias.service.GuiaService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    @PostMapping
    public ResponseEntity<GuiaDespacho> crearGuia(@Valid @RequestBody CrearGuiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.crearGuia(request));
    }

    @GetMapping
    public ResponseEntity<List<GuiaDespacho>> listarGuias() {
        return ResponseEntity.ok(guiaService.listarGuias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaDespacho> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<GuiaDespacho>> buscarPorTransportistaYFecha(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(guiaService.buscarPorTransportistaYFecha(transportista, fecha));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespacho> actualizarGuia(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGuiaRequest request
    ) {
        return ResponseEntity.ok(guiaService.actualizarGuia(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable Long id) {
        guiaService.eliminarGuia(id);
        return ResponseEntity.noContent().build();
    }

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

    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<GuiaDespacho> subirGuiaAS3(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.subirGuiaAS3(id));
    }
}