package co.edu.unbosque.horariosuebback.domain;

import co.edu.unbosque.horariosuebback.entity.PeriodoAcademico;
import co.edu.unbosque.horariosuebback.entity.enums.Vinculacion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Parámetros de programación de un periodo (RF-01, RF-02), en la forma que usan las reglas y el generador. */
public record Parametros(
        Rango franjaLunesViernes,
        Rango franjaSabado,
        List<Rango> bloquesNoAsignables,
        int duracionSesion,
        int cupoMaximo,
        int toleranciaCupo,
        int cupoMinimo,
        int sesionesMinimas,
        int sesionesMaximas,
        Map<Vinculacion, Integer> horasMaxPorVinculacion) {

    public record Rango(int inicio, int fin) {
        public boolean seSolapaCon(int a, int b) {
            return inicio < b && a < fin;
        }
    }

    /** Cupo máximo más la tolerancia (R9): 40 + 10 % = 44. */
    public int cupoEfectivo() {
        return (int) Math.floor(cupoMaximo * (1 + toleranciaCupo / 100.0));
    }

    public Rango franja(int dia) {
        return dia == 5 ? franjaSabado : franjaLunesViernes;
    }

    public static Parametros de(PeriodoAcademico p) {
        Map<Vinculacion, Integer> horas = new EnumMap<>(Vinculacion.class);
        horas.put(Vinculacion.TIEMPO_COMPLETO, p.getHorasTiempoCompleto());
        horas.put(Vinculacion.TRES_CUARTOS, p.getHorasTresCuartos());
        horas.put(Vinculacion.MEDIO_TIEMPO, p.getHorasMedioTiempo());
        horas.put(Vinculacion.CUARTO_TIEMPO, p.getHorasCuartoTiempo());
        return new Parametros(
                new Rango(p.getFranjaLvInicio(), p.getFranjaLvFin()),
                new Rango(p.getFranjaSabInicio(), p.getFranjaSabFin()),
                p.getBloquesNoAsignables().stream().map(b -> new Rango(b.getInicio(), b.getFin())).toList(),
                p.getDuracionSesion(),
                p.getCupoMaximo(),
                p.getToleranciaCupo(),
                p.getCupoMinimo(),
                p.getSesionesMinimas(),
                p.getSesionesMaximas(),
                horas);
    }
}
