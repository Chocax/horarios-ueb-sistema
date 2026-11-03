// Prueba de carga de la API (RNF-01): varios usuarios consultando el estado del periodo al mismo tiempo.
// Uso:  node carga.mjs [baseUrl] [peticiones] [concurrencia]      la clave se pasa en la variable CLAVE
//   PowerShell:  $env:CLAVE="..."; node carga.mjs http://localhost:8080/api 200 20
// Sin dependencias: usa fetch de Node 20+.
const base = process.argv[2] ?? 'http://localhost:8080/api';
const total = Number(process.argv[3] ?? 200);
const concurrencia = Number(process.argv[4] ?? 20);
const clave = process.env.CLAVE;
if (!clave) {
  console.error('Defina la variable de entorno CLAVE con la contraseña del coordinador.');
  process.exit(1);
}

const login = await fetch(`${base}/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ correo: 'coordinacion@unibosque.com', clave }),
});
if (!login.ok) {
  console.error('No se pudo iniciar sesión:', login.status, await login.text());
  process.exit(1);
}
const token = (await login.json()).valor.token;

const tiempos = [];
let errores = 0;
let siguiente = 0;
async function trabajador() {
  while (siguiente < total) {
    siguiente++;
    const t0 = performance.now();
    try {
      const r = await fetch(`${base}/estado?periodo=per-2026-1`, { headers: { Authorization: `Bearer ${token}` } });
      await r.arrayBuffer();
      if (!r.ok) errores++;
    } catch {
      errores++;
    }
    tiempos.push(performance.now() - t0);
  }
}

const inicio = performance.now();
await Promise.all(Array.from({ length: concurrencia }, trabajador));
const duracion = (performance.now() - inicio) / 1000;
tiempos.sort((a, b) => a - b);
const p = (q) => tiempos[Math.min(tiempos.length - 1, Math.floor(q * tiempos.length))].toFixed(0);
const resumen = {
  url: `${base}/estado`,
  peticiones: total,
  concurrencia,
  errores,
  'duración (s)': duracion.toFixed(1),
  'peticiones/s': (total / duracion).toFixed(1),
  'p50 (ms)': p(0.5),
  'p95 (ms)': p(0.95),
  'máx (ms)': tiempos.at(-1).toFixed(0),
};
console.table(resumen);
// criterio de aceptación del plan: 0 errores y p95 < 2000 ms
process.exit(errores === 0 && Number(resumen['p95 (ms)']) < 2000 ? 0 : 1);
