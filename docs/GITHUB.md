# Publicar y trabajar con GitHub

## Obtener el proyecto desde Windows

Repositorio del grupo: https://github.com/CrLs19/comparaprecios

Abrir una terminal en la carpeta donde se guardará una nueva copia del proyecto:

```bash
git clone https://github.com/CrLs19/comparaprecios.git
cd comparaprecios
git config user.name "TU NOMBRE"
git config user.email "TU CORREO DE GITHUB"
```

Usar el nombre y correo reales del integrante que realizará los cambios.
El propietario debe invitar como colaboradores a los compañeros que necesiten
subir sus ramas. Si Git abre el inicio de sesión, completarlo en el navegador.
No pegar contraseñas ni tokens en el código o en el chat.

Si se trabajaba desde un ZIP, conservar esa carpeta hasta trasladar cualquier
cambio propio a la nueva copia. No volver a inicializar el repositorio clonado
ni usar `push --force`.

## Comprobar la ejecución automática

1. Abrir la pestaña **Actions** del repositorio.
2. Entrar a **Pruebas Java y cobertura** y esperar el resultado.
3. Revisar el paso de compilación y pruebas; debe terminar en `BUILD SUCCESS`.
4. Descargar el artefacto `pruebas-<commit>` y abrir `index.html` del reporte JaCoCo.
5. Guardar el enlace de la ejecución y el identificador completo del commit.

Para repetirla, abrir el workflow y usar **Run workflow**. Comprobar siempre
el resultado de la ejecución correspondiente al commit que se va a entregar.

## Aportes de los cinco integrantes

Cada integrante clona el mismo repositorio, crea una rama para una tarea real,
realiza su cambio y ejecuta las pruebas antes de proponerlo. Por ejemplo:

```powershell
git switch main
git pull --ff-only
git switch -c pruebas/apellido-reglas
# Editar y comprobar la tarea asignada
.\mvnw.cmd -B -ntp clean verify
git add .
git commit -m "Agregar caso de prueba de la regla revisada"
git push -u origin pruebas/apellido-reglas
```

Después abre un pull request hacia `main`. Otro compañero revisa el cambio,
comprueba Actions y lo integra. Las ramas y los commits deben representar trabajo
real; subir este paquete no demuestra por sí solo la participación de cinco personas.

Distribución posible (el grupo asigna nombres según el trabajo que sí realice):

| Tarea | Evidencia útil |
|---|---|
| Preparación y revisión del repositorio | README y primera publicación |
| Reglas de comparabilidad | Casos revisados y cambios necesarios |
| OECE y BCRP | Solicitudes reales y revisión de adaptadores |
| API y persistencia | Validaciones y comprobación de historial |
| Integración y entrega | Revisión de PR, reportes y video |

## Datos para el informe

- URL del repositorio y acceso del docente.
- Rama de entrega y commit: `git rev-parse HEAD`.
- Enlace de Actions que ejecutó ese mismo commit.
- Aporte y enlace de cada integrante.

La evidencia local incluida tiene fecha y hash del código. No reemplaza el enlace
de GitHub ni los aportes que el grupo debe realizar.

Referencia de configuración: https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven
