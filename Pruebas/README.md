# Pruebas · Horarios UEB

| Tipo | Herramienta | Dónde | Cómo se corre |
|---|---|---|---|
| Unitarias e integración del dominio | JUnit 5 + Spring Boot Test | `Backend/horarios-ueb-back/src/test` | `.\gradlew.bat test` (o la tarea **Backend: pruebas**) |
| Integración de la API y seguridad | **Bruno** | `Backend/horarios-ueb-back/bruno` | App de Bruno o `bru run` (abajo) |
| Rendimiento (carga) | Script Node (`fetch`) | `Pruebas/rendimiento/carga.mjs` | `node carga.mjs URL peticiones concurrencia` |
| Regresión | GitHub Actions | `.github/workflows/pruebas.yml` | Automático en cada push a `develop` y `main` |
| Funcionales de interfaz y usabilidad | Manual (guion + heurísticas de Nielsen) | Documento del plan de pruebas | Navegador |

Las evidencias de la última ejecución están en `Pruebas/evidencias/` (reporte HTML de Bruno, JUnit XML y la prueba de carga).

## Bruno en 5 minutos

Bruno es un cliente de APIs como Postman, pero guarda cada petición como un archivo de texto (`.bru`) dentro del
repositorio. Las pruebas viajan con el código, se revisan en los pull requests y no dependen de una cuenta en la nube.

### 1. Instalar y abrir la colección

1. Descargue Bruno desde [usebruno.com/downloads](https://www.usebruno.com/downloads) (gratis).
2. **Open Collection** → elija la carpeta `Backend/horarios-ueb-back/bruno`.
3. Arriba a la derecha, en el selector de ambientes, elija **local** (backend en su computador) o **produccion** (Render).
4. En **Configure** del ambiente escriba el valor de la variable secreta `clave` (la contraseña inicial de los usuarios).
   Bruno la guarda solo en su computador: nunca queda en los archivos del repositorio.

### 2. Cómo está organizada

| Carpeta | Qué prueba | ¿Modifica datos? |
|---|---|---|
| `01 Autenticacion y seguridad` | Salud, login de cada rol (guarda los tokens), clave errada, inyección SQL, token ausente o alterado, permisos por rol, política de contraseñas | No |
| `02 Validaciones del catalogo` | Reglas que el servidor debe rechazar (R9, vinculación, franjas, importación todo o nada…) | No: todas se rechazan |
| `03 Consultas` | Estado por rol, horario publicado, bitácora, usuarios | No |
| `04 Flujo del horario (solo local)` | Generar → mover (R3 y válido) → concepto del comité → publicar → restaurar → inscritos → borrar versiones | **Sí**: se omite sola si el ambiente no es `local` |

Cada petición tiene:

- **Body / Auth**: el JSON que se envía y el token (`{{tokenCoordinador}}`, etc.) que dejó el login.
- **Script › Post Response**: guarda datos para las siguientes peticiones (`bru.setVar('versionGenerada', …)`).
- **Tests**: las verificaciones (`expect(res.status).to.equal(200)`). En verde, la prueba pasó.
- **Docs**: el código del caso de prueba (TC-xx) del plan.

### 3. Ejecutar

- Una petición: ábrala y pulse la flecha (o `Ctrl+Enter`).
- Toda la colección: clic derecho sobre la colección → **Run** → **Run Collection**. Primero corre la carpeta 01 porque
  ahí se obtienen los tokens.

Desde la terminal (útil para CI o para generar el reporte de evidencias):

```bash
npm install -g @usebruno/cli
```
```bash
cd Backend/horarios-ueb-back/bruno
```
```bash
bru run -r --env local --env-var "clave=LA_CLAVE" --reporter-html ../../../Pruebas/evidencias/bruno-local.html
```

### 4. Crear una prueba nueva

1. Clic derecho en una carpeta → **New Request**. Póngale nombre, método y URL usando `{{baseUrl}}`.
2. En **Auth** elija *Bearer Token* y escriba `{{tokenCoordinador}}` (o el rol que corresponda).
3. En **Tests** escriba las verificaciones, por ejemplo:
   ```js
   test("responde 200", function () {
     expect(res.status).to.equal(200);
   });
   ```
4. Guarde (`Ctrl+S`): aparece un archivo `.bru` nuevo en la carpeta. Haga commit como cualquier otro archivo.

> [!WARNING]
> Si una prueba nueva crea o borra datos, agregue en **Script › Pre Request** la protección que usan las de la carpeta 04
> (`if (bru.getEnvName() !== 'local') bru.runner.skipRequest();`) para que nunca corra contra producción.

## Integración continua

En GitHub: **Settings → Secrets and variables → Actions → New repository secret** con nombre `CLAVE_PRUEBAS` y la
contraseña inicial. Desde ese momento cada push a `develop` o `main` corre JUnit, levanta el backend y ejecuta toda la
colección de Bruno; los reportes quedan como artefacto en la pestaña **Actions**.
