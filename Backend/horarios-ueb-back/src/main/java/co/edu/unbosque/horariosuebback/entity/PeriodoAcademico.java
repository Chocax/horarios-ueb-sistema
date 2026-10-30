package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.EstadoPeriodo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Periodo académico con sus propios parámetros de programación (RF-01, RF-02, RF-16). */
@Entity
@Table(name = "periodo_academico")
@Getter
@Setter
@NoArgsConstructor
public class PeriodoAcademico {

    @Id
    private String id;
    private String codigo;
    private int anio;
    private int semestre;

    @Enumerated(EnumType.STRING)
    private EstadoPeriodo estado;

    private int franjaLvInicio;
    private int franjaLvFin;
    private int franjaSabInicio;
    private int franjaSabFin;

    @ElementCollection
    @CollectionTable(name = "bloque_no_asignable", joinColumns = @JoinColumn(name = "periodo_id"))
    @OrderColumn(name = "orden")
    private List<RangoHorario> bloquesNoAsignables = new ArrayList<>();

    private int duracionSesion;
    private int cupoMaximo;
    private int toleranciaCupo;
    private int cupoMinimo;
    private int sesionesMinimas;
    private int sesionesMaximas;
    private int horasTiempoCompleto;
    private int horasTresCuartos;
    private int horasMedioTiempo;
    private int horasCuartoTiempo;
}
