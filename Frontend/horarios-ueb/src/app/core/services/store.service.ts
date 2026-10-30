// Estado de la aplicación en el navegador. La fuente de verdad es el backend: cada operación se envía a la API,
// que valida permisos y reglas (R1–R9) y escribe la bitácora; aquí solo se guarda lo que el servidor confirma.
import { computed, inject, Injectable, signal } from '@angular/core';
import {
  Aula, BloqueDisponible, Curso, Dia, Docente, EstadoApp, Grupo, HorarioAtencion, IncumplimientoBlando, Observacion, OfertaCurso,
  ParametrosProgramacion, PeriodoAcademico, RegistroBitacora, Rol, Sesion, Usuario, VersionHorario,
} from '../models';
import { evaluarBlandas } from '../rules/reglas-blandas';
import { Catalogo, cupoEfectivo } from '../rules/reglas-duras';
import { ValidadorReglas } from '../rules/validador';
import { rango } from '../util';
import { ApiService, Resultado } from './api.service';
import { SessionService } from './session.service';

export type { Resultado } from './api.service';

const VACIO: EstadoApp = {
  periodos: [], cursos: [], ofertas: [], docentes: [], aulas: [], grupos: [], versiones: [], observaciones: [], usuarios: [],
  horariosAtencion: [], periodoActivoId: '',
};

interface Login {
  token: string;
  expiraEn: number;
  usuario: { id: string; nombre: string; correo: string; rol: Rol; docenteId?: string | null };
}

@Injectable({ providedIn: 'root' })
export class StoreService {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SessionService);
  /** Validación local solo para dar respuesta inmediata en el tablero; el servidor vuelve a validar. */
  readonly validador = new ValidadorReglas();

  readonly estado = signal<EstadoApp>(VACIO);
  /** true cuando ya se cargó el estado del servidor en esta sesión. */
  readonly listo = signal(false);
  readonly cargando = signal(false);
  readonly bitacora = signal<RegistroBitacora[]>([]);

  // ------------------------------------------------------------------ selectores
  readonly periodos = computed(() => [...this.estado().periodos].sort((a, b) => a.codigo.localeCompare(b.codigo)));
  readonly periodo = computed(() => this.estado().periodos.find((p) => p.id === this.estado().periodoActivoId)!);
  readonly parametros = computed(() => this.periodo().parametros);
  readonly cursos = computed(() => this.estado().cursos);
  readonly docentes = computed(() => [...this.estado().docentes].sort((a, b) => a.nombre.localeCompare(b.nombre)));
  readonly aulas = computed(() => this.estado().aulas);
  readonly usuarios = computed(() => this.estado().usuarios);
  readonly ofertasPeriodo = computed(() => this.estado().ofertas.filter((o) => o.periodoId === this.periodo().id));
  readonly gruposPeriodo = computed(() => this.estado().grupos.filter((g) => g.periodoId === this.periodo().id));
  readonly gruposAbiertos = computed(() => this.gruposPeriodo().filter((g) => g.estado === 'ABIERTO'));
  readonly versionesPeriodo = computed(() =>
    this.estado().versiones.filter((v) => v.periodoId === this.periodo().id).sort((a, b) => b.numero - a.numero),
  );
  readonly versionTrabajo = computed(() => this.versionesPeriodo().find((v) => v.estado === 'BORRADOR') ?? null);
  readonly versionPublicada = computed(() => this.versionesPeriodo().find((v) => v.estado === 'PUBLICADA') ?? null);
  readonly observaciones = computed(() => this.estado().observaciones);
  readonly horariosAtencion = computed(() => this.estado().horariosAtencion.filter((h) => h.periodoId === this.periodo().id));

  readonly catalogo = computed<Catalogo>(() => {
    const e = this.estado();
    const mc = new Map(e.cursos.map((x) => [x.id, x]));
    const mg = new Map(e.grupos.map((x) => [x.id, x]));
    const md = new Map(e.docentes.map((x) => [x.id, x]));
    const ma = new Map(e.aulas.map((x) => [x.id, x]));
    return { curso: (id) => mc.get(id), grupo: (id) => mg.get(id), docente: (id) => md.get(id), aula: (id) => ma.get(id) };
  });

  version(id: string | null | undefined) {
    return id ? this.estado().versiones.find((v) => v.id === id) ?? null : null;
  }
  parametrosDe(v: VersionHorario) {
    return this.estado().periodos.find((p) => p.id === v.periodoId)!.parametros;
  }
  violacionesDe(v: VersionHorario) {
    return this.validador.validarVersion(v, this.parametrosDe(v), this.catalogo());
  }
  blandasDe(v: VersionHorario): IncumplimientoBlando[] {
    return evaluarBlandas(v, this.parametrosDe(v), this.catalogo(), this.docentes().map((d) => d.id));
  }
  etiquetaGrupo(grupoId: string) {
    const g = this.catalogo().grupo(grupoId);
    const c = g ? this.catalogo().curso(g.cursoId) : undefined;
    return g && c ? `${c.nombre} ${g.codigo}` : '—';
  }
  sesionesRequeridas(grupoId: string) {
    const g = this.catalogo().grupo(grupoId);
    const c = g ? this.catalogo().curso(g.cursoId) : undefined;
    return c ? Math.max(this.parametros().sesionesMinimas, c.sesionesSemanales) : 0;
  }
  cargaDocente(v: VersionHorario | null, docenteId: string) {
    if (!v) return 0;
    return v.sesiones.filter((s) => s.docenteId === docenteId).length * this.parametrosDe(v).duracionSesion;
  }
  sugerenciaGrupos(cursoId: string) {
    const o = this.ofertasPeriodo().find((x) => x.cursoId === cursoId);
    const p = this.parametros();
    const demanda = o?.demandaEstimada ?? 0;
    return { demanda, sugeridos: demanda > 0 ? Math.ceil(demanda / p.cupoMaximo) : 0, minimo: demanda > 0 ? Math.ceil(demanda / cupoEfectivo(p)) : 0 };
  }

  // ------------------------------------------------------------------ carga y autenticación (CU-01)
  /** Trae del servidor el estado del periodo indicado (o del periodo abierto más reciente). */
  async cargar(periodoId = this.estado().periodoActivoId): Promise<Resultado> {
    this.cargando.set(true);
    const r = await this.api.get<EstadoApp>(`/estado${periodoId ? `?periodo=${encodeURIComponent(periodoId)}` : ''}`);
    this.cargando.set(false);
    if (!r.ok || !r.valor) return { ok: false, mensaje: r.mensaje };
    this.estado.set(r.valor);
    this.listo.set(true);
    return { ok: true };
  }

  async iniciarSesion(correo: string, clave: string): Promise<Resultado> {
    const r = await this.api.post<Login>('/auth/login', { correo: correo.trim(), clave });
    if (!r.ok || !r.valor) return { ok: false, mensaje: r.mensaje ?? 'No se pudo iniciar sesión.' };
    const u = r.valor.usuario;
    this.sesion.iniciar({ usuarioId: u.id, nombre: u.nombre, correo: u.correo, rol: u.rol, docenteId: u.docenteId, token: r.valor.token, expira: Date.now() + r.valor.expiraEn * 1000 });
    return this.cargar('');
  }

  cerrarSesion() {
    this.sesion.cerrar();
    this.estado.set(VACIO);
    this.bitacora.set([]);
    this.listo.set(false);
  }

  cambiarClave(claveActual: string, claveNueva: string): Promise<Resultado> {
    return this.api.post('/auth/cambiar-clave', { claveActual, claveNueva }) as Promise<Resultado>;
  }

  // ------------------------------------------------------------------ periodos y parámetros (CU-03)
  seleccionarPeriodo(id: string) {
    return this.cargar(id);
  }

  async crearPeriodo(anio: number, semestre: 1 | 2, copiarOferta: boolean): Promise<Resultado<PeriodoAcademico>> {
    const r = await this.api.post<PeriodoAcademico>('/periodos', { anio, semestre, copiarOferta, basePeriodoId: this.periodo()?.id ?? null });
    if (r.ok && r.valor) await this.cargar(r.valor.id);
    return r;
  }

  async actualizarParametros(periodoId: string, p: ParametrosProgramacion): Promise<Resultado> {
    const r = await this.api.put<PeriodoAcademico>(`/periodos/${periodoId}/parametros`, p);
    if (r.ok && r.valor) this.reemplazar('periodos', r.valor);
    return r as Resultado;
  }

  // ------------------------------------------------------------------ catálogo (CU-04, CU-06)
  async guardarDocente(d: Docente): Promise<Resultado> {
    const r = await this.api.put<Docente>(`/docentes/${d.id}`, d);
    if (r.ok && r.valor) this.reemplazar('docentes', r.valor);
    return r as Resultado;
  }

  async actualizarDisponibilidad(docenteId: string, bloques: BloqueDisponible[]): Promise<Resultado> {
    const r = await this.api.put<Docente>(`/docentes/${docenteId}/disponibilidad`, { bloques });
    if (r.ok && r.valor) this.reemplazar('docentes', r.valor);
    return r as Resultado;
  }

  async guardarCurso(c: Curso, demanda: number | null): Promise<Resultado> {
    const pid = this.periodo().id;
    const r = await this.api.put<Curso>(`/cursos/${c.id}`, { curso: c, periodoId: pid, demanda });
    if (r.ok && r.valor) {
      this.reemplazar('cursos', r.valor);
      if (demanda !== null) this.ponerOferta({ periodoId: pid, cursoId: c.id, demandaEstimada: demanda });
    }
    return r as Resultado;
  }

  async guardarAula(a: Aula): Promise<Resultado> {
    const r = await this.api.put<Aula>(`/aulas/${a.id}`, a);
    if (r.ok && r.valor) this.reemplazar('aulas', r.valor);
    return r as Resultado;
  }

  // ------------------------------------------------------------------ grupos y demanda (CU-08, CU-09)
  async actualizarDemanda(cursoId: string, demanda: number): Promise<Resultado> {
    const pid = this.periodo().id;
    const r = await this.api.put<OfertaCurso>(`/periodos/${pid}/ofertas/${cursoId}`, { demanda });
    if (r.ok && r.valor) this.ponerOferta(r.valor);
    return r as Resultado;
  }

  /** Ajusta el número de grupos de un curso; el servidor valida el cupo efectivo (RF-18). */
  async aplicarNumeroGrupos(cursoId: string, numero: number): Promise<Resultado> {
    const r = await this.api.post(`/periodos/${this.periodo().id}/cursos/${cursoId}/numero-grupos`, { numero });
    if (r.ok) await this.cargar();
    return r as Resultado;
  }

  async guardarGrupo(g: Grupo): Promise<Resultado> {
    const r = await this.api.put<Grupo>(`/grupos/${g.id}`, g);
    if (r.ok && r.valor) this.reemplazar('grupos', r.valor);
    return r as Resultado;
  }

  // ------------------------------------------------------------------ importación CSV (CU-07): todo o nada en el servidor
  async aplicarImportacion(cambios: Partial<Pick<EstadoApp, 'docentes' | 'cursos' | 'aulas' | 'ofertas'>>, resumen: string): Promise<Resultado> {
    const e = this.estado();
    const distintos = <T,>(nuevos: T[] | undefined, actuales: T[], clave: (x: T) => string) => {
      if (!nuevos) return undefined;
      const m = new Map(actuales.map((x) => [clave(x), JSON.stringify(x)]));
      return nuevos.filter((x) => m.get(clave(x)) !== JSON.stringify(x));
    };
    const cuerpo = {
      docentes: distintos(cambios.docentes, e.docentes, (x) => x.id),
      cursos: distintos(cambios.cursos, e.cursos, (x) => x.id),
      aulas: distintos(cambios.aulas, e.aulas, (x) => x.id),
      ofertas: distintos(cambios.ofertas, e.ofertas, (x) => `${x.periodoId}|${x.cursoId}`)?.filter((o) => o.periodoId === this.periodo().id),
      resumen,
    };
    const r = await this.api.post(`/periodos/${this.periodo().id}/importacion`, cuerpo);
    if (r.ok) await this.cargar();
    return r as Resultado;
  }

  // ------------------------------------------------------------------ generación y tablero (CU-10, CU-11, CU-12)
  async generarPropuesta(): Promise<Resultado<VersionHorario>> {
    const r = await this.api.post<VersionHorario>(`/periodos/${this.periodo().id}/generar`);
    if (r.ok && r.valor) this.nuevaVersion(r.valor);
    return r;
  }

  /** Mueve o reasigna una sesión. Valida R1–R9 en el navegador para responder al instante y el servidor lo confirma (RF-10). */
  async moverSesion(versionId: string, sesionId: string, cambios: { dia?: Dia; inicio?: number; docenteId?: string | null; aulaId?: string | null }): Promise<Resultado> {
    const v = this.version(versionId);
    if (!v || v.estado !== 'BORRADOR') return { ok: false, mensaje: 'Solo se puede ajustar la versión de trabajo (borrador).' };
    const s = v.sesiones.find((x) => x.id === sesionId);
    if (!s) return { ok: false, mensaje: 'La sesión ya no existe; recargue la página.' };
    const nueva: Sesion = { ...s, ...cambios };
    const viol = this.validador.validar(
      { sesionId: s.id, grupoId: s.grupoId, dia: nueva.dia, inicio: nueva.inicio, docenteId: nueva.docenteId, aulaId: nueva.aulaId },
      v.sesiones, this.parametrosDe(v), this.catalogo(),
    );
    if (viol.length) return { ok: false, mensaje: 'Movimiento rechazado', violaciones: viol };
    const r = await this.api.patch<VersionHorario>(`/versiones/${versionId}/sesiones/${sesionId}`, {
      dia: nueva.dia, inicio: nueva.inicio, docenteId: nueva.docenteId, aulaId: nueva.aulaId,
    });
    if (r.ok && r.valor) this.reemplazar('versiones', r.valor);
    return r as Resultado;
  }

  /** Ubica una sesión de un grupo pendiente en el día y hora indicados; el servidor elige docente y aula válidos. */
  async ubicarSesionPendiente(versionId: string, grupoId: string, dia: Dia, inicio: number): Promise<Resultado> {
    const r = await this.api.post<VersionHorario>(`/versiones/${versionId}/pendientes/${grupoId}/ubicar`, { dia, inicio });
    if (r.ok && r.valor) this.reemplazar('versiones', r.valor);
    return r as Resultado;
  }

  async reintentarPendiente(versionId: string, grupoId: string): Promise<Resultado> {
    const r = await this.api.post<VersionHorario>(`/versiones/${versionId}/pendientes/${grupoId}/reintentar`);
    if (r.ok && r.valor) this.reemplazar('versiones', r.valor);
    return r as Resultado;
  }

  // ------------------------------------------------------------------ horario de atención (RF-23)
  atencionDe(docenteId: string, cursoId: string, periodoId = this.periodo().id) {
    return this.estado().horariosAtencion.find((h) => h.periodoId === periodoId && h.docenteId === docenteId && h.cursoId === cursoId) ?? null;
  }

  /**
   * Materias que dicta el docente en el periodo: las de sus sesiones en la versión vigente (borrador o publicada)
   * y las de los grupos donde es docente preferente (asignación acordada antes de generar).
   */
  cursosDictados(docenteId: string, periodoId = this.periodo().id): Curso[] {
    const ids = new Set<string>();
    for (const s of this.vigente(periodoId)?.sesiones ?? []) if (s.docenteId === docenteId) ids.add(this.catalogo().grupo(s.grupoId)?.cursoId ?? '');
    for (const g of this.estado().grupos) if (g.periodoId === periodoId && g.estado === 'ABIERTO' && g.docentePreferido === docenteId) ids.add(g.cursoId);
    ids.delete('');
    return [...ids].map((id) => this.catalogo().curso(id)!).filter(Boolean).sort((a, b) => a.nivel - b.nivel || a.nombre.localeCompare(b.nombre));
  }

  /** Aviso cuando una clase (generada o movida después) quedó encima del horario de atención. */
  avisoAtencion(h: HorarioAtencion): string | null {
    if (h.modo !== 'HORARIO' || h.dia === null || h.inicio === null || h.fin === null) return null;
    const v = this.vigente(h.periodoId);
    if (!v) return null;
    const dur = this.parametrosDe(v).duracionSesion;
    const clase = v.sesiones.find((s) => s.docenteId === h.docenteId && s.dia === h.dia && s.inicio < h.fin! && h.inicio! < s.inicio + dur);
    return clase ? `Ahora se cruza con ${this.etiquetaGrupo(clase.grupoId)}; ajuste el horario de atención.` : null;
  }

  async guardarAtencion(h: HorarioAtencion): Promise<Resultado> {
    const r = await this.api.put<HorarioAtencion>('/atencion', h);
    if (r.ok && r.valor) {
      const nuevo = r.valor;
      this.estado.update((e) => ({
        ...e,
        horariosAtencion: [...e.horariosAtencion.filter((x) => !(x.periodoId === nuevo.periodoId && x.docenteId === nuevo.docenteId && x.cursoId === nuevo.cursoId)), nuevo],
      }));
    }
    return r as Resultado;
  }

  async eliminarAtencion(h: HorarioAtencion): Promise<Resultado> {
    const r = await this.api.delete(`/atencion/${h.id}`);
    if (r.ok) this.estado.update((e) => ({ ...e, horariosAtencion: e.horariosAtencion.filter((x) => x.id !== h.id) }));
    return r as Resultado;
  }

  textoAtencion(h: HorarioAtencion | null): string {
    if (!h) return 'Sin definir';
    if (h.modo === 'ACORDAR') return `Acordar con el docente${h.lugar ? ' · ' + h.lugar : ''}`;
    const dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    return `${dias[h.dia!]} ${rango(h.inicio!, h.fin!)}${h.lugar ? ' · ' + h.lugar : ''}`;
  }

  // ------------------------------------------------------------------ versiones (CU-14, CU-15)
  async publicarVersion(versionId: string): Promise<Resultado> {
    const r = await this.api.post<VersionHorario>(`/versiones/${versionId}/publicar`);
    if (r.ok) await this.cargar();
    return r as Resultado;
  }

  async restaurarVersion(versionId: string): Promise<Resultado<VersionHorario>> {
    const r = await this.api.post<VersionHorario>(`/versiones/${versionId}/restaurar`);
    if (r.ok && r.valor) this.nuevaVersion(r.valor);
    return r;
  }

  // ------------------------------------------------------------------ comité (CU-20)
  async registrarObservacion(versionId: string, concepto: Observacion['concepto'], texto: string): Promise<Resultado> {
    if (!texto.trim()) return { ok: false, mensaje: 'Escriba la observación.' };
    const r = await this.api.post<Observacion>(`/versiones/${versionId}/observaciones`, { concepto, texto: texto.trim() });
    if (r.ok && r.valor) this.estado.update((e) => ({ ...e, observaciones: [...e.observaciones, r.valor!] }));
    return r as Resultado;
  }

  // ------------------------------------------------------------------ inscritos y cierre (CU-17)
  async registrarInscritos(grupoId: string, inscritos: number | null): Promise<Resultado> {
    const r = await this.api.put<Grupo>(`/grupos/${grupoId}/inscritos`, { inscritos });
    if (r.ok && r.valor) this.reemplazar('grupos', r.valor);
    return r as Resultado;
  }

  async cerrarGrupo(grupoId: string): Promise<Resultado> {
    const r = await this.api.post(`/grupos/${grupoId}/cerrar`);
    if (r.ok) await this.cargar();
    return r as Resultado;
  }

  // ------------------------------------------------------------------ usuarios (CU-02) y bitácora (RF-15)
  async guardarUsuario(u: Usuario, clave?: string): Promise<Resultado> {
    const r = await this.api.put<Usuario>('/usuarios', { ...u, clave: clave || null });
    if (r.ok && r.valor) this.reemplazar('usuarios', r.valor);
    return r as Resultado;
  }

  async cargarBitacora(limite = 500): Promise<Resultado> {
    const r = await this.api.get<RegistroBitacora[]>(`/bitacora?limite=${limite}`);
    if (r.ok && r.valor) this.bitacora.set(r.valor);
    return r as Resultado;
  }

  // ------------------------------------------------------------------ internos
  private vigente(periodoId: string) {
    const v = this.estado().versiones.filter((x) => x.periodoId === periodoId);
    return v.find((x) => x.estado === 'BORRADOR') ?? v.find((x) => x.estado === 'PUBLICADA') ?? null;
  }

  /** Solo una versión de trabajo por periodo: el servidor archiva el borrador anterior. */
  private nuevaVersion(v: VersionHorario) {
    this.estado.update((e) => ({
      ...e,
      versiones: [...e.versiones.map((x) => (x.periodoId === v.periodoId && x.estado === 'BORRADOR' ? { ...x, estado: 'ARCHIVADA' as const } : x)), v],
    }));
  }

  private ponerOferta(o: OfertaCurso) {
    this.estado.update((e) => ({
      ...e,
      ofertas: [...e.ofertas.filter((x) => !(x.periodoId === o.periodoId && x.cursoId === o.cursoId)), o],
    }));
  }

  private reemplazar<K extends 'periodos' | 'docentes' | 'cursos' | 'aulas' | 'grupos' | 'versiones' | 'usuarios'>(clave: K, item: EstadoApp[K][number]) {
    this.estado.update((e) => {
      const lista = e[clave] as { id: string }[];
      const existe = lista.some((x) => x.id === (item as { id: string }).id);
      return { ...e, [clave]: existe ? lista.map((x) => (x.id === (item as { id: string }).id ? item : x)) : [...lista, item] };
    });
  }
}
