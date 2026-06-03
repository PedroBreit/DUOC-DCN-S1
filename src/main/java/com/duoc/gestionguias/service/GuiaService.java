package com.duoc.gestionguias.service;

import com.duoc.gestionguias.dto.ActualizarGuiaRequest;
import com.duoc.gestionguias.dto.CrearGuiaRequest;
import com.duoc.gestionguias.model.GuiaDespacho;
import com.duoc.gestionguias.repository.GuiaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final S3Service s3Service;

    @Value("${app.storage.local-path}")
    private String storagePath;

    public GuiaService(GuiaRepository guiaRepository, S3Service s3Service) {
        this.guiaRepository = guiaRepository;
        this.s3Service = s3Service;
    }

    public GuiaDespacho crearGuia(CrearGuiaRequest request) {
        try {
            Files.createDirectories(Paths.get(storagePath));

            GuiaDespacho guia = GuiaDespacho.builder()
                    .transportista(request.getTransportista())
                    .fecha(request.getFecha())
                    .cliente(request.getCliente())
                    .direccionDestino(request.getDireccionDestino())
                    .descripcionPedido(request.getDescripcionPedido())
                    .estado("GENERADA")
                    .fechaCreacion(LocalDateTime.now())
                    .fechaActualizacion(LocalDateTime.now())
                    .build();

            guia = guiaRepository.save(guia);

            String nombreArchivo = "guia-" + guia.getId() + ".txt";
            Path archivoPath = Paths.get(storagePath, nombreArchivo);

            String contenido = generarContenidoGuia(guia);

            Files.writeString(
                    archivoPath,
                    contenido,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            guia.setNombreArchivo(nombreArchivo);
            guia.setRutaLocal(archivoPath.toString());

            return guiaRepository.save(guia);

        } catch (IOException e) {
            throw new RuntimeException("Error al generar la guia de despacho", e);
        }
    }

    public List<GuiaDespacho> listarGuias() {
        return guiaRepository.findAll();
    }

    public GuiaDespacho buscarPorId(Long id) {
        return guiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guia no encontrada con ID: " + id));
    }

    public List<GuiaDespacho> buscarPorTransportistaYFecha(String transportista, LocalDate fecha) {
        return guiaRepository.findByTransportistaAndFecha(transportista, fecha);
    }

    public GuiaDespacho actualizarGuia(Long id, ActualizarGuiaRequest request) {
        try {
            GuiaDespacho guia = buscarPorId(id);

            guia.setCliente(request.getCliente());
            guia.setDireccionDestino(request.getDireccionDestino());
            guia.setDescripcionPedido(request.getDescripcionPedido());
            guia.setEstado(request.getEstado());
            guia.setFechaActualizacion(LocalDateTime.now());

            String contenido = generarContenidoGuia(guia);

            Files.writeString(
                    Paths.get(guia.getRutaLocal()),
                    contenido,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return guiaRepository.save(guia);

        } catch (IOException e) {
            throw new RuntimeException("Error al actualizar la guia", e);
        }
    }

    public Resource descargarGuia(Long id) {
        GuiaDespacho guia = buscarPorId(id);

        Path path = Paths.get(guia.getRutaLocal());

        if (!Files.exists(path)) {
            throw new RuntimeException("El archivo de la guia no existe");
        }

        return new FileSystemResource(path);
    }

    public void eliminarGuia(Long id) {
        GuiaDespacho guia = buscarPorId(id);

        try {
            if (guia.getS3Key() != null && !guia.getS3Key().isBlank()) {
                s3Service.eliminarArchivo(guia.getS3Key());
            }

            if (guia.getRutaLocal() != null) {
                Files.deleteIfExists(Paths.get(guia.getRutaLocal()));
            }

            guiaRepository.deleteById(id);

        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar la guia", e);
        }
    }

    public GuiaDespacho subirGuiaAS3(Long id) {
        GuiaDespacho guia = buscarPorId(id);

        Path archivoPath = Paths.get(guia.getRutaLocal());

        if (!Files.exists(archivoPath)) {
            throw new RuntimeException("El archivo local de la guia no existe");
        }

        String s3Key = guia.getFecha()
                + "/"
                + guia.getTransportista().replace(" ", "_")
                + "/"
                + guia.getNombreArchivo();

        s3Service.subirArchivo(archivoPath, s3Key);

        guia.setS3Key(s3Key);
        guia.setEstado("SUBIDA_S3");
        guia.setFechaActualizacion(LocalDateTime.now());

        return guiaRepository.save(guia);
    }

    private String generarContenidoGuia(GuiaDespacho guia) {
        return """
                GUIA DE DESPACHO
                ==============================

                ID: %s
                Transportista: %s
                Fecha: %s
                Cliente: %s
                Direccion destino: %s
                Descripcion pedido: %s
                Estado: %s

                Archivo generado automaticamente por el microservicio gestion-guias.
                """.formatted(
                guia.getId(),
                guia.getTransportista(),
                guia.getFecha(),
                guia.getCliente(),
                guia.getDireccionDestino(),
                guia.getDescripcionPedido(),
                guia.getEstado()
        );
    }
}