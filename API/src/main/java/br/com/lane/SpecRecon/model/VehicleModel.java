package br.com.lane.SpecRecon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidade de domínio para veículos.
 * <p>
 * Representa os dados persistidos de um veículo no sistema.
 * </p>
 */
@Entity
@Table(name = "Vehicle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModel {
    /** Identificador do veículo. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Marca do veículo. */
    @Column(nullable = false)
    private String brand;

    /** Modelo do veículo. */
    @Column(nullable = false)
    private String model;

    /** Versão do veículo. */
    @Column(nullable = false)
    private String version;

    /** Data/hora de criação do registro. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime created_at;

    /**
     * Define a data de criação antes de persistir o registro.
     */
    @PrePersist
    public void prePersist(){
        this.created_at = LocalDateTime.now();
    }

}
