# Resultado de las pruebas

## Ejecución local comprobada

- Fecha de finalización: **2026-11-16T16:36:31-05:00** (hora de Lima).
- JDK: **Zulu OpenJDK 17.0.13**; Maven **3.9.9**; Spring Boot **3.3.5**.
- Sistema: Linux x86_64. El script Windows se entrega, pero no se ejecutó en Windows.
- Operación: **Maven `clean verify`**. Se usó configuración de proxy propia del entorno
  para descargar dependencias; esa configuración no se incluye en el proyecto.
- Resultado: **BUILD SUCCESS**. **57 casos, 0 fallos, 0 errores y 0 omitidos**.
- Los 10 casos originales se ejecutaron primero y pasaron; se añadieron 47 ejecuciones.
- Las pruebas incluyen 17 casos de integración de API con componentes reales y HTTP externo simulado.

| Clase | Casos | Fallos | Errores |
|---|---:|---:|---:|
| IntegracionApiTest | 17 | 0 | 0 |
| ClienteHttpTest | 5 | 0 | 0 |
| BcrpAdapterResilienciaTest | 3 | 0 | 0 |
| OeceAdapterHttpTest | 3 | 0 | 0 |
| CalculadoraAjusteLimitesTest | 6 | 0 | 0 |
| CalculadoraAjusteTest | 2 | 0 | 0 |
| ClasificadorComparabilidadTest | 7 | 0 | 0 |
| ClasificadorLimitesTest | 7 | 0 | 0 |
| ExtractorModeloTest | 6 | 0 | 0 |
| OeceAdapterMapeoTest | 1 | 0 | 0 |
| **Total** | **57** | **0** | **0** |

## Cobertura de código

- Líneas: **510 de 544 (93.75 %)**.
- Ramas de decisión: **142 de 170 (83.53 %)**.
- Medición: JaCoCo 0.8.12 sobre las clases Java de producción, sin exclusiones añadidas.
- Reporte navegable: `evidencias/cobertura/index.html`.

Este porcentaje mide ejecución de código, no funcionalidad entregada ni cumplimiento
del video solicitado por el docente. No demuestra ausencia de defectos.

## Comprobaciones HTTP con la aplicación encendida

Después de empaquetar el JAR se ejecutó la aplicación en el puerto 18080 con H2.
Se hicieron solicitudes HTTP reales, sin MockMvc ni respuestas externas simuladas.
Las fuentes se alcanzaron mediante el proxy de red del entorno de ejecución.

| Solicitud | HTTP observado | Resultado observado |
|---|---:|---|
| Historial `/api/v1/consultas` | 200 | Lista vacía en la instancia recién iniciada |
| Búsqueda con `termino=ab` | 400 | Validación de longitud del término |
| Ajuste BCRP, 551.89, 2024-07 → 2026-06 | 200 | IPC: 582.36; TC: 499.38 |
| Detalle OECE `ocds-dgv273-seacev3-1034013` | 200 | Ate; 136 unidades; monto 75056.50; precio unitario 551.89 |

El detalle de Ate recibió ajuste con referencia 2026-10: IPC 588.44 y TC 504.67.
Son resultados observados en esta ejecución; no están fijados en las pruebas
para consultas futuras. Solicitudes, fecha y respuestas están en
`evidencias/http-aplicacion-real.json`. No se ejecutó una búsqueda exhaustiva
en línea ni se comprobó PostgreSQL.

## Relación con la versión probada

El manifiesto `evidencias/codigo-sha256.txt` identifica los archivos de código,
pruebas y configuración utilizados. SHA-256 del manifiesto:

`306601c60b64baa6808d8033597dd5c18ddcee9b3e18682af0b8c3e4a754668e`

Los reportes JUnit conservan resultados y salidas; se retiró únicamente el
bloque de propiedades del entorno. El registro completo de Maven está incluido.
Si se modifica el código, ejecutar nuevamente las pruebas y usar la nueva evidencia.

## GitHub y la entrega

Repositorio de destino: https://github.com/CrLs19/comparaprecios.
El workflow ejecuta las pruebas al publicar cambios. La evidencia de esta sección
es local; el estado y los reportes de cada ejecución remota se consultan en
https://github.com/CrLs19/comparaprecios/actions.

La publicación inicial reúne el proyecto existente y las pruebas añadidas.
No demuestra por sí sola aportes individuales de los cinco integrantes.

Para mostrar al docente: ejecutar `probar.cmd`, abrir el reporte JaCoCo y enseñar
un caso de integración y su prueba. Una vez publicado, conservar el enlace de
Actions y el commit de esa misma versión. Los pendientes técnicos están en `PRUEBAS.md`.
