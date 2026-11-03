<p align="center">
  <img src="Docs/img/banner.png" alt="Horarios UEB" width="100%">
</p>

<p align="center">
  <a href="https://github.com/Chocax/horarios-ueb-sistema/actions/workflows/pruebas.yml"><img src="https://github.com/Chocax/horarios-ueb-sistema/actions/workflows/pruebas.yml/badge.svg?branch=main" alt="Pruebas"></a>
  <img src="https://img.shields.io/badge/Angular-21-dd0031?logo=angular&logoColor=white" alt="Angular 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-6db33f?logo=springboot&logoColor=white" alt="Spring Boot 4.1">
  <img src="https://img.shields.io/badge/Java-17-ea2d2e?logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/MySQL-8-4479a1?logo=mysql&logoColor=white" alt="MySQL 8">
  <img src="https://img.shields.io/badge/Bruno-67%2F67-f4aa41?logo=bruno&logoColor=white" alt="Bruno 67/67">
  <img src="https://img.shields.io/badge/front-Vercel-000000?logo=vercel&logoColor=white" alt="Vercel">
  <img src="https://img.shields.io/badge/back-Render-46e3b7?logo=render&logoColor=black" alt="Render">
</p>

<p align="center">
  <a href="https://horarios-ueb-sistema.vercel.app"><b>Abrir la aplicación</b></a> ·
  <a href="#recorrido-por-la-aplicación">Capturas</a> ·
  <a href="#cómo-funciona">Cómo funciona</a> ·
  <a href="#el-algoritmo-voraz">Algoritmo voraz</a> ·
  <a href="#pruebas">Pruebas</a> ·
  <a href="#correr-el-proyecto-en-local">Correr en local</a>
</p>

# Horarios UEB

Cada semestre, la coordinación del Programa de Ingeniería de Sistemas de la Universidad El Bosque arma a mano el horario
de 161 grupos con 55 docentes y 86 aulas, y los choques (un docente en dos salones, un aula con dos clases, un grupo
encima del almuerzo) aparecen cuando el horario ya está publicado.

**Horarios UEB** lo resuelve en cuatro pasos:

1. **Genera** una propuesta completa en menos de un segundo con un algoritmo voraz que respeta las nueve reglas duras.
2. **Ajusta**: la coordinación arrastra sesiones en un tablero semanal y el servidor valida cada movimiento al instante.
3. **Revisa**: el comité deja su concepto sobre la versión.
4. **Publica**: cada docente ve su horario y registra su disponibilidad y su horario de atención.

> Proyecto Núcleo II · Grupo 7 · Hernando Javier García Mogollón, Jhonatan Lara Gómez, Juanita Mejía Pineda,
> Laura Gabriela Díaz Amaya y Santiago Pinzón Vásquez.

## Recorrido por la aplicación

### Coordinación

<table>
  <tr>
    <td width="50%"><img src="Docs/img/app/inicio.jpg" alt="Inicio"><br><b>Inicio.</b> Estado del periodo, plan de estudios y el flujo de la planeación a la publicación.</td>
    <td width="50%"><img src="Docs/img/app/tablero.jpg" alt="Tablero"><br><b>Tablero del horario.</b> Arrastrar y soltar con validación R1–R9 en el servidor; 0 pendientes y 0 violaciones.</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/versiones.jpg" alt="Versiones"><br><b>Versiones y publicación.</b> La publicación se bloquea si hay pendientes o violaciones.</td>
    <td><img src="Docs/img/app/grupos.jpg" alt="Grupos"><br><b>Grupos y demanda.</b> Sugiere el número de grupos con el cupo efectivo (40 + 10 %).</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/docentes.jpg" alt="Docentes"><br><b>Docentes.</b> Vinculación, horas máximas, cursos habilitados y carga de la versión.</td>
    <td><img src="Docs/img/app/aulas.jpg" alt="Aulas"><br><b>Aulas.</b> 86 aulas por bloque, con capacidad, características y ocupación.</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/importar.jpg" alt="Importar"><br><b>Importar datos.</b> CSV todo o nada: si una fila falla, no se carga ninguna.</td>
    <td><img src="Docs/img/app/reportes.jpg" alt="Reportes"><br><b>Reportes.</b> Ocupación de aulas por franja, carga docente y grupos bajo el mínimo.</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/inscritos.jpg" alt="Inscritos"><br><b>Inscritos y cierre.</b> Cierra los grupos con menos de 10 estudiantes y libera docente y aula.</td>
    <td><img src="Docs/img/app/bitacora.jpg" alt="Bitácora"><br><b>Bitácora.</b> Cada acción, incluidos los intentos rechazados, con usuario, rol y fecha.</td>
  </tr>
</table>

### Comité, docentes y administración

<table>
  <tr>
    <td width="50%"><img src="Docs/img/app/revision.jpg" alt="Revisión"><br><b>Revisión de propuestas.</b> El comité ve la versión y registra su concepto; no puede modificarla.</td>
    <td width="50%"><img src="Docs/img/app/consulta.jpg" alt="Consulta"><br><b>Consultar horario.</b> Por semestre, docente, aula o curso; exporta a CSV y PDF.</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/mi-horario.jpg" alt="Mi horario"><br><b>Mi horario (docente).</b> Solo sus sesiones publicadas y su horario de atención.</td>
    <td><img src="Docs/img/app/mi-disponibilidad.jpg" alt="Mi disponibilidad"><br><b>Mi disponibilidad.</b> Marca las horas en que puede dictar y su atención semanal.</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/usuarios.jpg" alt="Usuarios"><br><b>Usuarios y roles.</b> Alta, cambio de rol y desactivación con efecto inmediato.</td>
    <td><img src="Docs/img/app/propuestas.jpg" alt="Propuestas"><br><b>Propuestas (administrador).</b> Borra las versiones de prueba como si nunca hubieran existido.</td>
  </tr>
  <tr>
    <td><img src="Docs/img/app/login.jpg" alt="Inicio de sesión"><br><b>Inicio de sesión</b> con correo institucional.</td>
    <td><img src="Docs/img/app/pantalla-carga.png" alt="Pantalla de carga"><br><b>Pantalla de carga</b> mientras el servidor gratuito despierta.</td>
  </tr>
</table>

## Cómo funciona

```mermaid
flowchart LR
    A[Configurar periodo<br>franjas y cupos] --> B[Catálogo<br>docentes · cursos · aulas]
    B --> C[Grupos<br>según la demanda]
    C --> D[Generar propuesta<br>algoritmo voraz]
    D --> E[Ajustar en el tablero<br>validación R1–R9]
    E --> F[Concepto del comité]
    F --> G{¿0 pendientes y<br>0 violaciones?}
    G -- sí --> H[Publicar]
    G -- no --> E
    H --> I[Docentes consultan<br>Inscritos y cierre]
```

Cada **versión** del horario pasa por estos estados: *borrador → en revisión → publicada*. Una versión publicada no se
edita: para cambiarla se **restaura** como borrador nuevo y se vuelve a publicar. Toda acción queda en la bitácora.

| Rol | Qué hace |
|---|---|
| **Coordinación** | Configura el periodo, el catálogo y los grupos; genera, ajusta y publica el horario |
| **Comité** | Revisa cada versión y registra su concepto (favorable o con ajustes) |
| **Docente** | Consulta su horario publicado y registra su disponibilidad y su horario de atención |
| **Administración** | Gestiona usuarios y roles y borra las propuestas de prueba |

### Reglas duras (bloquean)

Se validan en el servidor en cada movimiento, al generar y al publicar ([`ReglasDuras.java`](Backend/horarios-ueb-back/src/main/java/co/edu/unbosque/horariosuebback/domain/reglas/ReglasDuras.java), patrón *Specification*).

| Regla | Qué exige |
|---|---|
| **R1** | Un docente no puede tener dos sesiones a la vez |
| **R2** | Un aula no puede tener dos sesiones a la vez |
| **R3** | Dentro de la franja hábil y fuera de los bloques no asignables (almuerzo 13:00–14:00) |
| **R4** | Aula con capacidad y características suficientes (por ejemplo, sala de cómputo) |
| **R5** | Docente habilitado para el curso |
| **R6** | Respeta la disponibilidad del docente |
| **R7** | No supera las horas máximas del docente |
| **R8** | Sesiones semanales máximas del grupo y una sola sesión por día |
| **R9** | Cupo del grupo dentro del máximo más la tolerancia (40 + 10 %) |

### Restricciones blandas (avisan, no bloquean)

| Regla | Qué señala |
|---|---|
| **B1** | Un grupo con sesiones en días consecutivos |
| **B2** | Huecos del docente entre clases |
| **B3** | Carga desbalanceada entre docentes |
| **B4** | Cruces de cursos del mismo semestre y jornada |
| **B5** | Aula especial ocupada por un curso que no necesita sus características |
| **B6** | Dos grupos diurnos del mismo curso con el mismo docente |

## El algoritmo voraz

[`GeneradorVoraz.java`](Backend/horarios-ueb-back/src/main/java/co/edu/unbosque/horariosuebback/domain/generador/GeneradorVoraz.java)
arma el horario de un periodo completo en menos de un segundo (2026-2: 295 sesiones, 0 pendientes, 0 violaciones).
Es *voraz* porque toma, grupo por grupo, la mejor opción disponible en ese momento y no la reconsidera.

```mermaid
flowchart TD
    A[Ordenar grupos:<br>primero los de docente acordado,<br>luego los de menos opciones docente × aula] --> B[Tomar el siguiente grupo]
    B --> C[Armar candidatos «días + misma hora»<br>L-X, M-J, X-V, L-X-V…]
    C --> D[Calcular el costo de cada candidato]
    D --> E[Probar del más barato al más caro<br>con el docente menos cargado<br>y el aula menos usada]
    E --> F{¿El validador R1–R9<br>lo aprueba?}
    F -- sí --> G[Asignar las sesiones]
    F -- no, ningún patrón cabe --> H[Probar sesión por sesión]
    H -- tampoco --> I[Grupo PENDIENTE<br>con su causa]
    G --> B
    H -- cabe --> G
```

El **costo** de un candidato suma lo que la coordinación quiere evitar:

| Componente | Peso | Por qué |
|---|---|---|
| Patrón de días | 0 habituales (L-X, M-J, X-V, L-X-V) · 4 aceptables (L-J, M-V) · 12 poco habituales | Así se programan las clases en la práctica |
| Días seguidos (B1) | 6 por par de días consecutivos | Evita clases de un mismo grupo en días contiguos |
| Sábado en jornada diurna | 80 | Último recurso, más caro que un cruce del semestre |
| Sábado en jornada nocturna | 3 | Es parte normal de la jornada nocturna |
| Cruce con otro curso del semestre (B4) | alto, pero permitido | Solo si no hay otra opción |
| Desbalance de la semana | 8 por sesión | Para que el viernes no quede vacío |
| Hora menos deseada | variable | Prefiere las franjas centrales |

Las aulas se eligen entre las que no tienen características sobrantes (B5) y, entre ellas, las **menos usadas**, para
repartir la ocupación: con los datos reales, las 86 aulas tienen clases. En la jornada diurna el algoritmo prefiere
docentes distintos para los grupos de un mismo curso (B6).

## Arquitectura

```mermaid
flowchart LR
    U[Navegador] -->|HTTPS| V[Vercel<br>Angular 21]
    V -->|API REST + JWT| R[Render · Docker<br>Spring Boot 4.1 · Java 17]
    R -->|JDBC + TLS| A[(Aiven<br>MySQL 8)]
    GH[GitHub Actions] -. pruebas en cada push .-> R
```

| Capa | Tecnología | Dónde corre |
|---|---|---|
| Cliente web | Angular 21 (componentes *standalone* y *signals*), TypeScript | Vercel (plan gratuito) |
| API y dominio | Spring Boot 4.1, Spring Security 7, JWT HS256, BCrypt, Hibernate 7 | Render, contenedor Docker (plan gratuito) |
| Datos | MySQL 8, migraciones Flyway V1–V4 (H2 en modo MySQL en local) | Aiven (plan gratuito) |
| Gestión y calidad | Jira (Kanban), Bruno, JUnit 5, GitHub Actions | Nube |

| Carpeta | Contenido |
|---|---|
| `Frontend/horarios-ueb` | Aplicación Angular ([README](Frontend/horarios-ueb/README.md)) · `Frontend/design/DESIGN.md` guía visual |
| `Backend/horarios-ueb-back` | API Spring Boot (`co.edu.unbosque.horariosuebback`) y la colección de Bruno en `bruno/` |
| `BD/migraciones` | Scripts SQL de Flyway: esquema, datos semilla y horario 2026-1 ([README](BD/README.md)) |
| `Pruebas` | Guía de pruebas, catálogo de casos, prueba de carga y evidencias ([README](Pruebas/README.md)) |
| `Docs` | Enunciado, plan de estudios, entregas de la materia y plan de pruebas |
| `Extras/muestras-csv` | Archivos CSV de ejemplo (válidos y con errores) para probar *Importar datos* |

<details>
<summary><b>Paquetes del backend</b></summary>

| Paquete | Qué contiene |
|---|---|
| `entity` (+ `enums`) | Entidades JPA con Lombok |
| `repository` | Repositorios Spring Data JPA |
| `domain.reglas` | Reglas duras R1–R9 (patrón Specification) y `ValidadorReglas` |
| `domain.generador` | `GeneradorHorario` (interfaz), `GeneradorVoraz` y `Ocupacion` (índices de ocupación) |
| `service` | Periodos, catálogo, grupos, horario y versiones, atención, usuarios, bitácora |
| `controller` | API REST bajo `/api` con permisos por rol (`@PreAuthorize`) |
| `security`, `config` | JWT, BCrypt, CORS, cabeceras, freno a fuerza bruta |
| `dto`, `exception` | Objetos de la API y manejo de errores en español |

</details>

### Seguridad

- Contraseñas con **BCrypt**; nunca salen del backend.
- Sesión con **token JWT** (8 h) firmado con un secreto que solo existe como variable de entorno (`JWT_SECRET`).
- **Permisos en el servidor**: cada endpoint exige su rol; el docente solo recibe su propio horario; los intentos
  rechazados quedan en la bitácora. Desactivar un usuario o cambiarle el rol tiene efecto inmediato.
- Bloqueo de 15 minutos tras 5 intentos fallidos con el mismo correo.
- CORS limitado a la URL del front, CSP y cabeceras de seguridad, sin trazas de error expuestas.
- Las reglas se validan en el servidor antes de guardar, y la base tiene restricciones `UNIQUE`/`CHECK` como segunda barrera.

## Pruebas

El plan sigue **ISO/IEC/IEEE 29119** (con la estructura de IEEE 829) e **ISO/IEC 25010**. Hay **38 casos de prueba**,
cada uno registrado en Jira como subtarea de la historia de usuario que verifica.

| Tipo | Herramienta | Resultado |
|---|---|---|
| Unitarias y de dominio | JUnit 5 + Spring Boot Test (H2 con el esquema real) | 7/7 |
| Integración de la API y seguridad | [Bruno](Backend/horarios-ueb-back/bruno) · 39 peticiones | 67/67 verificaciones |
| Rendimiento | [`carga.mjs`](Pruebas/rendimiento/carga.mjs) · 300 peticiones, 25 simultáneas | 0 errores · p95 = 325 ms |
| Regresión | GitHub Actions en cada push | Automática |
| Usabilidad | Heurísticas de Nielsen | Manual |

### Integración continua con GitHub Actions

Cada push o pull request a `develop` y `main` ejecuta [`.github/workflows/pruebas.yml`](.github/workflows/pruebas.yml):

```mermaid
flowchart LR
    P[push / pull request] --> J[JUnit<br>reglas, generador, seguridad]
    J --> B[Levantar backend<br>perfil local con H2]
    B --> C[Colección de Bruno<br>39 peticiones]
    P --> F[Compilar el frontend]
    C --> R[Reportes como artefacto]
```

- La colección inicia sesión con cada rol, así que necesita el secreto **`CLAVE_PRUEBAS`** (*Settings → Secrets and
  variables → Actions*) con la contraseña inicial. Si falta, el paso de Bruno se omite con un aviso.
- Los reportes de JUnit y de Bruno se descargan como artefacto `reportes-pruebas` en la pestaña **Actions**.
- Si una prueba falla, el commit queda con ❌ y no conviene integrarlo a `main`.

<p align="center"><img src="Docs/img/app/pruebas-bruno.png" alt="Reporte de Bruno" width="80%"><br><sub>Reporte de Bruno: 39 peticiones y 67 verificaciones aprobadas.</sub></p>

La guía para usar Bruno (instalar, elegir ambiente, correr y crear pruebas) está en [Pruebas/README.md](Pruebas/README.md).

### Defectos encontrados

| Defecto | Severidad | Qué pasaba | Corrección |
|---|---|---|---|
| BUG-01 | Crítica | En producción nadie podía iniciar sesión: el front apuntaba a una URL equivocada de Render | URL y CSP corregidas |
| BUG-02 | Alta | Cursos con software programados en salones sin computadores | Migración V4: sala de cómputo |
| BUG-03 | Alta | Fundamentos de programación con 2 sesiones semanales en lugar de 3 | Datos semilla corregidos |
| BUG-04 | Media | Grupos diurnos con clases el sábado | Sábado diurno como último recurso (costo 80) |
| BUG-05 | Media | Ocupación de aulas desbalanceada | Aulas menos usadas primero: 86/86 con clases |
| BUG-06 | Media | Viernes casi vacíos | Patrones X-V, L-J, M-V y balance de la semana |
| BUG-07 | Media | El tablero tardaba en confirmar el arrastre | Actualización optimista |
| BUG-08 | Baja | Sin indicación de carga en operaciones largas | Pantalla de carga y barra de progreso |
| BUG-09 | Alta | Estructuras de datos quedaba pendiente por falta de horas docentes | Más docentes habilitados |
| BUG-10 | Baja | 60–70 ms de latencia entre Render (Virginia) y Aiven (San Francisco) | Diferido |

## Requerimientos y casos de uso

Cada requerimiento tiene su historia de usuario en Jira (proyecto HDC) y al menos un caso de prueba.

| RF | Requerimiento | CU | Historia |
|---|---|---|---|
| RF-01 | Configurar franjas, bloques no asignables y duración de clase | CU-03 | [HDC-7](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-7) |
| RF-02 | Configurar parámetros de aforo y frecuencia | CU-03 | [HDC-8](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-8) |
| RF-03 | Registrar docentes con vinculación y horas contratadas | CU-04 | [HDC-10](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-10) |
| RF-04 | Registrar disponibilidad y competencias del docente | CU-05 | [HDC-11](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-11) |
| RF-05 | Registrar cursos con intensidad y requisitos de aula | CU-06 | [HDC-12](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-12) |
| RF-06 | Registrar aulas con capacidad y características | CU-06 | [HDC-13](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-13) |
| RF-07 | Crear grupos (secciones) por periodo académico | CU-08 | [HDC-14](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-14) |
| RF-08 | Generar la propuesta inicial con el algoritmo voraz | CU-10 | [HDC-16](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-16) |
| RF-09 | Motor de validación de restricciones duras R1–R9 | CU-11 | [HDC-15](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-15) |
| RF-10 | Tablero semanal de arrastrar y soltar con validación en línea | CU-12 | [HDC-17](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-17) |
| RF-11 | Reportar el cumplimiento de restricciones blandas | CU-13 | [HDC-18](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-18) |
| RF-12 | Versionar y publicar el horario con bloqueo por reglas duras | CU-14, CU-15 | [HDC-19](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-19) |
| RF-13 | Exportar el horario publicado en CSV y PDF | CU-19 | [HDC-20](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-20) |
| RF-14 | Reportes de uso de aulas, carga docente y grupos bajo cupo | CU-16 | [HDC-21](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-21) |
| RF-15 | Autenticación y control de acceso por rol | CU-01 | [HDC-22](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-22) |
| RF-16 | Crear periodos académicos con parámetros independientes | CU-03 | [HDC-70](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-70) |
| RF-17 | Importar docentes, disponibilidad, cursos y aulas desde CSV | CU-07 | [HDC-71](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-71) |
| RF-18 | Registrar la demanda y sugerir el número de grupos | CU-09 | [HDC-84](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-84) |
| RF-19 | Consultar el horario por docente, aula, curso y semestre | CU-18 | [HDC-74](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-74) |
| RF-20 | Registrar el concepto y las observaciones del comité | CU-20 | [HDC-82](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-82) |
| RF-21 | Registrar inscritos y cerrar grupos por debajo del cupo mínimo | CU-17 | [HDC-76](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-76), [HDC-77](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-77) |
| RF-22 | Gestionar usuarios y roles | CU-02 | [HDC-69](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-69) |
| RF-23 | Registrar el horario de atención por materia (pedido en la validación con usuario) | CU-21 | [HDC-86](https://unbosque-team-k30lb9ry.atlassian.net/browse/HDC-86) |

## Usuarios iniciales

Todos comparten la contraseña inicial definida por el equipo; cada uno la cambia en **Mi cuenta**.

| Rol | Correo |
|---|---|
| Coordinación de horarios | `coordinacion@unibosque.com` |
| Comité | `comite.software@unibosque.com` · `comite.ciencias@unibosque.com` |
| Docentes | Cada docente con su correo institucional (p. ej. `gneuta@unibosque.com`); la lista está en *Usuarios y roles* |
| Administración | `admin@unibosque.com` |

> [!WARNING]
> El sitio es público: después de la sustentación cambie las contraseñas, sobre todo la del administrador y la de
> coordinación.

## Correr el proyecto en local

**Requisitos:** JDK 17 y Node.js 20.19+ o 22.12+.

En Antigravity: `Ctrl+Shift+P` → **Tasks: Run Task** → **Frontend: instalar dependencias** (la primera vez) y luego
**Iniciar todo (backend + frontend)**. O a mano, en dos terminales:

```powershell
cd Backend/horarios-ueb-back
.\gradlew.bat bootRun
```

```powershell
cd Frontend/horarios-ueb
npm install
npm start
```

El backend queda en `http://localhost:8080` y el front en `http://localhost:4200`. En local se usa el perfil
**`local`**: una base **H2 en archivo** (`Backend/horarios-ueb-back/data/`) con el mismo esquema y datos que MySQL,
creada por Flyway al arrancar. Para empezar de cero, detenga el backend y borre esa carpeta.

Pruebas: `.\gradlew.bat test` (JUnit) y la colección de Bruno según [Pruebas/README.md](Pruebas/README.md).

## Cómo está desplegado

| Pieza | Servicio | URL |
|---|---|---|
| Front | Vercel, rama `main` | https://horarios-ueb-sistema.vercel.app |
| API | Render, Docker desde `render.yaml` | https://horarios-ueb-sistema.onrender.com/api/salud |
| Base de datos | Aiven MySQL 8 (plan gratuito) | Solo accesible desde el backend |

Cada push a `main` vuelve a desplegar Vercel y Render. En el plan gratuito el backend se **duerme tras 15 minutos** sin
uso y tarda cerca de un minuto en despertar; la pantalla de carga del inicio de sesión lo explica al usuario.

<details>
<summary><b>Montarlo desde cero (Aiven → Render → Vercel)</b></summary>

1. **Aiven.** *Create service → MySQL → Free plan*. Con los datos de *Connection information* arme
   `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&rewriteBatchedStatements=true&serverTimezone=UTC&characterEncoding=UTF-8`.
   No hace falta crear tablas: Flyway las crea la primera vez.
2. **Render.** *New → Blueprint* con este repositorio. Complete `DB_URL`, `DB_USER` y `DB_PASSWORD`; `JWT_SECRET` se
   genera solo y `CORS_ORIGINS` ya trae la URL de Vercel. Verifique `/api/salud`.
3. **Vercel.** *Add New → Project*, **Root Directory** `Frontend/horarios-ueb`, rama de producción `main`.
4. **Opcional.** En [cron-job.org](https://cron-job.org), una llamada cada 10 minutos a `/api/salud` mantiene despiertos
   el backend y la base.

Si Render asigna otra URL, cámbiela en `Frontend/horarios-ueb/src/environments/environment.production.ts` y en
`connect-src` de `Frontend/horarios-ueb/vercel.json`.

</details>

---

<p align="center">
  <img src="Frontend/horarios-ueb/public/ueb-simbolo.png" alt="Universidad El Bosque" width="48"><br>
  <sub>Proyecto Núcleo II · Universidad El Bosque · Programa de Ingeniería de Sistemas</sub>
</p>
