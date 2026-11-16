# ComparaPrecios Perú

Proyecto de Arquitectura Orientada al Servicio, UTP, sección 20076.
Consulta antecedentes de compra de tóner en OECE, aplica reglas de comparabilidad
y calcula referencias separadas de ajuste con IPC y tipo de cambio del BCRP.

[Repositorio](https://github.com/CrLs19/comparaprecios) ·
[Pruebas en GitHub Actions](https://github.com/CrLs19/comparaprecios/actions)

## Tecnología de esta entrega

- Java 17, Spring Boot 3.3.5 y Maven 3.9.9.
- Spring MVC, validación Jakarta y Spring Data JPA.
- H2 en memoria para desarrollo; perfil PostgreSQL configurable.
- Interfaz HTML y JavaScript.

Es una aplicación Spring Boot con módulos internos y dos fuentes externas.
Esta entrega no acredita microservicios desplegados de forma independiente.

## Ejecutar en Windows

Instalar un **JDK 17**, comprobar `java -version` y `javac -version` y abrir
PowerShell en la carpeta que contiene `pom.xml`:

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir http://localhost:8080. En Linux/macOS: `bash ./mvnw spring-boot:run`.
El wrapper descarga Maven; no se necesita instalarlo por separado. La primera
compilación necesita internet para descargar dependencias.

Si la terminal integrada de VS Code es **Git Bash**, usar:

```bash
bash ./mvnw spring-boot:run
```

### Si BCRP devuelve un error de certificado en Windows

Un mensaje `PKIX path building failed` indica que Java no pudo establecer una
cadena de confianza para el certificado recibido. El origen concreto requiere
revisar los certificados y la conexión del equipo. La aplicación conserva los
precios originales y avisa que no pudo ajustarlos.

Como comprobación local, detener la aplicación con `Ctrl+C` y ejecutar en Git Bash:

```bash
bash ./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE"
```

Esta opción usa las autoridades de confianza de Windows y mantiene la validación
HTTPS. Solo corresponde a Windows; no se configura en el workflow de Linux.
Volver a buscar en la página y comprobar tanto las columnas de ajuste como la
terminal. Su resultado en el equipo donde se reportó el error sigue pendiente.

## Ejecutar las pruebas

```powershell
.\mvnw.cmd -B -ntp clean verify
```

También se puede abrir `probar.cmd` en Windows. Mantiene la ventana abierta,
guarda el registro de ejecución y abre el reporte si las pruebas terminan bien.
En Linux/macOS: `bash ./mvnw -B -ntp clean verify`.

Resultados:

- `target/surefire-reports/`: casos ejecutados, fallos y errores.
- `target/site/jacoco/index.html`: cobertura de código.
- `target/pruebas-api/`: respuestas generadas por tres pruebas de integración.

Las pruebas automáticas usan respuestas HTTP simuladas y los JSON del ZIP
original. No consultan OECE ni BCRP en línea. El controlador, el negocio,
los adaptadores y la persistencia H2 sí se ejecutan en la prueba de integración.
Ver `docs/PRUEBAS.md` y `docs/RESULTADOS.md`.

## GitHub

`docs/GITHUB.md` explica cómo clonar el repositorio y trabajar por ramas.
El workflow `.github/workflows/pruebas.yml` ejecuta `clean verify` con Java 17
al recibir un push o un pull request, y conserva los reportes como artefactos.
El estado de cada ejecución se consulta en Actions. Los resultados locales
incluidos se identifican por separado en `docs/RESULTADOS.md`.

## API propia

| Método | Ruta | Propósito |
|---|---|---|
| GET | `/api/v1/antecedentes?termino=TONER&modelo=TK-3432&desde=2024&hasta=2026` | Buscar, clasificar y ajustar |
| GET | `/api/v1/antecedentes/{ocid}` | Consultar un proceso |
| GET | `/api/v1/ajustes?monto=551.89&origen=2024-07&destino=2026-06` | Obtener las dos referencias de ajuste |
| GET | `/api/v1/consultas` | Consultar las últimas 20 búsquedas |

La colección `docs/ComparaPrecios.postman_collection.json` sirve para comprobar
la aplicación encendida. Las consultas de búsqueda, detalle y ajuste requieren
acceso a las fuentes externas. Los ejemplos no garantizan disponibilidad actual.

## PostgreSQL (opcional)

Crear la base previamente y configurar las credenciales en la terminal:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/comparaprecios"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "TU_CLAVE_LOCAL"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

La contraseña no está incluida en el repositorio. H2 pierde su contenido al
cerrar la aplicación. Estas pruebas no validan PostgreSQL ni persistencia tras reinicio.

## Alcance de los resultados

Un caso utilizable permite dividir monto adjudicado entre cantidad. Los estados
revisión y descartado conservan sus motivos. Los ajustes IPC y TC son referencias
distintas: sus extremos no constituyen un intervalo de confianza ni una cotización.
Las limitaciones detectadas y las correcciones de esta revisión están en `docs/PRUEBAS.md`.
