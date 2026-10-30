import { Rol } from '../core/models';

export interface ItemNav {
  ruta: string;
  texto: string;
  icono: string;
  grupo?: string;
  roles: Rol[];
}

const TODOS: Rol[] = ['COORDINADOR', 'COMITE', 'DOCENTE', 'ADMINISTRADOR'];

export const NAV: ItemNav[] = [
  { ruta: '/inicio', texto: 'Inicio', icono: 'home', roles: ['COORDINADOR'] },
  { ruta: '/revision', texto: 'Revisión de propuestas', icono: 'message', roles: ['COMITE'] },
  { ruta: '/mi-horario', texto: 'Mi horario', icono: 'calendar', roles: ['DOCENTE'] },
  { ruta: '/mi-disponibilidad', texto: 'Mi disponibilidad', icono: 'clock', roles: ['DOCENTE'] },
  { ruta: '/usuarios', texto: 'Usuarios y roles', icono: 'shield', roles: ['ADMINISTRADOR'] },
  { ruta: '/propuestas', texto: 'Propuestas de horario', icono: 'history', roles: ['ADMINISTRADOR'] },

  { ruta: '/periodos', texto: 'Periodos y parámetros', icono: 'settings', grupo: 'Planeación', roles: ['COORDINADOR', 'ADMINISTRADOR'] },
  { ruta: '/docentes', texto: 'Docentes', icono: 'users', grupo: 'Planeación', roles: ['COORDINADOR'] },
  { ruta: '/cursos', texto: 'Cursos', icono: 'book', grupo: 'Planeación', roles: ['COORDINADOR'] },
  { ruta: '/aulas', texto: 'Aulas', icono: 'door', grupo: 'Planeación', roles: ['COORDINADOR'] },
  { ruta: '/grupos', texto: 'Grupos y demanda', icono: 'layers', grupo: 'Planeación', roles: ['COORDINADOR'] },
  { ruta: '/importar', texto: 'Importar datos', icono: 'upload', grupo: 'Planeación', roles: ['COORDINADOR'] },

  { ruta: '/tablero', texto: 'Tablero del horario', icono: 'board', grupo: 'Programación', roles: ['COORDINADOR'] },
  { ruta: '/versiones', texto: 'Versiones y publicación', icono: 'history', grupo: 'Programación', roles: ['COORDINADOR'] },
  { ruta: '/consulta', texto: 'Consultar horario', icono: 'eye', grupo: 'Programación', roles: ['COORDINADOR', 'COMITE', 'ADMINISTRADOR'] },

  { ruta: '/reportes', texto: 'Reportes', icono: 'chart', grupo: 'Seguimiento', roles: ['COORDINADOR'] },
  { ruta: '/inscritos', texto: 'Inscritos y cierre', icono: 'list', grupo: 'Seguimiento', roles: ['COORDINADOR'] },
  { ruta: '/bitacora', texto: 'Bitácora', icono: 'lock', grupo: 'Seguimiento', roles: ['COORDINADOR', 'ADMINISTRADOR'] },

  { ruta: '/mi-cuenta', texto: 'Mi cuenta', icono: 'shield', grupo: 'Cuenta', roles: TODOS },
];

export const RUTA_INICIAL: Record<Rol, string> = {
  COORDINADOR: '/inicio',
  COMITE: '/revision',
  DOCENTE: '/mi-horario',
  ADMINISTRADOR: '/usuarios',
};
