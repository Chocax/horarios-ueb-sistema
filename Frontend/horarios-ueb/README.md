# Frontend · Horarios UEB

Aplicación Angular 21 (componentes standalone, signals, Angular CDK para arrastrar y soltar) con el estilo del
`Frontend/design/DESIGN.md`. Toda la información viene del backend (`src/environments/`):

| Archivo | Se usa en | URL de la API |
|---|---|---|
| `environment.ts` | `npm start` (desarrollo) | `http://localhost:8080/api` |
| `environment.production.ts` | `npm run build` (Vercel) | `https://horarios-ueb-back.onrender.com/api` |

```bash
npm install
npm start        # http://localhost:4200 (necesita el backend corriendo en el puerto 8080)
npm run build    # compilación de producción en dist/horarios-ueb/browser
```

- `core/services/api.service.ts`: cliente HTTP; agrega el token de sesión y convierte errores en mensajes.
- `core/services/store.service.ts`: estado en memoria de lo que el servidor confirmó.
- `core/rules/`: copia de las reglas R1–R9 y B1–B6 para mostrar violaciones y rechazar al instante en el tablero;
  el servidor vuelve a validar todo antes de guardar.
- `vercel.json`: configuración del despliegue y cabeceras de seguridad (CSP). Si la URL del backend cambia,
  actualícela en `environment.production.ts` **y** en `connect-src` de `vercel.json`.

Vea el [README principal](../../README.md) para el despliegue completo.
