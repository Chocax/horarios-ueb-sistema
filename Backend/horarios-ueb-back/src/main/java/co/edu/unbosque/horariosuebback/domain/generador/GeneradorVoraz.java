package co.edu.unbosque.horariosuebback.domain.generador;

import co.edu.unbosque.horariosuebback.domain.Catalogo;
import co.edu.unbosque.horariosuebback.domain.Ids;
import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.domain.Textos;
import co.edu.unbosque.horariosuebback.domain.reglas.Asignacion;
import co.edu.unbosque.horariosuebback.domain.reglas.ValidadorReglas;
import co.edu.unbosque.horariosuebback.entity.Aula;
import co.edu.unbosque.horariosuebback.entity.BloqueDisponible;
import co.edu.unbosque.horariosuebback.entity.Curso;
import co.edu.unbosque.horariosuebback.entity.Docente;
import co.edu.unbosque.horariosuebback.entity.Grupo;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.enums.Caracteristica;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Algoritmo voraz con patrones semanales:
 * <ol>
 *   <li>Ordena los grupos: primero los que tienen docente preferente; luego de menos a más opciones (docentes × aulas).</li>
 *   <li>Para cada grupo arma candidatos «patrón de días + misma hora» (L-X, M-J, X-V, L-X-V… como se programan las clases en la
 *   práctica) y les asigna un costo: días seguidos (B1), sábado en jornada diurna, huecos y cruces en el horario del semestre (B4),
 *   horas menos deseadas y el desbalance de la semana (para que el viernes no quede casi vacío).</li>
 *   <li>Toma el candidato más barato que el validador R1–R9 aprueba, con el docente menos cargado (B3). En la jornada diurna prefiere
 *   docentes que no dicten otro grupo diurno del curso (norma del Programa, B6); si no hay, repite docente y la versión lo reporta.</li>
 *   <li>Si ningún patrón cabe, intenta sesión por sesión; si tampoco, el grupo queda PENDIENTE con su causa.</li>
 * </ol>
 */
@Component
public class GeneradorVoraz implements GeneradorHorario {

    /** Costo de un patrón poco habitual (días seguidos sin ser 3 sesiones, L-V…): solo si los habituales no caben. */
    private static final int PESO_PATRON = 12;
    /** Peso del desbalance de la semana por cada sesión. */
    private static final double PESO_BALANCE = 8;

    private final ValidadorReglas validador;

    public GeneradorVoraz(ValidadorReglas validador) {
        this.validador = validador;
    }

    @Override
    public String nombre() {
        return "Voraz por patrones semanales";
    }

    private record Candidato(int[][] franjas, double costo) {
    }

    private record Eleccion(double costo, List<Sesion> sesiones, Docente docente) {
    }

    @Override
    public Resultado generar(Entrada e) {
        long t0 = System.nanoTime();
        Parametros p = e.parametros();
        int dur = p.duracionSesion();
        Catalogo cat = e.catalogo();
        List<Sesion> fijas = e.sesionesFijas() == null ? List.of() : e.sesionesFijas();
        List<Sesion> nuevas = new ArrayList<>();
        List<Sesion> todas = new ArrayList<>(fijas);
        List<Pendiente> pendientes = new ArrayList<>();
        Map<String, Integer> carga = new HashMap<>();
        Ocupacion oc = new Ocupacion(cat, p);
        if (e.bloqueosDocente() != null) e.bloqueosDocente().forEach(b -> oc.bloquearDocente(b.docenteId(), b.dia(), b.inicio(), b.fin()));
        for (Sesion s : fijas) {
            oc.agregar(s);
            if (s.getDocenteId() != null) carga.merge(s.getDocenteId(), dur, Integer::sum);
        }

        Map<String, List<Aula>> aulasAptas = new HashMap<>();
        Map<String, List<Docente>> habilitados = new HashMap<>();
        for (Grupo g : e.grupos()) {
            aulasAptas.put(g.getId(), aulasAptas(g, cat.curso(g.getCursoId()), e.aulas()));
            habilitados.put(g.getId(), e.docentes().stream()
                    .filter(d -> d.getHabilitaciones().contains(g.getCursoId()) && sirveJornada(d, g, dur))
                    .sorted(Comparator.comparing(Docente::getId))
                    .toList());
        }

        List<Grupo> orden = new ArrayList<>(e.grupos());
        orden.sort(Comparator
                .comparingInt((Grupo g) -> g.getDocentePreferido() != null ? 0 : 1)
                .thenComparingInt(g -> habilitados.get(g.getId()).size() * Math.min(aulasAptas.get(g.getId()).size(), 20))
                .thenComparingInt(g -> -sesionesDe(g, cat, p))
                .thenComparingInt(g -> g.getJornada() == Jornada.NOCTURNA ? 0 : 1)
                .thenComparing(Grupo::getId));

        for (Grupo g : orden) {
            Curso curso = cat.curso(g.getCursoId());
            int n = sesionesDe(g, cat, p);
            if (curso.getSesionesSemanales() > p.sesionesMaximas()) {
                pendientes.add(new Pendiente(g.getId(), "R8", curso.getNombre() + " pide " + curso.getSesionesSemanales()
                        + " sesiones y el máximo es " + p.sesionesMaximas() + "."));
                continue;
            }
            if (g.getCupoPlaneado() > p.cupoEfectivo()) {
                pendientes.add(new Pendiente(g.getId(), "R9", "El cupo " + g.getCupoPlaneado() + " supera el máximo efectivo de " + p.cupoEfectivo() + "."));
                continue;
            }
            List<Docente> todos = habilitados.get(g.getId());
            if (todos.isEmpty()) {
                pendientes.add(new Pendiente(g.getId(), "R5", "Ningún docente habilitado para " + curso.getNombre()
                        + " tiene disponibilidad en la jornada " + g.getJornada().name().toLowerCase() + "."));
                continue;
            }
            List<Aula> aulas = aulasAptas.get(g.getId());
            if (aulas.isEmpty()) {
                String req = curso.getCaracteristicasRequeridas().isEmpty() ? ""
                        : " con " + curso.getCaracteristicasRequeridas().stream().sorted().map(Textos::caracteristica).collect(Collectors.joining(" y "));
                pendientes.add(new Pendiente(g.getId(), "R4", "No hay aula" + req + " con capacidad para " + g.getCupoPlaneado() + " estudiantes."));
                continue;
            }
            List<Docente> conHoras = new ArrayList<>(todos.stream()
                    .filter(d -> d.getHorasMaxDocencia() - carga.getOrDefault(d.getId(), 0) >= n * dur)
                    .toList());
            if (conHoras.isEmpty()) {
                pendientes.add(new Pendiente(g.getId(), "R7", "Los docentes habilitados (" + todos.stream()
                        .map(d -> d.getNombre() + ": " + carga.getOrDefault(d.getId(), 0) + "/" + d.getHorasMaxDocencia() + " h")
                        .collect(Collectors.joining("; ")) + ") no tienen horas libres para " + (n * dur) + " h más."));
                continue;
            }
            // menor ocupación relativa primero (equilibra la carga, B3)
            conHoras.sort(Comparator.comparingDouble((Docente d) -> carga.getOrDefault(d.getId(), 0) / (double) Math.max(1, d.getHorasMaxDocencia()))
                    .thenComparing(Docente::getId));

            // Norma del Programa (blanda, B6): los grupos diurnos de un curso los dictan docentes distintos.
            Set<String> ocupados = g.getJornada() == Jornada.DIURNA ? docentesDiurnosDelCurso(g, todas, cat) : Set.of();
            List<List<Docente>> niveles = new ArrayList<>();
            List<Docente> libres = conHoras.stream().filter(d -> !ocupados.contains(d.getId())).toList();
            List<Docente> repetidos = conHoras.stream().filter(d -> ocupados.contains(d.getId())).toList();
            if (!libres.isEmpty()) niveles.add(libres);
            if (!repetidos.isEmpty()) niveles.add(repetidos);

            Eleccion mejor = null;
            for (List<Docente> lista : niveles) {
                mejor = buscar(g, n, lista, aulas, oc, todas, e, carga);
                if (mejor != null) break;
            }
            if (mejor == null) {
                pendientes.add(new Pendiente(g.getId(), "R6", "No hay franja común entre la disponibilidad de "
                        + conHoras.stream().map(Docente::getNombre).collect(Collectors.joining(", "))
                        + " y las aulas libres (" + g.getJornada().name().toLowerCase() + ")."));
                continue;
            }
            mejor.sesiones().forEach(oc::agregar);
            nuevas.addAll(mejor.sesiones());
            todas.addAll(mejor.sesiones());
            carga.merge(mejor.docente().getId(), n * dur, Integer::sum);
        }
        return new Resultado(nuevas, pendientes, (System.nanoTime() - t0) / 1_000_000);
    }

    private Eleccion buscar(Grupo g, int n, List<Docente> lista, List<Aula> aulas, Ocupacion oc, List<Sesion> ya, Entrada e, Map<String, Integer> carga) {
        // asignación acordada con el docente: se intenta primero
        if (g.getDocentePreferido() != null) {
            Docente pref = lista.stream().filter(d -> d.getId().equals(g.getDocentePreferido())).findFirst().orElse(null);
            if (pref != null) {
                Eleccion r = mejorPatron(g, pref, n, aulas, oc, ya, e);
                if (r != null) return r;
            }
        }
        // si no, se evalúan los tres docentes menos cargados y se elige el mejor horario
        Eleccion mejor = null;
        for (Docente d : lista.subList(0, Math.min(3, lista.size()))) {
            Eleccion r = mejorPatron(g, d, n, aulas, oc, ya, e);
            if (r == null) continue;
            // a igualdad de horario se prefiere al docente con menos carga relativa (B3)
            double costo = r.costo() + carga.getOrDefault(d.getId(), 0) / (double) Math.max(1, d.getHorasMaxDocencia()) * 8;
            if (mejor == null || costo < mejor.costo()) mejor = new Eleccion(costo, r.sesiones(), d);
        }
        if (mejor != null) return mejor;
        for (Docente d : lista.subList(Math.min(3, lista.size()), lista.size())) {
            Eleccion r = mejorPatron(g, d, n, aulas, oc, ya, e);
            if (r != null) return r;
        }
        for (Docente d : lista) {
            List<Sesion> r = sesionPorSesion(g, d, n, aulas, ya, e, oc);
            if (r != null) return new Eleccion(999, r, d);
        }
        return null;
    }

    /** Busca el patrón de días + hora de menor costo en el que el docente y alguna aula estén libres. */
    private Eleccion mejorPatron(Grupo g, Docente d, int n, List<Aula> aulas, Ocupacion oc, List<Sesion> ya, Entrada e) {
        Parametros p = e.parametros();
        int dur = p.duracionSesion();
        int nivel = e.catalogo().curso(g.getCursoId()).getNivel();
        List<Candidato> candidatos = candidatos(g, n, nivel, oc, p);
        candidatos.sort(Comparator.comparingDouble(Candidato::costo));
        for (Candidato c : candidatos) {
            boolean disponible = true;
            for (int[] f : c.franjas()) {
                if (!disponible(d, f[0], f[1], dur) || oc.docenteOcupado(d.getId(), f[0], f[1])) {
                    disponible = false;
                    break;
                }
            }
            if (!disponible) continue;
            // misma aula toda la semana si se puede; si no, la primera libre de cada día
            Aula comun = aulas.stream().filter(a -> java.util.Arrays.stream(c.franjas()).noneMatch(f -> oc.aulaOcupada(a.getId(), f[0], f[1])))
                    .findFirst().orElse(null);
            List<Aula> porDia = new ArrayList<>();
            for (int[] f : c.franjas()) {
                porDia.add(comun != null ? comun : aulas.stream().filter(a -> !oc.aulaOcupada(a.getId(), f[0], f[1])).findFirst().orElse(null));
            }
            if (porDia.contains(null)) continue;
            List<Sesion> tent = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < c.franjas().length; i++) {
                int[] f = c.franjas()[i];
                Sesion s = new Sesion(Ids.nuevo("ses"), null, g.getId(), f[0], f[1], d.getId(), porDia.get(i).getId());
                // confirmación con el validador de reglas duras (CU-11)
                List<Sesion> base = new ArrayList<>(ya);
                base.addAll(tent);
                if (!validador.validar(new Asignacion(null, g.getId(), s.getDia(), s.getInicio(), s.getDocenteId(), s.getAulaId()), base, p, e.catalogo()).isEmpty()) {
                    ok = false;
                    break;
                }
                tent.add(s);
            }
            if (!ok) continue;
            double costoDocente = 0;
            for (int[] f : c.franjas()) costoDocente += oc.huecoDocente(d.getId(), f[0], f[1]);
            return new Eleccion(c.costo() + costoDocente + (comun != null ? 0 : 1), tent, d);
        }
        return null;
    }

    private List<Candidato> candidatos(Grupo g, int n, int nivel, Ocupacion oc, Parametros p) {
        boolean diurna = g.getJornada() == Jornada.DIURNA;
        int dur = p.duracionSesion();
        int[] horasLV = diurna ? new int[]{7, 9, 11, 14, 16} : new int[]{18, 20};
        Parametros.Rango fs = p.franjaSabado();
        List<Integer> horasSab = IntStream.of(7, 9, 11)
                .filter(h -> h >= fs.inicio() && h + dur <= fs.fin() && p.bloquesNoAsignables().stream().noneMatch(b -> b.seSolapaCon(h, h + dur)))
                .boxed().toList();
        List<Candidato> out = new ArrayList<>();
        for (int[] dias : combinaciones(n)) {
            boolean conSab = dias[dias.length - 1] == 5;
            int consecutivos = 0;
            for (int i = 1; i < dias.length; i++) if (dias[i] - dias[i - 1] == 1 && dias[i] != 5) consecutivos++;
            double base = consecutivos * 6 + (conSab ? (diurna ? 12 : 3) : 0) + costoPatron(dias);
            boolean entreSemana = dias[0] != 5;
            List<Integer> hs = entreSemana ? IntStream.of(horasLV).boxed().toList() : java.util.Collections.singletonList(null);
            List<Integer> hsSab = conSab ? horasSab : java.util.Collections.singletonList(null);
            for (Integer h : hs) {
                for (Integer hSab : hsSab) {
                    if (diurna && h != null && hSab != null && !h.equals(hSab)) continue; // diurna: misma hora también el sábado
                    int[][] franjas = new int[dias.length][];
                    for (int i = 0; i < dias.length; i++) franjas[i] = new int[]{dias[i], dias[i] == 5 ? hSab : h};
                    double costo = base;
                    for (int[] f : franjas) {
                        costo += preferenciaHora(f[0], f[1], diurna);
                        Ocupacion.EfectoNivel r = oc.efectoNivel(nivel, g.getJornada(), g.getCursoId(), f[0], f[1]);
                        // B4: cada sesión que se cruce con otro curso del semestre se evita, pero se permite como último recurso
                        costo += r.huecos() * 4 + r.sobrecarga() * 5 + (r.cruza() ? 40 : 0);
                        costo += oc.desbalanceDia(g.getJornada(), f[0]) * PESO_BALANCE;
                    }
                    out.add(new Candidato(franjas, costo));
                }
            }
        }
        return out;
    }

    /**
     * Costo del patrón de días entre semana. Habituales (0): una sola sesión, L-X, M-J, X-V y L-X-V. Aceptables (4): L-J, M-V.
     * El resto (días seguidos, L-V…) solo si los demás no caben.
     */
    private static double costoPatron(int[] dias) {
        int[] lv = java.util.Arrays.stream(dias).filter(x -> x != 5).toArray();
        if (lv.length <= 1) return 0;
        boolean cadaDos = true;
        boolean cadaTres = true;
        for (int i = 1; i < lv.length; i++) {
            cadaDos &= lv[i] - lv[i - 1] == 2;
            cadaTres &= lv[i] - lv[i - 1] == 3;
        }
        if (cadaDos) return 0;
        if (cadaTres) return 4;
        return PESO_PATRON;
    }

    private static double preferenciaHora(int dia, int h, boolean diurna) {
        if (dia == 5) return h == 11 ? 2 : 1;
        if (!diurna) return 0;
        return switch (h) {
            case 7, 9 -> 0;
            case 11, 14 -> 1;
            case 16 -> 2;
            default -> 1;
        };
    }

    /** Último recurso: ubica sesión por sesión en cualquier día y hora libre (sin exigir patrón). */
    private List<Sesion> sesionPorSesion(Grupo g, Docente d, int n, List<Aula> aulas, List<Sesion> ya, Entrada e, Ocupacion oc) {
        for (boolean evitarCruces : new boolean[]{true, false}) {
            List<Sesion> r = sesionPorSesionPasada(g, d, n, aulas, ya, e, oc, evitarCruces);
            if (r != null) return r;
        }
        return null;
    }

    private List<Sesion> sesionPorSesionPasada(Grupo g, Docente d, int n, List<Aula> aulas, List<Sesion> ya, Entrada e, Ocupacion oc, boolean evitarCruces) {
        Parametros p = e.parametros();
        int nivel = e.catalogo().curso(g.getCursoId()).getNivel();
        int[] horasLV = g.getJornada() == Jornada.DIURNA ? new int[]{7, 9, 11, 14, 16, 18} : new int[]{18, 20, 14, 16};
        List<Sesion> tent = new ArrayList<>();
        // los días menos cargados primero (el sábado al final)
        List<Integer> diasOrden = IntStream.rangeClosed(0, 5).boxed()
                .sorted(Comparator.comparingInt((Integer x) -> x == 5 ? Integer.MAX_VALUE : oc.sesionesDia(g.getJornada(), x)).thenComparingInt(x -> x))
                .toList();
        for (int k = 0; k < n; k++) {
            Sesion ubicada = null;
            Set<Integer> usados = tent.stream().map(Sesion::getDia).collect(Collectors.toCollection(HashSet::new));
            for (int dia : diasOrden) {
                if (usados.contains(dia)) continue;
                for (int h : dia == 5 ? new int[]{7, 9, 11} : horasLV) {
                    if (!disponible(d, dia, h, p.duracionSesion()) || oc.docenteOcupado(d.getId(), dia, h)) continue;
                    if (evitarCruces && oc.efectoNivel(nivel, g.getJornada(), g.getCursoId(), dia, h).cruza()) continue;
                    List<Sesion> base = new ArrayList<>(ya);
                    base.addAll(tent);
                    for (Aula aula : aulas) {
                        if (validador.validar(new Asignacion(null, g.getId(), dia, h, d.getId(), aula.getId()), base, p, e.catalogo()).isEmpty()) {
                            ubicada = new Sesion(Ids.nuevo("ses"), null, g.getId(), dia, h, d.getId(), aula.getId());
                            break;
                        }
                    }
                    if (ubicada != null) break;
                }
                if (ubicada != null) break;
            }
            if (ubicada == null) return null;
            tent.add(ubicada);
        }
        return tent;
    }

    // ------------------------------------------------------------------ apoyo

    static int sesionesDe(Grupo g, Catalogo cat, Parametros p) {
        return Math.max(p.sesionesMinimas(), cat.curso(g.getCursoId()).getSesionesSemanales());
    }

    private static boolean disponible(Docente d, int dia, int h, int dur) {
        for (BloqueDisponible b : d.getDisponibilidad()) {
            if (b.getDia() == dia && b.getInicio() <= h && b.getFin() >= h + dur) return true;
        }
        return false;
    }

    /** Un docente sirve para la jornada si su disponibilidad cubre al menos una franja de ella. */
    private static boolean sirveJornada(Docente d, Grupo g, int dur) {
        for (BloqueDisponible b : d.getDisponibilidad()) {
            int util = b.getDia() == 5 ? b.getFin() - b.getInicio()
                    : g.getJornada() == Jornada.DIURNA ? Math.min(b.getFin(), 18) - Math.max(b.getInicio(), 7)
                    : Math.min(b.getFin(), 22) - Math.max(b.getInicio(), 18);
            if (util >= dur) return true;
        }
        return false;
    }

    private static List<Aula> aulasAptas(Grupo g, Curso c, java.util.Collection<Aula> aulas) {
        Set<Caracteristica> req = c.getCaracteristicasRequeridas();
        return aulas.stream()
                .filter(a -> a.getCapacidad() >= g.getCupoPlaneado() && a.getCaracteristicas().containsAll(req))
                // aulas sin características que el curso no necesita primero (B5); luego la de mejor ajuste
                .sorted(Comparator.comparingLong((Aula a) -> a.getCaracteristicas().stream().filter(x -> !req.contains(x)).count())
                        .thenComparingInt(Aula::getCapacidad)
                        .thenComparing(Aula::getCodigo))
                .toList();
    }

    private static Set<String> docentesDiurnosDelCurso(Grupo g, List<Sesion> sesiones, Catalogo cat) {
        Set<String> ids = new HashSet<>();
        for (Sesion s : sesiones) {
            Grupo og = cat.grupo(s.getGrupoId());
            if (og != null && !og.getId().equals(g.getId()) && og.getCursoId().equals(g.getCursoId()) && og.getJornada() == Jornada.DIURNA
                    && s.getDocenteId() != null) {
                ids.add(s.getDocenteId());
            }
        }
        return ids;
    }

    /** Combinaciones de n días entre lunes (0) y sábado (5), en orden ascendente. */
    private static List<int[]> combinaciones(int n) {
        List<int[]> out = new ArrayList<>();
        combinar(0, n, new int[n], 0, out);
        return out;
    }

    private static void combinar(int desde, int n, int[] acc, int k, List<int[]> out) {
        if (k == n) {
            out.add(acc.clone());
            return;
        }
        for (int i = desde; i <= 5; i++) {
            acc[k] = i;
            combinar(i + 1, n, acc, k + 1, out);
        }
    }
}
