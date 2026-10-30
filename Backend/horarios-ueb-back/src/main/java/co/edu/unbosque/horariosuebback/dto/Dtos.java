package co.edu.unbosque.horariosuebback.dto;

import co.edu.unbosque.horariosuebback.domain.reglas.Violacion;
import co.edu.unbosque.horariosuebback.entity.enums.Caracteristica;
import co.edu.unbosque.horariosuebback.entity.enums.Concepto;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoGrupo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoPeriodo;
import co.edu.unbosque.horariosuebback.entity.enums.EstadoVersion;
import co.edu.unbosque.horariosuebback.entity.enums.Jornada;
import co.edu.unbosque.horariosuebback.entity.enums.ModoAtencion;
import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import co.edu.unbosque.horariosuebback.entity.enums.TipoCurso;
import co.edu.unbosque.horariosuebback.entity.enums.Vinculacion;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Objetos que viajan entre el front (Angular) y el backend. Tienen la misma forma que los modelos del front
 * (src/app/core/models.ts). Nunca incluyen el hash de la contraseña.
 */
public final class Dtos {

    private Dtos() {
    }

    // ------------------------------------------------------------------ respuesta común

    /** Resultado de una operación: ok, mensaje y, si falló, errores o violaciones de las reglas duras. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Respuesta<T>(boolean ok, String mensaje, List<String> errores, List<Violacion> violaciones, T valor) {
        public static <T> Respuesta<T> ok(T valor) {
            return new Respuesta<>(true, null, null, null, valor);
        }

        public static <T> Respuesta<T> ok(String mensaje, T valor) {
            return new Respuesta<>(true, mensaje, null, null, valor);
        }
    }

    // ------------------------------------------------------------------ catálogo

    public record RangoDto(@Min(0) @Max(24) int inicio, @Min(0) @Max(24) int fin) {
    }

    public record ParametrosDto(
            @NotNull @Valid RangoDto franjaLunesViernes,
            @NotNull @Valid RangoDto franjaSabado,
            @NotNull @Size(max = 6) List<@Valid RangoDto> bloquesNoAsignables,
            int duracionSesion,
            int cupoMaximo,
            int toleranciaCupo,
            int cupoMinimo,
            int sesionesMinimas,
            int sesionesMaximas,
            @NotNull Map<Vinculacion, Integer> horasMaxPorVinculacion) {
    }

    public record PeriodoDto(String id, String codigo, int anio, int semestre, EstadoPeriodo estado, ParametrosDto parametros) {
    }

    public record CursoDto(
            @NotBlank @Size(max = 40) String id,
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 120) String nombre,
            int nivel,
            int creditos,
            int sesionesSemanales,
            TipoCurso tipo,
            @NotNull Set<Caracteristica> caracteristicasRequeridas) {
    }

    public record OfertaDto(@NotBlank String periodoId, @NotBlank String cursoId, @Min(0) @Max(100000) int demandaEstimada) {
    }

    public record BloqueDto(@Min(0) @Max(5) int dia, @Min(0) @Max(24) int inicio, @Min(0) @Max(24) int fin) {
    }

    public record DocenteDto(
            @NotBlank @Size(max = 40) String id,
            @NotBlank @Size(max = 20) String identificador,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 120) String correoInstitucional,
            Vinculacion tipoVinculacion,
            @Size(max = 40) String categoriaEscalafon,
            int horasMaxDocencia,
            @NotNull @Size(max = 200) Set<String> habilitaciones,
            @NotNull @Size(max = 100) List<@Valid BloqueDto> disponibilidad) {
    }

    public record AulaDto(
            @NotBlank @Size(max = 40) String id,
            @NotBlank @Size(max = 20) String codigo,
            @Size(max = 120) String nombre,
            int capacidad,
            @NotNull Set<Caracteristica> caracteristicas,
            @Size(max = 5) String bloque,
            Integer piso) {
    }

    public record GrupoDto(
            @NotBlank @Size(max = 40) String id,
            @NotBlank String periodoId,
            @NotBlank String cursoId,
            @NotBlank @Size(max = 10) String codigo,
            @NotNull Jornada jornada,
            int cupoPlaneado,
            Integer inscritos,
            EstadoGrupo estado,
            String docentePreferido) {
    }

    // ------------------------------------------------------------------ horario

    public record SesionDto(String id, String grupoId, int dia, int inicio, String docenteId, String aulaId) {
    }

    public record PendienteDto(String grupoId, String causa, String regla) {
    }

    public record VersionDto(String id, String periodoId, int numero, EstadoVersion estado, String origen, Instant fechaCreacion,
                             Instant fechaPublicacion, String autor, List<SesionDto> sesiones, List<PendienteDto> pendientes) {
    }

    public record ObservacionDto(String id, String versionId, String usuarioId, String autor, Concepto concepto, String texto, Instant fecha) {
    }

    public record AtencionDto(
            @NotBlank @Size(max = 40) String id,
            @NotBlank String periodoId,
            @NotBlank String docenteId,
            @NotBlank String cursoId,
            @NotNull ModoAtencion modo,
            Integer dia,
            Integer inicio,
            Integer fin,
            @Size(max = 200) String lugar) {
    }

    // ------------------------------------------------------------------ usuarios y auditoría

    public record UsuarioDto(String id, String nombre, String correo, Rol rol, boolean activo, String docenteId) {
    }

    public record BitacoraDto(Long id, Instant fecha, String usuario, String rol, String accion, String detalle) {
    }

    /** Estado que el front necesita para trabajar en un periodo; se filtra según el rol de quien lo pide. */
    public record EstadoDto(
            List<PeriodoDto> periodos,
            List<CursoDto> cursos,
            List<OfertaDto> ofertas,
            List<DocenteDto> docentes,
            List<AulaDto> aulas,
            List<GrupoDto> grupos,
            List<VersionDto> versiones,
            List<ObservacionDto> observaciones,
            List<UsuarioDto> usuarios,
            List<AtencionDto> horariosAtencion,
            String periodoActivoId) {
    }

    // ------------------------------------------------------------------ peticiones

    public record LoginRequest(@NotBlank @Size(max = 120) String correo, @NotBlank @Size(max = 128) String clave) {
    }

    public record LoginResponse(String token, long expiraEn, UsuarioDto usuario) {
    }

    public record CambioClaveRequest(@NotBlank @Size(max = 128) String claveActual, @NotBlank @Size(max = 128) String claveNueva) {
    }

    public record CrearPeriodoRequest(@Min(2000) @Max(2100) int anio, @Min(1) @Max(2) int semestre, boolean copiarOferta, String basePeriodoId) {
    }

    public record CursoRequest(@NotNull @Valid CursoDto curso, String periodoId, @Min(0) @Max(100000) Integer demanda) {
    }

    public record DemandaRequest(@Min(0) @Max(100000) int demanda) {
    }

    public record NumeroGruposRequest(@Min(1) @Max(30) int numero) {
    }

    public record InscritosRequest(@Min(0) @Max(1000) Integer inscritos) {
    }

    public record MoverSesionRequest(int dia, int inicio, String docenteId, String aulaId) {
    }

    public record UbicarRequest(int dia, int inicio) {
    }

    public record ObservacionRequest(@NotNull Concepto concepto, @NotBlank @Size(max = 2000) String texto) {
    }

    public record DisponibilidadRequest(@NotNull @Size(max = 100) List<@Valid BloqueDto> bloques) {
    }

    public record UsuarioRequest(
            @Size(max = 40) String id,
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 120) String correo,
            @NotNull Rol rol,
            boolean activo,
            String docenteId,
            @Size(max = 128) String clave) {
    }

    public record ImportacionRequest(
            @Size(max = 2000) List<@Valid DocenteDto> docentes,
            @Size(max = 2000) List<@Valid CursoDto> cursos,
            @Size(max = 2000) List<@Valid AulaDto> aulas,
            @Size(max = 2000) List<@Valid OfertaDto> ofertas,
            @Size(max = 300) String resumen) {
    }
}
