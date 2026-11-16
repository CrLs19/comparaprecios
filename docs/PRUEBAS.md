# Pruebas y alcance de la evidencia

## Qué se ejecuta

| Bloque | Comprobación |
|---|---|
| Clasificación | Casos originales, cantidades inválidas, varios ítems/adjudicaciones y reclasificación |
| Modelos | Guiones, mayúsculas, repetición y modelos diferentes |
| Ajuste | Ejemplo histórico, mismo periodo, dato ausente e índices no positivos |
| Cliente HTTP | JSON válido, error 400 sin reintento, recuperación tras 500 y agotamiento de intentos |
| OECE | Paginación, arreglo results ausente y detalle sin compiledRelease |
| BCRP | Columnas identificadas por nombre, n.d., último periodo común y fecha malformada |
| Integración de la API | Búsqueda completa, tres estados, ajuste, guardado JPA e historial |
| Fallos en integración | BCRP no disponible, búsqueda OECE caída y detalle parcialmente fallido |
| Contratos | Filtros, detalle, ajuste directo y parámetros inválidos que devuelven HTTP 400 |

La clase `IntegracionApiTest` levanta el contexto Spring Boot, usa MockMvc y
conecta los componentes reales del proyecto. `MockRestServiceServer` sustituye
únicamente las respuestas HTTP de las fuentes externas. La base es H2 en memoria.
No se despliegan microservicios separados en estas pruebas.

## Correcciones hechas al comprobar el código

1. **CalculadoraAjuste:** rechaza IPC o tipo de cambio menores o iguales a cero,
   tanto en origen como en destino. Antes podía dividir entre cero o aceptar un
   índice negativo. Se conservan las fórmulas para datos válidos.
2. **BcrpAdapter:** un mes/año mal formado produce `ServicioExternoException`
   identificada como BCRP. Antes un año no numérico escapaba como
   `NumberFormatException` y podía confundirse con un parámetro inválido del usuario.
3. **ClasificadorComparabilidad:** el mensaje R2 ahora explica que la cantidad
   puede estar ausente, no ser positiva o representar un posible lote. Las condiciones
   de clasificación son las mismas; antes todos esos casos decían «igual a 1».
4. **Perfil PostgreSQL:** la clave se toma de `DB_PASSWORD`; usuario y URL también
   admiten variables de entorno. Ya no se deja una contraseña fija en el código.
5. **Pruebas portables:** se usa el generador de mocks por subclases, sin adjuntar
   dinámicamente un agente a la JVM. Estas pruebas no sustituyen clases finales.

## Evidencia incluida

`RESULTADOS.md` resume la ejecución efectuada sobre este paquete. La carpeta
`evidencias/` contiene el registro de Maven, reportes de JUnit, reporte JaCoCo,
JSON generados durante las pruebas y una comprobación HTTP de la aplicación real. Las respuestas sintéticas están
identificadas como tales y usan dominios `.test`.

El número de casos cuenta cada combinación de una prueba parametrizada como
una ejecución. La cobertura JaCoCo mide código ejecutado durante estas pruebas;
**no equivale al porcentaje funcional del proyecto ni al 75 % solicitado para el video**.

## Comprobación manual en línea

1. Ejecutar la aplicación con `mvnw.cmd spring-boot:run`.
2. Importar `ComparaPrecios.postman_collection.json` en Postman.
3. Probar historial y parámetro inválido para confirmar que la API responde.
4. Probar ajuste BCRP, detalle OECE y búsqueda completa. Guardar fecha, URL,
   parámetros, código HTTP y respuesta reales. La búsqueda puede tardar por la
   paginación y los detalles de OECE.
5. Capturar también un caso en revisión y uno descartado, explicando sus motivos.

Los registros locales de una ejecución exitosa no garantizan disponibilidad
futura de las fuentes. Un error 503 en una consulta real debe registrarse y
analizarse; no reemplazarlo por una respuesta simulada presentada como real.

## Pendientes que estas pruebas no resuelven

- Publicar el repositorio y verificar una ejecución real de GitHub Actions.
- Probar PostgreSQL y la conservación de consultas tras reiniciar.
- Mostrar el video con las funcionalidades reales de la versión entregada.
- Verificar moneda y unidad antes de afirmar comparabilidad completa. El modelo
  actual no conserva la moneda de adjudicación ni descarta todas las unidades ambiguas.
- Resolver completamente la correspondencia entre adjudicación e ítems: el
  adaptador toma la primera adjudicación y la regla R5 envía los casos múltiples a revisión.
- Informar cuando una búsqueda se trunca por el máximo de páginas.
- El detalle individual solo registra en el log algunas fallas de ajuste; aún
  conviene devolver una advertencia al consumidor y manejar periodos de origen faltantes.

## Ajustes para el informe

El `pom.xml` del proyecto recibido declara **Java 17 y Spring Boot 3.3.5**.
No corresponde presentar Java 26 o Spring Boot 4.1 como las versiones de este ZIP.
El código sí evita reintentar errores HTTP 4xx; los casos del cliente HTTP lo comprueban.
Los 10 tests originales se conservan y el resultado final se indica en `RESULTADOS.md`.

Referencia de cobertura: https://www.jacoco.org/jacoco/trunk/doc/maven.html
