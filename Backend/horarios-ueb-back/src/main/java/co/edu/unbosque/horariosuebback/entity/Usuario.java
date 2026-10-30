package co.edu.unbosque.horariosuebback.entity;

import co.edu.unbosque.horariosuebback.entity.enums.Rol;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario del sistema (RF-22). La contraseña se guarda con BCrypt y nunca sale del backend. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    private String id;
    private String nombre;
    private String correo;
    private String hashClave;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private boolean activo;
    private String docenteId;
}
