# Sistema de Gestion de Guias - Semana 8

Proyecto desarrollado para la asignatura **Desarrollo Cloud Native (CDY2204)**.

## Actividad

**Exp 3 - Semana 8:** Desarrollando un sistema asincrono con la utilizacion de colas.

Esta version extiende el sistema de gestion de guias de despacho incorporando mensajeria asincrona mediante **RabbitMQ**, utilizando una cola principal para procesamiento normal y una cola de errores para registrar mensajes con problemas.

## Repositorio

https://github.com/PedroBreit/DUOC-DCN-S1

## Objetivo de la implementacion S8

Implementar un flujo asincrono para el procesamiento de guias de despacho, utilizando RabbitMQ como broker de mensajeria, Spring Boot como backend, AWS API Gateway como punto publico de acceso, Docker para despliegue en EC2 y Oracle Cloud Autonomous Database para almacenar los mensajes procesados.

## Arquitectura implementada

```text
Cliente / Postman
      |
      v
AWS API Gateway - Stage s8
      |
      v
EC2 - Docker
      |
      +--> Contenedor Spring Boot: gestion-guias-app
      |
      +--> Contenedor RabbitMQ: rabbitmq-guias
                |
                +--> guias.pendientes.queue
                |
                +--> guias.errores.queue

Spring Boot
      |
      +--> H2 local para datos base de guias
      |
      +--> Oracle Cloud Autonomous DB para mensajes procesados
      |
      +--> Amazon S3 / EFS para archivos de guias
      |
      +--> Azure AD B2C para autenticacion JWT
```

## Tecnologias utilizadas

- Java 17
- Spring Boot 3.5.14
- Spring Web
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server
- RabbitMQ
- Spring AMQP
- Spring Boot Actuator
- Oracle Cloud Autonomous Database
- Oracle JDBC
- H2 Database
- AWS EC2
- AWS API Gateway
- Amazon S3
- Amazon EFS
- Docker
- GitHub Actions
- Azure AD B2C
- Postman

## Componentes RabbitMQ

La solucion utiliza un exchange directo y dos colas durables.

### Exchange

```text
guias.exchange
```

Tipo:

```text
direct
```

### Cola principal

```text
guias.pendientes.queue
```

Uso:

```text
Recibe las guias enviadas para procesamiento asincrono.
```

### Cola de errores

```text
guias.errores.queue
```

Uso:

```text
Recibe mensajes que presentan errores o que se envian como evidencia del manejo de errores.
```

### Routing keys

```text
guias.pendientes
guias.errores
```

## Flujo asincrono implementado

### 1. Crear guia

Se crea una guia de despacho mediante el endpoint tradicional del sistema.

```http
POST /api/guias
```

### 2. Enviar guia a la cola principal

El backend construye un mensaje `GuiaDespachoMessage` y lo envia a RabbitMQ.

```http
POST /api/guias/{id}/enviar-cola
```

La guia queda almacenada en:

```text
guias.pendientes.queue
```

### 3. Procesar mensaje pendiente

El backend consume manualmente un mensaje desde la cola principal y lo guarda en Oracle Cloud.

```http
POST /api/guias/colas/procesar
```

El registro queda almacenado en la tabla:

```text
GUIAS_COLA_PROCESADAS
```

### 4. Enviar guia a cola de errores

Endpoint agregado para evidenciar el uso de la segunda cola RabbitMQ.

```http
POST /api/guias/{id}/enviar-cola-error
```

La guia queda almacenada en:

```text
guias.errores.queue
```

## Endpoints S8

URL base de API Gateway:

```text
https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s8
```

| Metodo |              Endpoint               |                          Descripcion                          |
|--------|-------------------------------------|---------------------------------------------------------------|
|  POST  | `/api/guias`                        | Crea una guia de despacho                                     |
|  POST  | `/api/guias/{id}/enviar-cola`       | Envia una guia a la cola principal de RabbitMQ                |
|  POST  | `/api/guias/colas/procesar`         | Consume un mensaje desde RabbitMQ y lo guarda en Oracle Cloud |
|  POST  | `/api/guias/{id}/enviar-cola-error` | Envia una guia a la cola de errores de RabbitMQ               |

## Ejemplos de uso en Postman

### Crear guia

```http
POST https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s8/api/guias
Authorization: Bearer <TOKEN_ADMIN>
Content-Type: application/json
```

Body:

```json
{
  "transportista": "Transportes RabbitMQ",
  "fecha": "2026-07-07",
  "cliente": "Cliente Cola S8",
  "direccionDestino": "Av. RabbitMQ 123",
  "descripcionPedido": "Guia creada para prueba de envio a cola RabbitMQ"
}
```

### Enviar guia a cola principal

```http
POST https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s8/api/guias/4/enviar-cola
Authorization: Bearer <TOKEN_ADMIN>
```

Respuesta esperada:

```text
Guia enviada correctamente a la cola principal de RabbitMQ
```

### Procesar mensaje desde cola principal

```http
POST https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s8/api/guias/colas/procesar
Authorization: Bearer <TOKEN_ADMIN>
```

Respuesta esperada:

```json
{
  "id": null,
  "guiaId": 4,
  "transportista": "Transportes RabbitMQ",
  "fechaGuia": "2026-07-07",
  "cliente": "Cliente Cola S8",
  "direccionDestino": "Av. RabbitMQ 123",
  "descripcionPedido": "Guia creada para prueba de envio a cola RabbitMQ",
  "estado": "GENERADA",
  "origen": "API_ENVIAR_COLA",
  "fechaEvento": "2026-07-07T23:35:36.769133298",
  "fechaProcesamiento": "2026-07-07T23:42:09.774990413"
}
```

### Enviar guia a cola de errores

```http
POST https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s8/api/guias/4/enviar-cola-error
Authorization: Bearer <TOKEN_ADMIN>
```

Respuesta esperada:

```text
Guia enviada correctamente a la cola de errores de RabbitMQ
```

## Tabla Oracle Cloud

La informacion consumida desde RabbitMQ se guarda en Oracle Cloud Autonomous Database en la tabla:

```text
GUIAS_COLA_PROCESADAS
```

Script utilizado:

```sql
CREATE TABLE GUIAS_COLA_PROCESADAS (
    ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    GUIA_ID NUMBER,
    TRANSPORTISTA VARCHAR2(150),
    FECHA_GUIA DATE,
    CLIENTE VARCHAR2(150),
    DIRECCION_DESTINO VARCHAR2(255),
    DESCRIPCION_PEDIDO VARCHAR2(1000),
    ESTADO VARCHAR2(50),
    ORIGEN VARCHAR2(150),
    FECHA_EVENTO TIMESTAMP,
    FECHA_PROCESAMIENTO TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Consulta de validacion:

```sql
SELECT *
FROM GUIAS_COLA_PROCESADAS
ORDER BY ID DESC;
```

## Ejecucion local

### 1. Levantar RabbitMQ con Docker Compose

```bash
docker compose up -d
```

RabbitMQ Management:

```text
http://localhost:15672
```

### 2. Variables de entorno Oracle

En PowerShell:

```powershell
$env:ORACLE_WALLET_PATH="src/main/resources/wallet"
$env:ORACLE_TNS_NAME="guiass8db_high"
$env:ORACLE_DB_USERNAME="ADMIN"
$env:ORACLE_DB_PASSWORD="TU_PASSWORD_REAL"
```

### 3. Ejecutar Spring Boot

```bash
mvn spring-boot:run
```

### 4. Health check local

```http
GET http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP"
}
```

## Despliegue en EC2

El despliegue se realiza mediante GitHub Actions.

El workflow realiza:

1. Build de la imagen Docker.
2. Push de la imagen a Docker Hub.
3. Conexion SSH a EC2.
4. Instalacion o validacion de Docker.
5. Montaje de EFS.
6. Creacion de red Docker `guias-net`.
7. Levantamiento de RabbitMQ.
8. Levantamiento de la aplicacion Spring Boot.
9. Montaje del wallet Oracle en el contenedor.
10. Impresion de evidencias del despliegue.

Contenedores esperados en EC2:

```text
gestion-guias-app
rabbitmq-guias
```

Red Docker:

```text
guias-net
```

## Variables de entorno utilizadas en EC2

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_SESSION_TOKEN
AWS_REGION
S3_BUCKET_NAME
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
ORACLE_WALLET_PATH
ORACLE_TNS_NAME
ORACLE_DB_USERNAME
ORACLE_DB_PASSWORD
```

El wallet Oracle no se sube al repositorio. Se monta manualmente en EC2 desde:

```text
/home/ec2-user/oracle/wallet
```

hacia el contenedor en:

```text
/app/wallet
```

## Seguridad

El backend mantiene seguridad mediante Spring Security y valida tokens JWT emitidos por Azure AD B2C.

Los endpoints generales requieren rol:

```text
admin
```

El token debe enviarse como Bearer Token:

```http
Authorization: Bearer <TOKEN>
```

## Archivos principales agregados o modificados en S8

```text
docker-compose.yml
.github/workflows/main.yml
src/main/resources/application.properties
src/main/java/com/duoc/gestionguias/config/RabbitMQConfig.java
src/main/java/com/duoc/gestionguias/config/OracleJdbcConfig.java
src/main/java/com/duoc/gestionguias/dto/mensaje/GuiaDespachoMessage.java
src/main/java/com/duoc/gestionguias/model/GuiaColaProcesada.java
src/main/java/com/duoc/gestionguias/repository/GuiaColaProcesadaRepository.java
src/main/java/com/duoc/gestionguias/repository/oracle/GuiaColaProcesadaOracleRepository.java
src/main/java/com/duoc/gestionguias/service/messaging/GuiaQueueProducer.java
src/main/java/com/duoc/gestionguias/service/messaging/GuiaQueueService.java
src/main/java/com/duoc/gestionguias/service/messaging/GuiaQueueConsumerService.java
src/main/java/com/duoc/gestionguias/controller/GuiaController.java
pom.xml
```

## Evidencias esperadas

Para la entrega S8 se consideran las siguientes evidencias:

1. GitHub Actions ejecutado correctamente.
2. Contenedores `gestion-guias-app` y `rabbitmq-guias` activos en EC2.
3. Red Docker `guias-net` con ambos contenedores conectados.
4. RabbitMQ Management mostrando `guias.pendientes.queue`.
5. RabbitMQ Management mostrando `guias.errores.queue`.
6. Exchange `guias.exchange` creado.
7. Postman creando una guia mediante API Gateway.
8. Postman enviando guia a la cola principal.
9. RabbitMQ mostrando mensaje en cola principal.
10. Postman procesando mensaje desde la cola.
11. Oracle Cloud mostrando registro en `GUIAS_COLA_PROCESADAS`.
12. Postman enviando guia a la cola de errores.
13. RabbitMQ mostrando mensaje en cola de errores.
14. API Gateway stage `s8` con rutas nuevas.
15. Azure AD B2C emitiendo JWT valido para rol admin.

## Conclusion

En la Semana 8 se incorporo mensajeria asincrona al sistema de gestion de guias mediante RabbitMQ. La solucion implementa un flujo productor-consumidor, una cola principal para guias pendientes, una cola de errores para manejo de fallos y persistencia de mensajes procesados en Oracle Cloud Autonomous Database.

Ademas, el sistema fue desplegado en EC2 usando Docker y GitHub Actions, exponiendo los endpoints mediante AWS API Gateway y manteniendo la validacion de seguridad con Azure AD B2C y Spring Security.