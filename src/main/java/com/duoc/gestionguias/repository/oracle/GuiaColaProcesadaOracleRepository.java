package com.duoc.gestionguias.repository.oracle;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.duoc.gestionguias.model.GuiaColaProcesada;

@Repository
public class GuiaColaProcesadaOracleRepository {

    private final JdbcTemplate oracleJdbcTemplate;

    public GuiaColaProcesadaOracleRepository(
            @Qualifier("oracleJdbcTemplate") JdbcTemplate oracleJdbcTemplate
    ) {
        this.oracleJdbcTemplate = oracleJdbcTemplate;
    }

    /*
     * Inserta en Oracle Cloud el mensaje consumido desde RabbitMQ.
     */
    public GuiaColaProcesada save(GuiaColaProcesada registro) {
        if (registro.getFechaProcesamiento() == null) {
            registro.setFechaProcesamiento(LocalDateTime.now());
        }

        String sql = """
                INSERT INTO GUIAS_COLA_PROCESADAS (
                    GUIA_ID,
                    TRANSPORTISTA,
                    FECHA_GUIA,
                    CLIENTE,
                    DIRECCION_DESTINO,
                    DESCRIPCION_PEDIDO,
                    ESTADO,
                    ORIGEN,
                    FECHA_EVENTO,
                    FECHA_PROCESAMIENTO
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        oracleJdbcTemplate.update(
                sql,
                registro.getGuiaId(),
                registro.getTransportista(),
                registro.getFechaGuia() != null ? Date.valueOf(registro.getFechaGuia()) : null,
                registro.getCliente(),
                registro.getDireccionDestino(),
                registro.getDescripcionPedido(),
                registro.getEstado(),
                registro.getOrigen(),
                registro.getFechaEvento() != null ? Timestamp.valueOf(registro.getFechaEvento()) : null,
                Timestamp.valueOf(registro.getFechaProcesamiento())
        );

        return registro;
    }
}