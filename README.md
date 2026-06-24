# Gestión de Guías de Despacho - Actividad S6

Microservicio desarrollado con **Spring Boot**, **Spring Security**, **OAuth 2.0**, **Azure AD B2C**, **AWS API Gateway**, **Docker**, **GitHub Actions**, **Amazon EC2** y **Amazon S3**.

La aplicación permite gestionar guías de despacho para una empresa transportista. El sistema permite crear, consultar, actualizar, buscar, eliminar, generar, subir a S3 y descargar guías de despacho.

En esta versión se incorporó seguridad Cloud Native mediante **Azure AD B2C** como proveedor de identidad, **JWT Authorizer en AWS API Gateway** y validación de roles mediante **Spring Security** en el backend.

---

## 1. Arquitectura general

La solución utiliza la siguiente arquitectura:

```text
Postman / Cliente
        |
        v
AWS API Gateway
        |
        v
Backend Spring Boot en EC2 / Docker
        |
        v
Amazon S3

Azure AD B2C emite los JWT
API Gateway valida el JWT
Spring Security valida los roles admin / descarga
```

Componentes principales:

| Componente      | Descripción                                        |
| --------------- | -------------------------------------------------- |
| Spring Boot     | Backend principal del sistema                      |
| Spring Security | Validación de autenticación y autorización por rol |
| Azure AD B2C    | IDaaS encargado de emitir tokens JWT               |
| AWS API Gateway | Punto público de entrada para todos los endpoints  |
| Amazon EC2      | Servidor donde se ejecuta el contenedor Docker     |
| Docker          | Empaquetado y ejecución de la aplicación           |
| GitHub Actions  | Build y despliegue automático                      |
| Amazon S3       | Almacenamiento de guías generadas                  |
| Postman         | Pruebas de OAuth 2.0, JWT y endpoints              |

---

## 2. URL pública de la API

Todas las pruebas deben realizarse mediante la URL pública de **AWS API Gateway**:

```text
https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s6
```

Ejemplo:

```http
GET https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s6/api/guias
```

> Importante: Para la actividad S6, las pruebas no deben realizarse contra `localhost` ni directamente contra la IP pública de EC2. El consumo debe realizarse mediante API Gateway.

---

## 3. Seguridad implementada

La seguridad se implementó en dos niveles:

### 3.1 Seguridad en AWS API Gateway

AWS API Gateway utiliza un **JWT Authorizer** para validar que cada solicitud incluya un token JWT válido emitido por Azure AD B2C.

Configuración principal:

```text
Identity source: $request.header.Authorization
Issuer: https://guiasdespachopedro2026.b2clogin.com/tfp/fa27e159-24ef-467f-bb9d-8810394076da/b2c_1_guias_signin/v2.0
Audience: 8701953c-c686-499e-81b0-08114c8cabe2
```

Si una solicitud no contiene token o contiene un token inválido, API Gateway responde:

```text
401 Unauthorized
```

---

### 3.2 Seguridad en Spring Security

El backend Spring Boot valida el token JWT y aplica reglas de autorización según el claim personalizado:

```text
extension_guiaRole
```

Roles utilizados:

| Rol      | Permisos                                                                                   |
| -------- | ------------------------------------------------------------------------------------------ |
| admin    | Puede crear, listar, consultar, buscar, actualizar, eliminar, subir a S3 y descargar guías |
| descarga | Solo puede descargar guías                                                                 |

Reglas principales:

```text
/api/guias/*/descargar    -> admin o descarga
/api/guias/**             -> solo admin
```

Si el token es válido, pero el usuario no tiene permisos para el endpoint solicitado, el backend responde:

```text
403 Forbidden
```

---

## 4. Azure AD B2C

Se configuró Azure AD B2C como servicio IDaaS para autenticación y emisión de tokens JWT.

### 4.1 Tenant

```text
guiasdespachopedro2026.onmicrosoft.com
```

### 4.2 Aplicación registrada

```text
guias-despacho-s6
```

Application Client ID:

```text
8701953c-c686-499e-81b0-08114c8cabe2
```

### 4.3 User Flow

```text
B2C_1_guias_signin
```

### 4.4 Redirect URI

Se utilizó la siguiente URL para ejecutar el flujo de usuario e inspeccionar tokens:

```text
https://jwt.ms
```

### 4.5 Claim personalizado

Se creó el atributo personalizado:

```text
guiaRole
```

En el JWT se emite como:

```text
extension_guiaRole
```

Valores utilizados:

```text
admin
descarga
```

### 4.6 API expuesta y scope

En la aplicación `guias-despacho-s6` se configuró **Expose an API** y se creó el scope:

```text
guias_api
```

Scope completo:

```text
https://guiasdespachopedro2026.onmicrosoft.com/8701953c-c686-499e-81b0-08114c8cabe2/guias_api
```

Para OAuth 2.0 Client Credentials se utilizó:

```text
https://guiasdespachopedro2026.onmicrosoft.com/8701953c-c686-499e-81b0-08114c8cabe2/.default
```

### 4.7 Client Secret

Se creó un **Client Secret** en Azure para obtener tokens mediante OAuth 2.0 Client Credentials desde Postman.

> Por seguridad, el valor del secreto no debe subirse al repositorio ni mostrarse públicamente.

---

## 5. Configuración Spring Boot

### 5.1 Dependencias principales

El proyecto utiliza dependencias de Spring Security y OAuth2 Resource Server para validar JWT.

Dependencias relevantes:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

---

### 5.2 Configuración JWT

En `src/main/resources/application.properties` se configuró la URL de claves públicas de Azure AD B2C:

```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://guiasdespachopedro2026.b2clogin.com/tfp/fa27e159-24ef-467f-bb9d-8810394076da/b2c_1_guias_signin/discovery/v2.0/keys
app.security.claim-role=extension_guiaRole
```

---

## 6. Variables de entorno y GitHub Secrets

El proyecto utiliza variables configuradas como **GitHub Secrets** para permitir el build, despliegue y conexión con AWS.

Ruta:

```text
GitHub > Repositorio > Settings > Secrets and variables > Actions > New repository secret
```

---

### 6.1 Secrets de Docker Hub

| Secret             | Descripción                          |
| ------------------ | ------------------------------------ |
| DOCKERHUB_USERNAME | Usuario de Docker Hub                |
| DOCKERHUB_TOKEN    | Token para publicar la imagen Docker |

---

### 6.2 Secrets de EC2

| Secret      | Descripción                                         |
| ----------- | --------------------------------------------------- |
| EC2_HOST    | Elastic IP o IP pública de la instancia EC2         |
| EC2_USER    | Usuario SSH de la instancia. Normalmente `ec2-user` |
| EC2_SSH_KEY | Llave privada `.pem` usada para conexión SSH        |

Ejemplo:

```text
EC2_USER=ec2-user
EC2_HOST=54.156.56.18
```

El secret `EC2_SSH_KEY` debe contener el contenido completo de la llave privada:

```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

> No subir el archivo `.pem` al repositorio.

---

### 6.3 Secrets de AWS Academy

| Secret                | Descripción                          |
| --------------------- | ------------------------------------ |
| AWS_ACCESS_KEY_ID     | Access Key temporal de AWS Academy   |
| AWS_SECRET_ACCESS_KEY | Secret Key temporal de AWS Academy   |
| AWS_SESSION_TOKEN     | Token temporal de sesión AWS Academy |
| AWS_REGION            | Región AWS utilizada                 |
| S3_BUCKET_NAME        | Nombre del bucket S3                 |

Ejemplo:

```text
AWS_REGION=us-east-1
S3_BUCKET_NAME=gestion-guias-s3
```

> Importante: Las credenciales de AWS Academy son temporales. Si el laboratorio se detiene o reinicia, se deben actualizar los secrets `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`.

---

## 7. Amazon EC2

La aplicación se ejecuta en una instancia EC2 con Docker.

Configuración recomendada:

```text
AMI: Amazon Linux 2023
Instance type: t2.micro o t3.micro
Usuario SSH: ec2-user
Puerto SSH: 22
Puerto aplicación: 8080
```

Security Group recomendado:

| Tipo       | Puerto | Origen    |
| ---------- | -----: | --------- |
| SSH        |     22 | 0.0.0.0/0 |
| Custom TCP |   8080 | 0.0.0.0/0 |

> Aunque el backend se ejecuta en EC2, las pruebas funcionales de la actividad deben realizarse mediante API Gateway.

---

## 8. Amazon S3

Se configuró un bucket S3 para almacenar las guías generadas.

Bucket utilizado:

```text
gestion-guias-s3
```

El backend genera la guía y la sube a S3 mediante el endpoint:

```http
POST /api/guias/{id}/subir-s3
```

Endpoint de descarga:

```http
GET /api/guias/{id}/descargar
```

---

## 9. AWS API Gateway

Se configuró AWS API Gateway como punto público de entrada para todos los endpoints del backend.

URL base:

```text
https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s6
```

Rutas registradas:

| Método | Ruta                      | Descripción                      |
| ------ | ------------------------- | -------------------------------- |
| GET    | /api/guias                | Listar guías                     |
| POST   | /api/guias                | Crear guía                       |
| GET    | /api/guias/{id}           | Consultar guía por ID            |
| PUT    | /api/guias/{id}           | Actualizar guía                  |
| DELETE | /api/guias/{id}           | Eliminar guía                    |
| GET    | /api/guias/buscar         | Buscar por transportista y fecha |
| POST   | /api/guias/{id}/subir-s3  | Subir guía generada a S3         |
| GET    | /api/guias/{id}/descargar | Descargar guía                   |

Todas las rutas se encuentran protegidas con JWT Auth.

---

## 10. Endpoints principales

Todas las URLs deben usar la base:

```text
{{baseUrl}} = https://u42wjueljf.execute-api.us-east-1.amazonaws.com/s6
```

---

### 10.1 Crear guía

```http
POST {{baseUrl}}/api/guias
```

Body:

```json
{
  "transportista": "Transportes EFS",
  "fecha": "2026-06-03",
  "cliente": "Cliente Demo S6",
  "direccionDestino": "Av. Siempre Viva 742",
  "descripcionPedido": "Pedido de prueba para validacion final S6"
}
```

Rol requerido:

```text
admin
```

---

### 10.2 Listar guías

```http
GET {{baseUrl}}/api/guias
```

Rol requerido:

```text
admin
```

---

### 10.3 Consultar guía por ID

```http
GET {{baseUrl}}/api/guias/{id}
```

Rol requerido:

```text
admin
```

---

### 10.4 Buscar por transportista y fecha

```http
GET {{baseUrl}}/api/guias/buscar?transportista=Transportes%20EFS&fecha=2026-06-03
```

Rol requerido:

```text
admin
```

---

### 10.5 Actualizar guía

```http
PUT {{baseUrl}}/api/guias/{id}
```

Body:

```json
{
  "cliente": "Cliente Demo S6 Actualizado",
  "direccionDestino": "Av. Nueva 123",
  "descripcionPedido": "Pedido actualizado para evidencia S6",
  "estado": "ACTUALIZADA"
}
```

Rol requerido:

```text
admin
```

---

### 10.6 Subir guía generada a S3

```http
POST {{baseUrl}}/api/guias/{id}/subir-s3
```

Rol requerido:

```text
admin
```

---

### 10.7 Descargar guía

```http
GET {{baseUrl}}/api/guias/{id}/descargar
```

Roles permitidos:

```text
admin
descarga
```

---

### 10.8 Eliminar guía

```http
DELETE {{baseUrl}}/api/guias/{id}
```

Rol requerido:

```text
admin
```

---

## 11. Pruebas recomendadas en Postman

La colección de Postman se organiza de la siguiente forma:

```text
01 - OAuth2 e IDaaS Azure B2C
02 - Validación de seguridad API Gateway
03 - Endpoints con rol ADMIN
04 - Endpoints con rol DESCARGA
```

---

### 11.1 OAuth2 e IDaaS Azure B2C

| Prueba                                                             | Descripción                                                | Resultado esperado |
| ------------------------------------------------------------------ | ---------------------------------------------------------- | ------------------ |
| 01.01 - OAuth2 Client Credentials - Obtener JWT con Secret y Scope | Obtención de token usando Client ID, Client Secret y Scope | Token generado     |
| 01.02 - Bearer Token Usuario ADMIN - Acceso permitido              | Token con `extension_guiaRole=admin`                       | 200 OK             |
| 01.03 - Bearer Token Usuario DESCARGA - Acceso restringido         | Token con `extension_guiaRole=descarga`                    | 403 Forbidden      |

---

### 11.2 Validación de seguridad API Gateway

| Prueba                                  | Resultado esperado |
| --------------------------------------- | ------------------ |
| Sin JWT                                 | 401 Unauthorized   |
| JWT inválido                            | 401 Unauthorized   |
| JWT válido admin                        | 200 OK             |
| JWT válido descarga en endpoint general | 403 Forbidden      |

---

### 11.3 Endpoints con rol ADMIN

| Prueba                           | Método | Endpoint                  | Resultado esperado      |
| -------------------------------- | ------ | ------------------------- | ----------------------- |
| Crear guía                       | POST   | /api/guias                | 200 OK / 201 Created    |
| Listar guías                     | GET    | /api/guias                | 200 OK                  |
| Consultar por ID                 | GET    | /api/guias/{id}           | 200 OK                  |
| Buscar por transportista y fecha | GET    | /api/guias/buscar         | 200 OK                  |
| Actualizar guía                  | PUT    | /api/guias/{id}           | 200 OK                  |
| Subir guía a S3                  | POST   | /api/guias/{id}/subir-s3  | 200 OK                  |
| Descargar guía                   | GET    | /api/guias/{id}/descargar | 200 OK                  |
| Eliminar guía                    | DELETE | /api/guias/{id}           | 200 OK / 204 No Content |

---

### 11.4 Endpoints con rol DESCARGA

| Prueba                   | Método | Endpoint                  | Resultado esperado |
| ------------------------ | ------ | ------------------------- | ------------------ |
| Intentar listar guías    | GET    | /api/guias                | 403 Forbidden      |
| Intentar crear guía      | POST   | /api/guias                | 403 Forbidden      |
| Intentar actualizar guía | PUT    | /api/guias/{id}           | 403 Forbidden      |
| Intentar eliminar guía   | DELETE | /api/guias/{id}           | 403 Forbidden      |
| Descargar guía permitida | GET    | /api/guias/{id}/descargar | 200 OK             |

---

## 12. GitHub Actions

El proyecto utiliza GitHub Actions para automatizar el build y despliegue.

Flujo general:

```text
1. Checkout del repositorio
2. Login en Docker Hub
3. Build de imagen Docker
4. Push de imagen a Docker Hub
5. Conexión SSH a EC2
6. Detención de contenedor anterior
7. Descarga de la nueva imagen
8. Ejecución del contenedor actualizado
```

Evidencias esperadas:

```text
Workflow ejecutado correctamente
Imagen Docker publicada
Contenedor activo en EC2
Aplicación disponible mediante API Gateway
```

---

## 13. Evidencias recomendadas

### Azure AD B2C

```text
Tenant B2C
App Registration guias-despacho-s6
Application Client ID
Authentication con https://jwt.ms
User Flow B2C_1_guias_signin
Claim personalizado guiaRole
Usuarios con roles admin y descarga
Expose an API con scope guias_api
Client Secret creado
```

### AWS

```text
EC2 ejecutando el backend
S3 bucket gestion-guias-s3
API Gateway con URL pública
Routes creadas
Integrations configuradas
JWT Authorizer configurado
Rutas protegidas con JWT Auth
```

### Postman

```text
OAuth2 Client Credentials con Client Secret y Scope
Token generado mediante Get New Access Token
Llamada sin token con 401
Llamada con token inválido con 401
Llamada admin con 200
Llamada descarga en endpoint general con 403
Creación de guía
Subida de guía a S3
Descarga de guía
Validación de permisos por rol
```

---

## 14. Consideraciones importantes

* Las credenciales de AWS Academy son temporales y deben actualizarse si el laboratorio se reinicia.
* No se debe subir el archivo `.pem` al repositorio.
* No se debe exponer el valor del Client Secret.
* Las pruebas funcionales deben realizarse usando API Gateway.
* El usuario `admin` tiene acceso completo a la gestión de guías.
* El usuario `descarga` solo tiene permiso para descargar guías.
* Si una solicitud no tiene token, API Gateway responde `401 Unauthorized`.
* Si el token es válido, pero el rol no tiene permiso, Spring Security responde `403 Forbidden`.

---

## 15. Repositorio

Repositorio del proyecto:

```text
https://github.com/PedroBreit/DUOC-DCN-S1.git
```

---

## 16. Conclusión

La solución implementa una arquitectura Cloud Native completa, integrando backend Spring Boot, contenedores Docker, despliegue automatizado con GitHub Actions, ejecución en Amazon EC2, almacenamiento en Amazon S3, exposición mediante AWS API Gateway y seguridad mediante Azure AD B2C.

Además, se incorporó autorización por roles con Spring Security, permitiendo diferenciar las funcionalidades disponibles para usuarios administradores y usuarios con permiso exclusivo de descarga.
