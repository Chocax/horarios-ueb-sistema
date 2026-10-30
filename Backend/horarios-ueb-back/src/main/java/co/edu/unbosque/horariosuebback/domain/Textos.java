package co.edu.unbosque.horariosuebback.domain;

import co.edu.unbosque.horariosuebback.entity.enums.Caracteristica;

/** Textos en español para los mensajes de las reglas (RNF-02: rechazos en lenguaje del dominio). */
public final class Textos {

    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado"};

    private Textos() {
    }

    public static String dia(int d) {
        return d >= 0 && d < DIAS.length ? DIAS[d] : "día " + d;
    }

    public static String hh(int h) {
        return String.format("%02d:00", h);
    }

    public static String rango(int inicio, int fin) {
        return hh(inicio) + "–" + hh(fin);
    }

    public static String caracteristica(Caracteristica c) {
        return switch (c) {
            case EQUIPOS_COMPUTO -> "equipos de cómputo";
            case MOBILIARIO_MOVIL -> "mobiliario móvil";
            case LAB_REDES -> "laboratorio de redes";
            case LAB_FISICA -> "laboratorio de física";
        };
    }
}
