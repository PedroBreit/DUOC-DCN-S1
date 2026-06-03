package com.duoc.gestionguias.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;

/**
 * Servicio encargado de comunicarse con Amazon S3.
 */
@Service
public class S3Service {

    private final S3Client s3Client;

    // Nombre del bucket S3 configurado por variable de entorno.
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Sube un archivo local o desde EFS hacia S3.
    public String subirArchivo(Path archivoPath, String s3Key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("text/plain")
                .build();

        s3Client.putObject(request, RequestBody.fromFile(archivoPath));

        return s3Key;
    }

    // Descarga un archivo desde S3 usando su key.
    public byte[] descargarArchivo(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);

        return response.asByteArray();
    }

    // Elimina un archivo desde S3 si existe una key valida.
    public void eliminarArchivo(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
    }
}