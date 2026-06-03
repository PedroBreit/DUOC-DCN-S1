# Gestión de Guías de Despacho

Microservicio desarrollado con **Spring Boot**, **Docker**, **GitHub Actions**, **EC2**, **S3** y **EFS**.

La aplicación permite crear, consultar, actualizar, descargar, eliminar y subir guías de despacho. Las guías se generan temporalmente en **EFS** y luego se almacenan en **S3** organizadas por fecha y transportista.

---

## 1. Variables de entorno y secrets necesarios

El proyecto usa variables configuradas como **GitHub Secrets** para que GitHub Actions pueda construir, desplegar y conectar la aplicación con AWS.

Los secrets se configuran en:

```text
GitHub > Repositorio > Settings > Secrets and variables > Actions > New repository secret
```

### Secrets de Docker Hub

| Secret               | Para qué sirve                                             | Dónde conseguirlo                                                 |
| -------------------- | ---------------------------------------------------------- | ----------------------------------------------------------------- |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub para publicar la imagen              | Docker Hub > Account Settings                                     |
| `DOCKERHUB_TOKEN`    | Token para que GitHub Actions pueda subir la imagen Docker | Docker Hub > Account Settings > Security > Personal Access Tokens |

---

### Secrets de EC2

| Secret        | Para qué sirve                                           | Dónde conseguirlo                                                     |
| ------------- | -------------------------------------------------------- | --------------------------------------------------------------------- |
| `EC2_HOST`    | IP pública o Elastic IP de la instancia EC2              | AWS > EC2 > Instances > seleccionar instancia > Public IPv4 address   |
| `EC2_USER`    | Usuario SSH de Amazon Linux                              | Usar `ec2-user`                                                       |
| `EC2_SSH_KEY` | Llave privada para que GitHub Actions se conecte por SSH | Contenido completo del archivo `.pem` descargado al crear la Key Pair |

Ejemplo:

```text
EC2_USER=ec2-user
EC2_HOST=54.156.56.18
```

El secret `EC2_SSH_KEY` debe contener todo el contenido del archivo `.pem`, incluyendo:

```text
-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----
```

o:

```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

No subir el archivo `.pem` al repositorio.

---

### Secrets de AWS Academy

| Secret                  | Para qué sirve                               | Dónde conseguirlo         |
| ----------------------- | -------------------------------------------- | ------------------------- |
| `AWS_ACCESS_KEY_ID`     | Access Key temporal de AWS Academy           | Learner Lab > AWS Details |
| `AWS_SECRET_ACCESS_KEY` | Secret Key temporal de AWS Academy           | Learner Lab > AWS Details |
| `AWS_SESSION_TOKEN`     | Token temporal de sesión de AWS Academy      | Learner Lab > AWS Details |
| `AWS_REGION`            | Región donde están los servicios AWS         | Normalmente `us-east-1`   |
| `S3_BUCKET_NAME`        | Nombre del bucket S3 usado por la aplicación | AWS > S3 > Buckets        |
| `EFS_ID`                | ID del sistema de archivos EFS               | AWS > EFS > File systems  |

Ejemplo:

```text
AWS_REGION=us-east-1
S3_BUCKET_NAME=gestion-guias-s3
EFS_ID=fs-082248910a6a31227
```

Importante: las credenciales de AWS Academy son temporales. Si el laboratorio se detiene o reinicia, se deben actualizar estos secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_SESSION_TOKEN
```

---

## 2. Crear instancia EC2

Ingresar a AWS Academy:

```text
Learner Lab > Start Lab > AWS
```

Luego ir a:

```text
EC2 > Instances > Launch instances
```

Configuración recomendada:

```text
Name: gestion-guias
AMI: Amazon Linux 2023
Instance type: t2.micro o t3.micro
Key pair: crear nueva llave .pem
Storage: 8 GiB
```

### Security Group de EC2

Agregar reglas de entrada:

| Tipo       | Puerto | Origen    |
| ---------- | -----: | --------- |
| SSH        |     22 | 0.0.0.0/0 |
| Custom TCP |   8080 | 0.0.0.0/0 |

El puerto `22` permite la conexión SSH desde GitHub Actions hacia EC2.

El puerto `8080` permite acceder a la API desde Postman o navegador.

---

## 3. Crear Elastic IP

Para evitar que cambie la IP pública de la instancia, crear una Elastic IP.

Ruta:

```text
EC2 > Elastic IPs > Allocate Elastic IP address
```

Luego asociarla a la instancia:

```text
Actions > Associate Elastic IP address
Resource type: Instance
Instance: gestion-guias
```

Guardar la IP pública y configurarla en GitHub como:

```text
EC2_HOST
```

Si se cambia la Elastic IP, se debe actualizar el secret:

```text
EC2_HOST
```

---

## 4. Crear bucket S3

Ir a:

```text
S3 > Buckets > Create bucket
```

Configuración usada:

```text
Bucket name: gestion-guias-s3
Region: us-east-1
Object Ownership: ACLs disabled
Block Public Access: según indicación del docente
Versioning: Disabled
Encryption: SSE-S3
```

El nombre del bucket debe ser único. Si `gestion-guias-s3` no está disponible, usar otro nombre.

Ejemplo:

```text
gestion-guias-s3-pedro
```

Luego configurar el nombre en GitHub Secrets:

```text
S3_BUCKET_NAME
```

Si se cambia el nombre del bucket, también se debe actualizar:

```text
GitHub Secret: S3_BUCKET_NAME
```

Y, si se desea cambiar el valor por defecto del código, modificar:

```text
src/main/resources/application.properties
```

Propiedad:

```properties
aws.s3.bucket-name=${S3_BUCKET_NAME:gestion-guias-s3}
```

---

## 5. Crear EFS

Ir a:

```text
EFS > File systems > Create file system
```

Configuración recomendada:

```text
Name: gestion-guias-efs
VPC: misma VPC de la EC2
Performance mode: General Purpose
Throughput mode: Bursting
```

Después de crear el EFS, guardar el ID.

Ejemplo:

```text
fs-082248910a6a31227
```

Este valor se debe configurar en GitHub como:

```text
EFS_ID
```

Si se crea otro EFS, se debe actualizar el secret:

```text
EFS_ID
```

---

## 6. Configurar red de EFS

Entrar al EFS creado:

```text
EFS > gestion-guias-efs > Network
```

Verificar:

```text
Mount target state: Available
Availability Zone: misma zona donde está la EC2
```

Luego revisar el Security Group asociado al EFS y agregar una regla de entrada:

| Tipo | Puerto | Origen                |
| ---- | -----: | --------------------- |
| NFS  |   2049 | Security Group de EC2 |

Para laboratorio, si no permite seleccionar el Security Group como origen, se puede usar:

```text
0.0.0.0/0
```

El puerto `2049` permite que EC2 monte el sistema de archivos EFS.

---

## 7. Montaje de EFS

El montaje de EFS se realiza automáticamente desde GitHub Actions.

En el archivo:

```text
.github/workflows/main.yml
```

Se monta EFS en EC2 con:

```bash
sudo mount -t efs -o tls ${{ secrets.EFS_ID }}:/ /mnt/efs
```

Luego se crea la carpeta:

```bash
sudo mkdir -p /mnt/efs/guias
```

Y se conecta al contenedor Docker con:

```bash
-v /mnt/efs/guias:/app/storage/guias
```

Esto significa:

```text
EC2:        /mnt/efs/guias
Contenedor: /app/storage/guias
```

La aplicación genera los archivos en:

```text
/app/storage/guias
```

Pero realmente quedan almacenados en EFS.

---

## 8. Variables que recibe el contenedor Docker

En el workflow `main.yml`, el contenedor se ejecuta con estas variables:

```bash
-e AWS_ACCESS_KEY_ID="${{ secrets.AWS_ACCESS_KEY_ID }}"
-e AWS_SECRET_ACCESS_KEY="${{ secrets.AWS_SECRET_ACCESS_KEY }}"
-e AWS_SESSION_TOKEN="${{ secrets.AWS_SESSION_TOKEN }}"
-e AWS_REGION="${{ secrets.AWS_REGION }}"
-e S3_BUCKET_NAME="${{ secrets.S3_BUCKET_NAME }}"
```

Estas variables permiten que la aplicación pueda conectarse a S3 usando las credenciales temporales de AWS Academy.

---

## 9. Dónde modificar si cambia algo

### Si cambia la IP de EC2

Actualizar en GitHub Secrets:

```text
EC2_HOST
```

También cambiar la URL usada en Postman.

---

### Si cambia el bucket S3

Actualizar en GitHub Secrets:

```text
S3_BUCKET_NAME
```

Opcionalmente modificar el valor por defecto en:

```text
src/main/resources/application.properties
```

```properties
aws.s3.bucket-name=${S3_BUCKET_NAME:gestion-guias-s3}
```

---

### Si cambia la región AWS

Actualizar en GitHub Secrets:

```text
AWS_REGION
```

Opcionalmente modificar el valor por defecto en:

```text
src/main/resources/application.properties
```

```properties
aws.region=${AWS_REGION:us-east-1}
```

---

### Si cambia el EFS

Actualizar en GitHub Secrets:

```text
EFS_ID
```

También revisar que el nuevo EFS tenga:

```text
Mount target disponible
Security Group con NFS 2049
Misma VPC que EC2
```

---

### Si cambia el usuario Docker Hub

Actualizar en GitHub Secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Y revisar en el workflow:

```text
.github/workflows/main.yml
```

La imagen se publica como:

```text
${{ secrets.DOCKERHUB_USERNAME }}/gestion-guias:latest
```

---

### Si cambia el nombre de la imagen Docker

Modificar en:

```text
.github/workflows/main.yml
```

Buscar:

```text
gestion-guias:latest
```

y reemplazarlo por el nuevo nombre.

---

### Si cambia la ruta temporal de archivos

Modificar en:

```text
src/main/resources/application.properties
```

```properties
app.storage.local-path=./storage/guias
```

También revisar el volumen Docker en:

```text
.github/workflows/main.yml
```

```bash
-v /mnt/efs/guias:/app/storage/guias
```

Ambas rutas deben coincidir con la ruta donde la aplicación escribe los archivos.

---

## 10. Endpoints principales

Reemplazar `IP_PUBLICA` por la Elastic IP de la instancia EC2.

### Crear guía

```http
POST http://IP_PUBLICA:8080/api/guias
```

### Listar guías

```http
GET http://IP_PUBLICA:8080/api/guias
```

### Buscar por ID

```http
GET http://IP_PUBLICA:8080/api/guias/1
```

### Buscar por transportista y fecha

```http
GET http://IP_PUBLICA:8080/api/guias/buscar?transportista=Transportes EFS&fecha=2026-06-03
```

### Subir guía a S3

```http
POST http://IP_PUBLICA:8080/api/guias/1/subir-s3
```

### Descargar guía

```http
GET http://IP_PUBLICA:8080/api/guias/1/descargar
```

### Actualizar guía

```http
PUT http://IP_PUBLICA:8080/api/guias/1
```

### Eliminar guía

```http
DELETE http://IP_PUBLICA:8080/api/guias/1
```

---

## 11. Dónde ver evidencias

### GitHub Actions

```text
GitHub > Actions > Build, Push and Deploy
```

Evidencias:

```text
Workflow en verde
Build de imagen Docker
Push a Docker Hub
Conexión SSH a EC2
Montaje de EFS
Docker run
Contenedor activo
```

---

### Docker Hub

```text
Docker Hub > Repositorio gestion-guias
```

Evidencia:

```text
Imagen latest publicada
```

---

### EC2

```text
EC2 > Instances > gestion-guias
```

Evidencias:

```text
Instancia Running
Elastic IP asociada
Security Group con puerto 8080
```

---

### EFS

```text
EFS > File systems > gestion-guias-efs
```

Evidencias:

```text
Estado Available
File system ID
Mount target Available
Security Group con regla NFS 2049
```

---

### S3

```text
S3 > Buckets > gestion-guias-s3
```

Evidencias:

```text
Carpeta por fecha
Carpeta por transportista
Archivo guia generado
```

Ejemplo:

```text
2026-06-03/Transportes_EFS/guia-1.txt
```

---

### Postman

Evidencias:

```text
Crear guia
Listar guias
Buscar por ID
Buscar por transportista y fecha
Subir guia a S3
Descargar guia
Actualizar guia
Eliminar guia
```
