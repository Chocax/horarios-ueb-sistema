package co.edu.unbosque.horariosuebback.domain.reglas;

import co.edu.unbosque.horariosuebback.domain.Parametros;
import co.edu.unbosque.horariosuebback.domain.Textos;
import co.edu.unbosque.horariosuebback.entity.BloqueDisponible;
import co.edu.unbosque.horariosuebback.entity.Sesion;
import co.edu.unbosque.horariosuebback.entity.enums.Caracteristica;

import java.util.List;
import java.util.stream.Collectors;

/** Las nueve restricciones duras del Programa (RF-09). Se evalúan al generar, al mover, al editar y al publicar. */
public final class ReglasDuras {

    private ReglasDuras() {
    }

    public static final List<ReglaDura> TODAS = List.of(
            new R1DocenteSinCruces(),
            new R2AulaSinCruces(),
            new R3FranjaHabil(),
            new R4AulaAdecuada(),
            new R5DocenteHabilitado(),
            new R6DisponibilidadDocente(),
            new R7CargaMaxima(),
            new R8SesionesMaximas(),
            new R9CupoMaximo());

    public static class R1DocenteSinCruces implements ReglaDura {
        public String codigo() { return "R1"; }
        public String nombre() { return "Un docente no puede tener dos sesiones a la vez"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            if (ctx.docente() == null) return null;
            String id = ctx.docente().getId();
            return ctx.otras().stream()
                    .filter(s -> id.equals(s.getDocenteId()) && ctx.solapa(s))
                    .findFirst()
                    .map(s -> new Violacion(codigo(), ctx.docente().getNombre() + " ya tiene clase: " + ctx.etiqueta(s) + "."))
                    .orElse(null);
        }
    }

    public static class R2AulaSinCruces implements ReglaDura {
        public String codigo() { return "R2"; }
        public String nombre() { return "Un aula no puede tener dos sesiones a la vez"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            if (ctx.aula() == null) return null;
            String id = ctx.aula().getId();
            return ctx.otras().stream()
                    .filter(s -> id.equals(s.getAulaId()) && ctx.solapa(s))
                    .findFirst()
                    .map(s -> new Violacion(codigo(), "El aula " + ctx.aula().getCodigo() + " ya está ocupada: " + ctx.etiqueta(s) + "."))
                    .orElse(null);
        }
    }

    public static class R3FranjaHabil implements ReglaDura {
        public String codigo() { return "R3"; }
        public String nombre() { return "Dentro de la franja hábil y fuera de los bloques no asignables"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            Parametros.Rango franja = ctx.parametros().franja(ctx.dia());
            if (ctx.dia() < 0 || ctx.dia() > 5) {
                return new Violacion(codigo(), "El día de la sesión no es válido.");
            }
            if (ctx.inicio() < franja.inicio() || ctx.fin() > franja.fin()) {
                return new Violacion(codigo(), "El " + Textos.dia(ctx.dia()) + " solo se programa entre " + Textos.rango(franja.inicio(), franja.fin())
                        + "; la sesión quedaría " + Textos.rango(ctx.inicio(), ctx.fin()) + ".");
            }
            return ctx.parametros().bloquesNoAsignables().stream()
                    .filter(b -> b.seSolapaCon(ctx.inicio(), ctx.fin()))
                    .findFirst()
                    .map(b -> new Violacion(codigo(), "La sesión " + Textos.rango(ctx.inicio(), ctx.fin()) + " cruza el bloque no asignable "
                            + Textos.rango(b.inicio(), b.fin()) + "."))
                    .orElse(null);
        }
    }

    public static class R4AulaAdecuada implements ReglaDura {
        public String codigo() { return "R4"; }
        public String nombre() { return "Aula con capacidad y características suficientes"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            if (ctx.aula() == null) return new Violacion(codigo(), "La sesión no tiene aula asignada.");
            if (ctx.aula().getCapacidad() < ctx.grupo().getCupoPlaneado()) {
                return new Violacion(codigo(), "El aula " + ctx.aula().getCodigo() + " tiene capacidad " + ctx.aula().getCapacidad()
                        + " y el grupo " + ctx.grupo().getCodigo() + " tiene cupo " + ctx.grupo().getCupoPlaneado() + ".");
            }
            List<Caracteristica> faltan = ctx.curso().getCaracteristicasRequeridas().stream()
                    .filter(c -> !ctx.aula().getCaracteristicas().contains(c))
                    .sorted()
                    .toList();
            if (!faltan.isEmpty()) {
                String txt = faltan.stream().map(Textos::caracteristica).collect(Collectors.joining(" y "));
                return new Violacion(codigo(), ctx.curso().getNombre() + " requiere " + txt + " y el aula " + ctx.aula().getCodigo() + " no los tiene.");
            }
            return null;
        }
    }

    public static class R5DocenteHabilitado implements ReglaDura {
        public String codigo() { return "R5"; }
        public String nombre() { return "Docente habilitado para el curso"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            if (ctx.docente() == null) return new Violacion(codigo(), "La sesión no tiene docente asignado.");
            return ctx.docente().getHabilitaciones().contains(ctx.curso().getId())
                    ? null
                    : new Violacion(codigo(), ctx.docente().getNombre() + " no está habilitado para " + ctx.curso().getNombre() + ".");
        }
    }

    public static class R6DisponibilidadDocente implements ReglaDura {
        public String codigo() { return "R6"; }
        public String nombre() { return "Respeta la disponibilidad del docente"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            if (ctx.docente() == null) return null;
            boolean ok = false;
            for (BloqueDisponible b : ctx.docente().getDisponibilidad()) {
                if (b.getDia() == ctx.dia() && b.getInicio() <= ctx.inicio() && b.getFin() >= ctx.fin()) {
                    ok = true;
                    break;
                }
            }
            return ok ? null : new Violacion(codigo(), ctx.docente().getNombre() + " no está disponible el " + Textos.dia(ctx.dia()) + " "
                    + Textos.rango(ctx.inicio(), ctx.fin()) + ".");
        }
    }

    public static class R7CargaMaxima implements ReglaDura {
        public String codigo() { return "R7"; }
        public String nombre() { return "No supera las horas máximas del docente"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            if (ctx.docente() == null) return null;
            int dur = ctx.parametros().duracionSesion();
            String id = ctx.docente().getId();
            long carga = ctx.otras().stream().filter(s -> id.equals(s.getDocenteId())).count() * dur;
            return carga + dur > ctx.docente().getHorasMaxDocencia()
                    ? new Violacion(codigo(), ctx.docente().getNombre() + " quedaría con " + (carga + dur) + " h y su máximo de docencia es "
                    + ctx.docente().getHorasMaxDocencia() + " h.")
                    : null;
        }
    }

    public static class R8SesionesMaximas implements ReglaDura {
        public String codigo() { return "R8"; }
        public String nombre() { return "Frecuencia del grupo: sesiones semanales máximas y una sola sesión por día"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            List<Sesion> delGrupo = ctx.otras().stream().filter(s -> s.getGrupoId().equals(ctx.grupo().getId())).toList();
            int n = delGrupo.size() + 1;
            // mover una sesión no cambia el número de sesiones: el tope solo se revisa al agregar una
            if (ctx.esNueva() && n > ctx.parametros().sesionesMaximas()) {
                return new Violacion(codigo(), "El grupo " + ctx.grupo().getCodigo() + " quedaría con " + n + " sesiones y el máximo es "
                        + ctx.parametros().sesionesMaximas() + ".");
            }
            // un grupo no ve dos sesiones de la misma materia el mismo día
            return delGrupo.stream()
                    .filter(s -> s.getDia() == ctx.dia())
                    .findFirst()
                    .map(s -> new Violacion(codigo(), ctx.curso().getNombre() + " " + ctx.grupo().getCodigo() + " ya tiene clase el "
                            + Textos.dia(ctx.dia()) + " (" + Textos.rango(s.getInicio(), s.getInicio() + ctx.parametros().duracionSesion())
                            + "); un grupo solo puede tener una sesión por día."))
                    .orElse(null);
        }
    }

    public static class R9CupoMaximo implements ReglaDura {
        public String codigo() { return "R9"; }
        public String nombre() { return "Cupo del grupo dentro del máximo más la tolerancia"; }

        public Violacion evaluar(ContextoValidacion ctx) {
            int tope = ctx.parametros().cupoEfectivo();
            return ctx.grupo().getCupoPlaneado() > tope
                    ? new Violacion(codigo(), "El grupo " + ctx.grupo().getCodigo() + " tiene cupo " + ctx.grupo().getCupoPlaneado()
                    + " y el máximo efectivo es " + tope + ".")
                    : null;
        }
    }
}
